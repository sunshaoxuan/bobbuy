package com.bobbuy.service;

import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.api.LedgerConfirmationRequest;
import com.bobbuy.api.ProcurementHudResponse;
import com.bobbuy.api.response.ApiException;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.ProductPatch;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = BobbuyApplication.class)
class ProcurementHudServiceTest {
  @Autowired
  private BobbuyStore store;

  @Autowired
  private ProcurementHudService procurementHudService;

  @BeforeEach
  void setUp() {
    store.seed();
  }

  @Test
  void reconcileInventoryFillsDraftOrdersByCreationOrderWithinSameTrip() {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader first = new OrderHeader("RECON-1", 1001L, trip.getId());
    first.addLine(new OrderLine("prd-1000", "Matcha", null, 3, 10.0));
    OrderHeader createdFirst = store.upsertOrder(first);

    OrderHeader second = new OrderHeader("RECON-2", 1001L, trip.getId());
    OrderLine secondLine = new OrderLine("prd-1000", "Matcha", null, 2, 10.0);
    secondLine.setPurchasedQuantity(1);
    second.addLine(secondLine);
    OrderHeader createdSecond = store.upsertOrder(second);

    int reconciled = procurementHudService.reconcileInventory("prd-1000", 3);

    assertThat(reconciled).isEqualTo(3);
    assertThat(store.getOrder(createdFirst.getId()).orElseThrow().getLines().get(0).getPurchasedQuantity()).isEqualTo(3);
    assertThat(store.getOrder(createdSecond.getId()).orElseThrow().getLines().get(0).getPurchasedQuantity()).isEqualTo(1);
  }

  @Test
  void reconcileInventoryStopsAtTargetTripWhenSameProductExistsInOtherTrips() {
    Trip trip1 = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    Trip trip2 = store.createTrip(new Trip(null, 1000L, "LA", "SF", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));

    OrderHeader firstTripOrder = new OrderHeader("RECON-T1", 1001L, trip1.getId());
    firstTripOrder.addLine(new OrderLine("prd-1000", "Matcha", null, 1, 10.0));
    OrderHeader createdTrip1 = store.upsertOrder(firstTripOrder);

    OrderHeader secondTripOrder = new OrderHeader("RECON-T2", 1001L, trip2.getId());
    secondTripOrder.addLine(new OrderLine("prd-1000", "Matcha", null, 2, 10.0));
    OrderHeader createdTrip2 = store.upsertOrder(secondTripOrder);

    int reconciled = procurementHudService.reconcileInventory("prd-1000", 2);

    assertThat(reconciled).isEqualTo(1);
    assertThat(store.getOrder(createdTrip1.getId()).orElseThrow().getLines().get(0).getPurchasedQuantity()).isEqualTo(1);
    assertThat(store.getOrder(createdTrip2.getId()).orElseThrow().getLines().get(0).getPurchasedQuantity()).isEqualTo(0);
  }

  @Test
  void getHudStatsUsesProductPhysicalMetricsInsteadOfStaticUnitWeight() {
    ProductPatch patch = new ProductPatch();
    patch.setWeight(2.5);
    patch.setVolume(1.2);
    store.patchProduct("prd-1000", patch);

    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader order = new OrderHeader("HUD-PHYSICAL", 1001L, trip.getId());
    OrderLine line = new OrderLine("prd-1000", "Matcha", null, 2, 10.0);
    line.setPurchasedQuantity(2);
    order.addLine(line);
    store.upsertOrder(order);

    ProcurementHudResponse updatedHud = procurementHudService.getHudStats(trip.getId());

    assertThat(updatedHud.getCurrentWeight()).isEqualTo(5.0);
    assertThat(updatedHud.getCurrentVolume()).isEqualTo(2.4);
  }

  @Test
  void reconcileInventoryWithDetailsReturnsAllocatedBusinessIds() {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader order = new OrderHeader("RECON-DETAIL", 1001L, trip.getId());
    order.addLine(new OrderLine("prd-1000", "Matcha", null, 2, 10.0));
    store.upsertOrder(order);

    ProcurementHudService.ReconcileInventoryResult result = procurementHudService.reconcileInventoryWithDetails("prd-1000", 1);

    assertThat(result.reconciledQuantity()).isEqualTo(1);
    assertThat(result.tripId()).isEqualTo(trip.getId());
    assertThat(result.allocatedBusinessIds()).containsExactly("RECON-DETAIL");
  }

  @Test
  void getHudStatsSubtractsTripExpensesFromProfit() {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader order = new OrderHeader("HUD-EXPENSE", 1001L, trip.getId());
    OrderLine line = new OrderLine("prd-1000", "Matcha", null, 2, 10.0);
    line.setPurchasedQuantity(2);
    order.addLine(line);
    store.upsertOrder(order);
    procurementHudService.createTripExpense(trip.getId(), buildExpense("停车费", 3.0));

    ProcurementHudResponse hud = procurementHudService.getHudStats(trip.getId());

    assertThat(hud.getTotalTripExpenses()).isEqualTo(3.0);
    assertThat(hud.getTotalEstimatedProfit()).isEqualTo(-3.0);
  }

  @Test
  void manualReconcileTransfersPurchasedQuantityBetweenOrders() {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader fromOrder = new OrderHeader("MANUAL-FROM", 1001L, trip.getId());
    OrderLine fromLine = new OrderLine("prd-1000", "Matcha", null, 3, 10.0);
    fromLine.setPurchasedQuantity(2);
    fromOrder.addLine(fromLine);
    store.upsertOrder(fromOrder);

    OrderHeader toOrder = new OrderHeader("MANUAL-TO", 1001L, trip.getId());
    toOrder.addLine(new OrderLine("prd-1000", "Matcha", null, 3, 10.0));
    store.upsertOrder(toOrder);

    int moved = procurementHudService.manualReconcile(trip.getId(), "prd-1000", "MANUAL-FROM", "MANUAL-TO", 1);

    assertThat(moved).isEqualTo(1);
    assertThat(store.getOrderByBusinessId("MANUAL-FROM").orElseThrow().getLines().get(0).getPurchasedQuantity()).isEqualTo(1);
    assertThat(store.getOrderByBusinessId("MANUAL-TO").orElseThrow().getLines().get(0).getPurchasedQuantity()).isEqualTo(1);
  }

  @Test
  void manualReconcileIsBlockedWhenTripSettlementFrozen() {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader fromOrder = new OrderHeader("MANUAL-FROZEN-FROM", 1001L, trip.getId());
    OrderLine fromLine = new OrderLine("prd-1000", "Matcha", null, 2, 10.0);
    fromLine.setPurchasedQuantity(2);
    fromOrder.addLine(fromLine);
    store.upsertOrder(fromOrder);

    OrderHeader toOrder = new OrderHeader("MANUAL-FROZEN-TO", 1001L, trip.getId());
    toOrder.addLine(new OrderLine("prd-1000", "Matcha", null, 2, 10.0));
    store.upsertOrder(toOrder);
    store.updateTripStatus(trip.getId(), TripStatus.COMPLETED);

    assertThatThrownBy(() -> procurementHudService.manualReconcile(trip.getId(), "prd-1000", "MANUAL-FROZEN-FROM", "MANUAL-FROZEN-TO", 1))
        .isInstanceOf(ApiException.class)
        .satisfies(error -> assertThat(((ApiException) error).getMessageKey()).isEqualTo("error.trip.settlement_frozen"));
  }

  @Test
  void confirmCustomerLedgerRequiresReceiptBeforeBilling() {
    LedgerConfirmationRequest request = new LedgerConfirmationRequest();
    request.setAction("BILLING");

    assertThatThrownBy(() -> procurementHudService.confirmCustomerLedger(
        2000L,
        "20260117001",
        request,
        new UsernamePasswordAuthenticationToken("1001", "N/A")))
        .isInstanceOf(ApiException.class)
        .satisfies(error -> assertThat(((ApiException) error).getMessageKey()).isEqualTo("error.procurement.billing.receipt_required"));
  }

  private com.bobbuy.api.TripExpenseRequest buildExpense(String category, double cost) {
    com.bobbuy.api.TripExpenseRequest request = new com.bobbuy.api.TripExpenseRequest();
    request.setCategory(category);
    request.setCost(cost);
    return request;
  }
}

package com.bobbuy.service;

import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.api.ProcurementHudResponse;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.ProductPatch;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

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

    ProcurementHudResponse hud = procurementHudService.getHudStats(2000L);

    assertThat(hud.getCurrentWeight()).isEqualTo(0);
    assertThat(hud.getCurrentVolume()).isEqualTo(0);

    procurementHudService.reconcileInventory("prd-1000", 2);
    ProcurementHudResponse updatedHud = procurementHudService.getHudStats(2000L);

    assertThat(updatedHud.getCurrentWeight()).isEqualTo(5.0);
    assertThat(updatedHud.getCurrentVolume()).isEqualTo(2.4);
  }

  @Test
  void reconcileInventoryWithDetailsReturnsAllocatedBusinessIds() {
    ProcurementHudService.ReconcileInventoryResult result = procurementHudService.reconcileInventoryWithDetails("prd-1000", 1);

    assertThat(result.reconciledQuantity()).isEqualTo(1);
    assertThat(result.tripId()).isEqualTo(2000L);
    assertThat(result.allocatedBusinessIds()).containsExactly("20260117001");
  }
}

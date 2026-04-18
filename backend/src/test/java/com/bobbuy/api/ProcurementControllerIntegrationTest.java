package com.bobbuy.api;

import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.service.BobbuyStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class ProcurementControllerIntegrationTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private BobbuyStore store;
  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    store.seed();
  }

  @Test
  void hudReturnsRealtimeProfitAndCategoryCompletion() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader order = new OrderHeader("HUD-1", 1001L, trip.getId());
    OrderLine line = new OrderLine("prd-1000", "Matcha", null, 4, 32.5);
    line.setPurchasedQuantity(1);
    order.addLine(line);
    store.upsertOrder(order);

    mockMvc.perform(get("/api/procurement/{tripId}/hud", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalEstimatedProfit").value(97.5))
        .andExpect(jsonPath("$.data.currentPurchasedAmount").value(32.5))
        .andExpect(jsonPath("$.data.currentFxRate").value(1.0))
        .andExpect(jsonPath("$.data.referenceFxRate").value(1.0))
        .andExpect(jsonPath("$.data.totalTripExpenses").value(0.0))
        .andExpect(jsonPath("$.data.currentWeight").value(0.5))
        .andExpect(jsonPath("$.data.currentVolume").value(0.2))
        .andExpect(jsonPath("$.data.categoryCompletionPercent.cat-1000").value(25.0));
  }

  @Test
  void deficitReturnsMissingItemsWithPriority() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader order = new OrderHeader("DEFICIT-1", 1001L, trip.getId());
    OrderLine line = new OrderLine("prd-1000", "Matcha", null, 4, 32.5);
    line.setPurchasedQuantity(1);
    order.addLine(line);
    store.upsertOrder(order);

    mockMvc.perform(get("/api/procurement/{tripId}/deficit", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.data[0].skuId").value("prd-1000"))
        .andExpect(jsonPath("$.data[0].deficitQuantity").value(3))
        .andExpect(jsonPath("$.data[0].completionPercent").value(25.0))
        .andExpect(jsonPath("$.data[0].priority").value("HIGH"));
  }

  @Test
  void expensesEndpointCreatesAndListsTripExpense() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));

    MvcResult createResult = mockMvc.perform(post("/api/procurement/{tripId}/expenses", trip.getId())
            .contentType("application/json")
            .content("""
                {"category":"停车费","cost":12.5}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.category").value("停车费"))
        .andExpect(jsonPath("$.data.cost").value(12.5))
        .andReturn();
    JsonNode createdPayload = objectMapper.readTree(createResult.getResponse().getContentAsString());
    long expenseId = createdPayload.path("data").path("id").asLong();

    mockMvc.perform(get("/api/procurement/{tripId}/expenses", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.data[0].category").value("停车费"))
        .andExpect(jsonPath("$.data[0].cost").value(12.5))
        .andExpect(jsonPath("$.data[0].ocrStatus").value("NOT_UPLOADED"));

    mockMvc.perform(get("/api/procurement/{tripId}/expenses/{expenseId}/receipt-preview", trip.getId(), expenseId))
        .andExpect(status().isNotFound());
  }

  @Test
  void exportEndpointReturnsCsvAndPdf() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));

    mockMvc.perform(get("/api/procurement/{tripId}/export?format=csv", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("tripId,currentFxRate")));

    mockMvc.perform(get("/api/procurement/{tripId}/export?format=pdf", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
        .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0));
  }

  @Test
  void auditLogAndLedgerEndpointsReturnFinancialData() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader fromOrder = new OrderHeader("LEDGER-FROM", 1001L, trip.getId());
    OrderLine fromLine = new OrderLine("prd-1000", "抹茶セット", null, 3, 10.0);
    fromLine.setPurchasedQuantity(2);
    fromOrder.addLine(fromLine);
    store.upsertOrder(fromOrder);

    OrderHeader toOrder = new OrderHeader("LEDGER-TO", 1001L, trip.getId());
    toOrder.addLine(new OrderLine("prd-1000", "抹茶セット", null, 3, 10.0));
    store.upsertOrder(toOrder);

    mockMvc.perform(post("/api/procurement/{tripId}/expenses", trip.getId())
            .contentType("application/json")
            .content("""
                {"category":"停车费","cost":12.5}
                """))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/procurement/{tripId}/manual-reconcile", trip.getId())
            .contentType("application/json")
            .content("""
                {"skuId":"prd-1000","fromBusinessId":"LEDGER-FROM","toBusinessId":"LEDGER-TO","quantity":1}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.transferredQuantity").value(1));

    mockMvc.perform(get("/api/procurement/{tripId}/ledger", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(2))
        .andExpect(jsonPath("$.data[0].businessId").value("LEDGER-FROM"))
        .andExpect(jsonPath("$.data[0].totalReceivable").value(10.0));

    mockMvc.perform(get("/api/procurement/{tripId}/audit-logs", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(2))
        .andExpect(jsonPath("$.data[0].actionType").exists())
        .andExpect(jsonPath("$.data[0].currentHash").exists());
  }

  @Test
  void customerStatementPdfEndpointReturnsDocument() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader order = new OrderHeader("CUST-PDF-1", 1001L, trip.getId());
    order.addLine(new OrderLine("prd-1000", "抹茶セット", null, 2, 32.5));
    store.upsertOrder(order);

    mockMvc.perform(get("/api/procurement/{tripId}/customers/{businessId}/statement", trip.getId(), "CUST-PDF-1"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
        .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0));
  }

  @Test
  void profitSharingEndpointsSupportReadAndUpdateWithAuditLog() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    OrderHeader order = new OrderHeader("PS-1", 1001L, trip.getId());
    order.addLine(new OrderLine("prd-1000", "Matcha", null, 2, 10.0));
    store.upsertOrder(order);

    mockMvc.perform(get("/api/procurement/{tripId}/profit-sharing", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.purchaserRatioPercent").value(70.0))
        .andExpect(jsonPath("$.data.promoterRatioPercent").value(30.0))
        .andExpect(jsonPath("$.data.shares[0].partnerRole").value("PURCHASER"));

    mockMvc.perform(patch("/api/procurement/{tripId}/profit-sharing", trip.getId())
            .contentType("application/json")
            .content("""
                {"purchaserRatioPercent":60,"promoterRatioPercent":40}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.purchaserRatioPercent").value(60.0))
        .andExpect(jsonPath("$.data.promoterRatioPercent").value(40.0));

    mockMvc.perform(get("/api/procurement/{tripId}/audit-logs", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].actionType").value("PROFIT_SHARE_RATIO_UPDATE"));
  }

  @Test
  void logisticsEndpointsSupportTrackingAndSettlementReminderTrigger() throws Exception {
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));

    MvcResult createResult = mockMvc.perform(post("/api/procurement/{tripId}/logistics", trip.getId())
            .contentType("application/json")
            .content("""
                {"trackingNumber":"MOCK-DELIVERED","channel":"INTERNATIONAL","provider":"MOCK"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.trackingNumber").value("MOCK-DELIVERED"))
        .andReturn();
    JsonNode createdPayload = objectMapper.readTree(createResult.getResponse().getContentAsString());
    long trackingId = createdPayload.path("data").path("id").asLong();

    mockMvc.perform(post("/api/procurement/{tripId}/logistics/{trackingId}/refresh", trip.getId(), trackingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("DELIVERED"))
        .andExpect(jsonPath("$.data.settlementReminderTriggered").value(true));

    mockMvc.perform(get("/api/procurement/{tripId}/logistics", trip.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.data[0].settlementReminderTriggered").value(true));
  }
}

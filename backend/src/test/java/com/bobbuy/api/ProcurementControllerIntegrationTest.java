package com.bobbuy.api;

import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProcurementControllerIntegrationTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private BobbuyStore store;

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
        .andExpect(jsonPath("$.data.totalEstimatedProfit").value(4.88))
        .andExpect(jsonPath("$.data.currentPurchasedAmount").value(1.63))
        .andExpect(jsonPath("$.data.currentWeight").value(1.0))
        .andExpect(jsonPath("$.data.currentVolume").value(1.0))
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
}

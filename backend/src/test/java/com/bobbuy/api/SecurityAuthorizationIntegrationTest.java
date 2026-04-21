package com.bobbuy.api;

import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.security.RoleInjectionFilter;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private BobbuyStore store;

  private Long tripId;

  @BeforeEach
  void setUp() {
    store.seed();
    Trip trip = store.createTrip(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 20, 0, TripStatus.DRAFT, null));
    tripId = trip.getId();

    OrderHeader ownOrder = new OrderHeader("LEDGER-CUST-OWN", 9001L, tripId);
    ownOrder.addLine(new OrderLine("SKU-OWN", "Own Item", null, 2, 10.0));
    store.upsertOrder(ownOrder);

    OrderHeader otherOrder = new OrderHeader("LEDGER-CUST-OTHER", 9002L, tripId);
    otherOrder.addLine(new OrderLine("SKU-OTHER", "Other Item", null, 1, 20.0));
    store.upsertOrder(otherOrder);
  }

  @Test
  void customerCanReadScopedLedgerOnly() throws Exception {
    mockMvc.perform(asCustomer(get("/api/procurement/{tripId}/ledger", tripId), "9001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.total").value(1))
        .andExpect(jsonPath("$.data[0].customerId").value(9001))
        .andExpect(jsonPath("$.data[0].businessId").value("LEDGER-CUST-OWN"));
  }

  @Test
  void customerIsForbiddenFromBackofficeApisButCanReadOrders() throws Exception {
    mockMvc.perform(asCustomer(get("/api/procurement/{tripId}/hud", tripId), "9001"))
        .andExpect(status().isForbidden());

    mockMvc.perform(asCustomer(get("/api/financial/audit/{tripId}", tripId), "9001"))
        .andExpect(status().isForbidden());

    mockMvc.perform(asCustomer(get("/api/users"), "9001"))
        .andExpect(status().isForbidden());

    mockMvc.perform(asCustomer(get("/api/orders"), "9001"))
        .andExpect(status().isOk());

    mockMvc.perform(asCustomer(patch("/api/orders/{id}/status", 3000L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"CONFIRMED"}
                """), "9001"))
        .andExpect(status().isForbidden());
  }

  @Test
  void agentCanAccessKeyBackofficeApis() throws Exception {
    mockMvc.perform(asAgent(get("/api/procurement/{tripId}/hud", tripId)))
        .andExpect(status().isOk());

    mockMvc.perform(asAgent(get("/api/financial/audit/{tripId}/check-integrity", tripId)))
        .andExpect(status().isOk());

    mockMvc.perform(asAgent(get("/api/users")))
        .andExpect(status().isOk());

    mockMvc.perform(asAgent(get("/api/orders")))
        .andExpect(status().isOk());

    mockMvc.perform(asAgent(patch("/api/orders/{id}/status", 3000L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"CONFIRMED"}
                """)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
  }

  private MockHttpServletRequestBuilder asCustomer(MockHttpServletRequestBuilder request, String user) {
    return request
        .header(RoleInjectionFilter.ROLE_HEADER, "CUSTOMER")
        .header(RoleInjectionFilter.USER_HEADER, user);
  }

  private MockHttpServletRequestBuilder asAgent(MockHttpServletRequestBuilder request) {
    return request
        .header(RoleInjectionFilter.ROLE_HEADER, "AGENT")
        .header(RoleInjectionFilter.USER_HEADER, "1000");
  }
}

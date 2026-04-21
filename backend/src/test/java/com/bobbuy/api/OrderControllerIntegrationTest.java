package com.bobbuy.api;

import com.bobbuy.service.BobbuyStore;
import com.bobbuy.security.RoleInjectionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    private static final String CUSTOMER_ROLE = "CUSTOMER";
    private static final String CUSTOMER_USER = "1001";
    private static final String AGENT_ROLE = "AGENT";
    private static final String AGENT_USER = "1000";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BobbuyStore store;

    @BeforeEach
    void setUp() {
        store.seed();
    }

    @Test
    void testLoggingInterceptorAndTraceId() throws Exception {
        mockMvc.perform(get("/api/orders")
                .header(RoleInjectionFilter.ROLE_HEADER, CUSTOMER_ROLE)
                .header(RoleInjectionFilter.USER_HEADER, CUSTOMER_USER))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void updateStatusAcceptsCaseInsensitiveEnumValues() throws Exception {
        mockMvc.perform(patch("/api/orders/{id}/status", 3000L)
                .header(RoleInjectionFilter.ROLE_HEADER, AGENT_ROLE)
                .header(RoleInjectionFilter.USER_HEADER, AGENT_USER)
                .contentType("application/json")
                .content("""
                    {"status":"confirmed"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}

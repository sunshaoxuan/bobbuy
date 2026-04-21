package com.bobbuy.api;

import com.bobbuy.security.RoleInjectionFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
public class FinancialAuditIntegrationTest {
    private static final String AGENT_ROLE = "AGENT";
    private static final String AGENT_USER = "1000";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAuditIntegrityAndLogRetrieval() throws Exception {
        Long tripId = 2000L;
        
        // 1. Initial integrity check (should be true even if empty)
        mockMvc.perform(get("/api/financial/audit/" + tripId + "/check-integrity")
                .header(RoleInjectionFilter.ROLE_HEADER, AGENT_ROLE)
                .header(RoleInjectionFilter.USER_HEADER, AGENT_USER)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isValid").value(true));

        // 2. Fetch logs (should be empty but succeed)
        mockMvc.perform(get("/api/financial/audit/" + tripId)
                .header(RoleInjectionFilter.ROLE_HEADER, AGENT_ROLE)
                .header(RoleInjectionFilter.USER_HEADER, AGENT_USER)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}

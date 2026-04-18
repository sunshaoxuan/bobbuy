package com.bobbuy.api;

import com.bobbuy.service.BobbuyStore;
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
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void updateStatusAcceptsCaseInsensitiveEnumValues() throws Exception {
        mockMvc.perform(patch("/api/orders/{id}/status", 3000L)
                .contentType("application/json")
                .content("""
                    {"status":"confirmed"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}

package com.bobbuy.api;

import com.bobbuy.service.BobbuyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "bobbuy.security.refresh-token.ttl-seconds=1")
@AutoConfigureMockMvc
class AuthRefreshTokenExpiryIntegrationTest {

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
  void expiredRefreshTokenIsRejected() throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"agent","password":"agent-pass"}
                """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    String refreshToken = objectMapper.readTree(response).path("data").path("refreshToken").asText();

    Thread.sleep(1200L);

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(refreshToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
  }
}

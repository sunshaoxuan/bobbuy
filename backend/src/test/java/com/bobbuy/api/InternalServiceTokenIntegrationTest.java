package com.bobbuy.api;

import com.bobbuy.security.InternalServiceTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "bobbuy.security.header-auth.enabled=false",
    "bobbuy.security.service-token.secret=test-service-token",
    "bobbuy.ai.remote.enabled=false"
})
@AutoConfigureMockMvc
class InternalServiceTokenIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void internalEndpointRejectsMissingServiceToken() throws Exception {
    mockMvc.perform(post("/internal/ai/receipt-recognition")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"base64Image":"ZmFrZQ==","fileName":"receipt.png","orders":[]}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void internalEndpointRejectsWrongServiceToken() throws Exception {
    mockMvc.perform(post("/internal/ai/receipt-recognition")
            .header(InternalServiceTokenFilter.SERVICE_TOKEN_HEADER, "wrong-token")
            .header(InternalServiceTokenFilter.INTERNAL_SERVICE_HEADER, "gateway-service")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"base64Image":"ZmFrZQ==","fileName":"receipt.png","orders":[]}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void internalEndpointAcceptsValidServiceTokenWithoutUserJwt() throws Exception {
    mockMvc.perform(post("/internal/ai/receipt-recognition")
            .header(InternalServiceTokenFilter.SERVICE_TOKEN_HEADER, "test-service-token")
            .header(InternalServiceTokenFilter.INTERNAL_SERVICE_HEADER, "core-service")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"base64Image":"ZmFrZQ==","fileName":"receipt.png","orders":[]}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void validServiceTokenDoesNotImpersonateCurrentUser() throws Exception {
    mockMvc.perform(get("/api/auth/me")
            .header(InternalServiceTokenFilter.SERVICE_TOKEN_HEADER, "test-service-token")
            .header(InternalServiceTokenFilter.INTERNAL_SERVICE_HEADER, "gateway-service"))
        .andExpect(status().isUnauthorized());
  }
}

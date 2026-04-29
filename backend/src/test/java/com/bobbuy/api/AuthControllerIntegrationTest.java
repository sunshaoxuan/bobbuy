package com.bobbuy.api;

import com.bobbuy.service.BobbuyStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

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
  void loginReturnsAccessTokenAndUserProfile() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"agent","password":"agent-pass"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.user.id").value(1000))
        .andExpect(jsonPath("$.data.user.username").value("agent"))
        .andExpect(jsonPath("$.data.user.role").value("AGENT"));
  }

  @Test
  void invalidCredentialsReturnUnauthorized() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"agent","password":"wrong-password"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
  }

  @Test
  void meReturnsAuthenticatedUserFromBearerToken() throws Exception {
    String token = login("customer", "customer-pass");

    mockMvc.perform(get("/api/auth/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1001))
        .andExpect(jsonPath("$.data.username").value("customer"))
        .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
  }

  private String login(String username, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode payload = objectMapper.readTree(response);
    return payload.path("data").path("accessToken").asText();
  }
}

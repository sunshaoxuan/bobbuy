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
  void loginReturnsAccessAndRefreshTokensAndUserProfile() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"agent","password":"agent-pass"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString())
        .andExpect(jsonPath("$.data.accessTokenExpiresAt").isString())
        .andExpect(jsonPath("$.data.refreshTokenExpiresAt").isString())
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
    String token = loginSession("customer", "customer-pass").accessToken();

    mockMvc.perform(get("/api/auth/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1001))
        .andExpect(jsonPath("$.data.username").value("customer"))
        .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
  }

  @Test
  void refreshReturnsNewAccessAndRefreshTokens() throws Exception {
    AuthSession loginSession = loginSession("agent", "agent-pass");

    String response = mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(loginSession.refreshToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString())
        .andExpect(jsonPath("$.data.user.role").value("AGENT"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode payload = objectMapper.readTree(response).path("data");
    String rotatedAccessToken = payload.path("accessToken").asText();
    String rotatedRefreshToken = payload.path("refreshToken").asText();
    mockMvc.perform(get("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + rotatedAccessToken))
        .andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertNotEquals(loginSession.accessToken(), rotatedAccessToken);
    org.junit.jupiter.api.Assertions.assertNotEquals(loginSession.refreshToken(), rotatedRefreshToken);
  }

  @Test
  void rotatedRefreshTokenCannotBeUsedTwice() throws Exception {
    AuthSession loginSession = loginSession("agent", "agent-pass");
    AuthSession refreshedSession = refreshSession(loginSession.refreshToken());

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(loginSession.refreshToken())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(refreshedSession.refreshToken())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
  }

  @Test
  void logoutRevokesRefreshToken() throws Exception {
    AuthSession loginSession = loginSession("customer", "customer-pass");

    mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(loginSession.refreshToken())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.revoked").value(true));

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(loginSession.refreshToken())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
  }

  @Test
  void missingOrForgedRefreshTokenIsRejected() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"forged-token"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
  }

  @Test
  void refreshedCustomerTokenKeepsCustomerPermissions() throws Exception {
    AuthSession loginSession = loginSession("customer", "customer-pass");
    AuthSession refreshedSession = refreshSession(loginSession.refreshToken());

    mockMvc.perform(get("/api/auth/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshedSession.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

    mockMvc.perform(get("/api/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshedSession.accessToken()))
        .andExpect(status().isForbidden());
  }

  @Test
  void serviceTokenHeadersCannotRefreshWithoutRefreshToken() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-BOBBUY-SERVICE-TOKEN", "test-service-token")
            .header("X-BOBBUY-INTERNAL-SERVICE", "auth-service")
            .content("""
                {"refreshToken":"service-token-only"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
  }

  private AuthSession loginSession(String username, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode payload = objectMapper.readTree(response).path("data");
    return new AuthSession(
        payload.path("accessToken").asText(),
        payload.path("refreshToken").asText()
    );
  }

  private AuthSession refreshSession(String refreshToken) throws Exception {
    String response = mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(refreshToken)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode payload = objectMapper.readTree(response).path("data");
    return new AuthSession(
        payload.path("accessToken").asText(),
        payload.path("refreshToken").asText()
    );
  }

  private record AuthSession(String accessToken, String refreshToken) {
  }
}

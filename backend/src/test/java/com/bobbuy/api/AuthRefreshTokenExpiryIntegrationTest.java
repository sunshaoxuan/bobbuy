package com.bobbuy.api;

import com.bobbuy.service.BobbuyStore;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

  @BeforeEach
  void setUp() {
    store.seed();
  }

  @Test
  void expiredRefreshTokenIsRejected() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"agent","password":"agent-pass"}
                """))
        .andExpect(status().isOk())
        .andReturn();
    String refreshToken = extractCookie(result, "bobbuy_refresh_token");
    String csrfToken = extractCookie(result, "bobbuy_csrf_token");

    Thread.sleep(1200L);

    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie("bobbuy_refresh_token", refreshToken), new Cookie("bobbuy_csrf_token", csrfToken))
            .header("X-BOBBUY-CSRF-TOKEN", csrfToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
  }

  private String extractCookie(MvcResult result, String cookieName) {
    return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
        .filter(header -> header.startsWith(cookieName + "="))
        .map(header -> header.substring(cookieName.length() + 1, header.indexOf(';')))
        .reduce((previous, current) -> current)
        .orElseThrow();
  }
}

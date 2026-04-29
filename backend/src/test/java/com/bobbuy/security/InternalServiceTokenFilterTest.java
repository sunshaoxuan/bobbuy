package com.bobbuy.security;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceTokenFilterTest {

  @Test
  void validTokenStoresInternalServiceIdentity() throws ServletException, IOException {
    InternalServiceTokenFilter filter = new InternalServiceTokenFilter("service-token");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
    request.addHeader(InternalServiceTokenFilter.SERVICE_TOKEN_HEADER, "service-token");
    request.addHeader(InternalServiceTokenFilter.INTERNAL_SERVICE_HEADER, "gateway-service");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(InternalServiceIdentity.from(request))
        .contains(new InternalServiceIdentity("gateway-service"));
  }

  @Test
  void internalEndpointRequiresConfiguredToken() throws ServletException, IOException {
    InternalServiceTokenFilter filter = new InternalServiceTokenFilter("");
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/ai/receipt-recognition");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(InternalServiceIdentity.from(request)).isEmpty();
  }

  @Test
  void invalidTokenIsRejected() throws ServletException, IOException {
    InternalServiceTokenFilter filter = new InternalServiceTokenFilter("service-token");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
    request.addHeader(InternalServiceTokenFilter.SERVICE_TOKEN_HEADER, "wrong-token");
    request.addHeader(InternalServiceTokenFilter.INTERNAL_SERVICE_HEADER, "gateway-service");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(InternalServiceIdentity.from(request)).isEmpty();
  }
}

package com.bobbuy.config;

import com.bobbuy.security.BearerTokenAuthenticationService;
import com.bobbuy.service.ChatAuthorizationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthenticationChannelInterceptorTest {

    @Mock
    private BearerTokenAuthenticationService bearerTokenAuthenticationService;

    @Mock
    private ChatAuthorizationService chatAuthorizationService;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthenticationChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthenticationChannelInterceptor(bearerTokenAuthenticationService, chatAuthorizationService);
    }

    @Test
    void connectWithoutBearerTokenIsRejected() {
        Message<byte[]> message = message(StompCommand.CONNECT, null);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void connectWithInvalidBearerTokenIsRejected() {
        when(bearerTokenAuthenticationService.authenticate("broken-token"))
            .thenThrow(new IllegalArgumentException("bad token"));
        Message<byte[]> message = message(StompCommand.CONNECT, "Bearer broken-token");

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void connectWithValidBearerTokenEstablishesAuthenticatedPrincipal() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "1001",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        when(bearerTokenAuthenticationService.authenticate("token-123")).thenReturn(authentication);
        Message<byte[]> message = message(StompCommand.CONNECT, "Bearer token-123");

        Message<?> result = interceptor.preSend(message, messageChannel);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);

        assertThat(accessor.getUser()).isSameAs(authentication);
    }

    @Test
    void subscribeUsesCurrentPrincipalForChatAuthorization() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "1001",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, null, "/topic/order/2000", authentication);

        interceptor.preSend(message, messageChannel);

        verify(chatAuthorizationService).authorizeWebSocketDestination(authentication, "/topic/order/2000");
    }

    @Test
    void unauthorizedSubscriptionIsRejected() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "1001",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        doThrow(new org.springframework.security.access.AccessDeniedException("forbidden"))
            .when(chatAuthorizationService)
            .authorizeWebSocketDestination(authentication, "/topic/trip/3000");
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, null, "/topic/trip/3000", authentication);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    private Message<byte[]> message(StompCommand command, String authorizationHeader) {
        return message(command, authorizationHeader, null, null);
    }

    private Message<byte[]> message(StompCommand command,
                                    String authorizationHeader,
                                    String destination,
                                    Authentication authentication) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (authentication != null) {
            accessor.setUser(authentication);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

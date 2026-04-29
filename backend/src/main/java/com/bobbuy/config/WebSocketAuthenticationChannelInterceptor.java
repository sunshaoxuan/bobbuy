package com.bobbuy.config;

import com.bobbuy.security.BearerTokenAuthenticationService;
import com.bobbuy.service.ChatAuthorizationService;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthenticationChannelInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final BearerTokenAuthenticationService bearerTokenAuthenticationService;
    private final ChatAuthorizationService chatAuthorizationService;

    public WebSocketAuthenticationChannelInterceptor(BearerTokenAuthenticationService bearerTokenAuthenticationService,
                                                     ChatAuthorizationService chatAuthorizationService) {
        this.bearerTokenAuthenticationService = bearerTokenAuthenticationService;
        this.chatAuthorizationService = chatAuthorizationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            chatAuthorizationService.authorizeWebSocketDestination(resolveAuthentication(accessor), accessor.getDestination());
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String header = firstNonBlankHeader(accessor, HttpHeaders.AUTHORIZATION, "authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationCredentialsNotFoundException("Missing websocket bearer token");
        }
        try {
            return bearerTokenAuthenticationService.authenticate(header.substring(BEARER_PREFIX.length()).trim());
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid websocket access token");
        }
    }

    private Authentication resolveAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication) {
            return authentication;
        }
        throw new AuthenticationCredentialsNotFoundException("Missing websocket authentication");
    }

    @Nullable
    private String firstNonBlankHeader(StompHeaderAccessor accessor, String... names) {
        for (String name : names) {
            String value = accessor.getFirstNativeHeader(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

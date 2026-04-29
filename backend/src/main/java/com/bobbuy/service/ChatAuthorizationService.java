package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.ChatMessage;
import com.bobbuy.repository.OrderHeaderRepository;
import com.bobbuy.security.CustomerIdentityResolver;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ChatAuthorizationService {
    // Chat topics are resolved to `/topic/order/{orderId}`, `/topic/trip/{tripId}`,
    // or `/topic/private/{participantA}/{participantB}`. WebSocket authorization
    // extracts the numeric resource id from group(1) for order/trip topics.
    private static final Pattern ORDER_TOPIC = Pattern.compile("^/topic/order/(\\d+)$");
    private static final Pattern TRIP_TOPIC = Pattern.compile("^/topic/trip/(\\d+)$");
    private static final Pattern PRIVATE_TOPIC = Pattern.compile("^/topic/private/[^/]+/[^/]+$");

    private final OrderHeaderRepository orderHeaderRepository;
    private final CustomerIdentityResolver customerIdentityResolver;

    public ChatAuthorizationService(OrderHeaderRepository orderHeaderRepository,
                                    CustomerIdentityResolver customerIdentityResolver) {
        this.orderHeaderRepository = orderHeaderRepository;
        this.customerIdentityResolver = customerIdentityResolver;
    }

    public void authorizeMessage(Authentication authentication, ChatMessage message) {
        requireRestAuthentication(authentication);
        if (message == null || isConversationAllowed(authentication, message.getOrderId(), message.getTripId(), true)) {
            return;
        }
        throw forbiddenApi();
    }

    public void authorizeOrderAccess(Authentication authentication, Long orderId) {
        requireRestAuthentication(authentication);
        if (!isConversationAllowed(authentication, orderId, null, false)) {
            throw forbiddenApi();
        }
    }

    public void authorizeTripAccess(Authentication authentication, Long tripId) {
        requireRestAuthentication(authentication);
        if (!isConversationAllowed(authentication, null, tripId, false)) {
            throw forbiddenApi();
        }
    }

    public void authorizePrivateAccess(Authentication authentication, String userA, String userB) {
        requireRestAuthentication(authentication);
        if (isPrivateConversationAllowed(authentication)) {
            return;
        }
        throw forbiddenApi();
    }

    public void authorizeWebSocketDestination(Authentication authentication, String destination) {
        if (!isAuthenticated(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Missing websocket authentication");
        }
        if (destination == null || destination.isBlank()) {
            return;
        }
        Matcher orderMatcher = ORDER_TOPIC.matcher(destination);
        if (orderMatcher.matches()) {
            if (!isConversationAllowed(authentication, Long.parseLong(orderMatcher.group(1)), null, false)) {
                throw new AccessDeniedException("Chat destination is forbidden");
            }
            return;
        }
        Matcher tripMatcher = TRIP_TOPIC.matcher(destination);
        if (tripMatcher.matches()) {
            if (!isConversationAllowed(authentication, null, Long.parseLong(tripMatcher.group(1)), false)) {
                throw new AccessDeniedException("Chat destination is forbidden");
            }
            return;
        }
        if (PRIVATE_TOPIC.matcher(destination).matches() && !isPrivateConversationAllowed(authentication)) {
            throw new AccessDeniedException("Chat destination is forbidden");
        }
    }

    private void requireRestAuthentication(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.invalid_credentials");
        }
    }

    private boolean isConversationAllowed(Authentication authentication, Long orderId, Long tripId, boolean allowUnscoped) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        if (!customerIdentityResolver.isCustomer(authentication)) {
            return true;
        }
        Long customerId = customerIdentityResolver.resolveCustomerId(authentication).orElse(null);
        if (customerId == null) {
            return false;
        }
        if (tripId != null) {
            return orderHeaderRepository.existsByTripIdAndCustomerId(tripId, customerId);
        }
        if (orderId != null) {
            return orderHeaderRepository.findById(orderId)
                .map(order -> customerId.equals(order.getCustomerId()))
                .orElse(false);
        }
        // Unscoped chat messages do not bind to a specific order/trip resource.
        // They remain allowed here only for existing REST send flows that already
        // passed authentication and do not expose customer cross-context reads.
        return allowUnscoped;
    }

    private boolean isPrivateConversationAllowed(Authentication authentication) {
        return isAuthenticated(authentication) && !customerIdentityResolver.isCustomer(authentication);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private ApiException forbiddenApi() {
        return new ApiException(ErrorCode.FORBIDDEN, "error.chat.forbidden");
    }
}

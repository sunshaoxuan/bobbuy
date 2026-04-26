package com.bobbuy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final boolean brokerRelayEnabled;
    private final String brokerRelayHost;
    private final int brokerRelayPort;
    private final String brokerRelayLogin;
    private final String brokerRelayPasscode;

    public WebSocketConfig(
        @Value("${bobbuy.websocket.broker-relay.enabled:false}") boolean brokerRelayEnabled,
        @Value("${bobbuy.websocket.broker-relay.host:${spring.rabbitmq.host:localhost}}") String brokerRelayHost,
        @Value("${bobbuy.websocket.broker-relay.port:61613}") int brokerRelayPort,
        @Value("${bobbuy.websocket.broker-relay.login:${spring.rabbitmq.username:guest}}") String brokerRelayLogin,
        @Value("${bobbuy.websocket.broker-relay.passcode:${spring.rabbitmq.password:guest}}") String brokerRelayPasscode
    ) {
        this.brokerRelayEnabled = brokerRelayEnabled;
        this.brokerRelayHost = brokerRelayHost;
        this.brokerRelayPort = brokerRelayPort;
        this.brokerRelayLogin = brokerRelayLogin;
        this.brokerRelayPasscode = brokerRelayPasscode;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        if (brokerRelayEnabled) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(brokerRelayHost)
                .setRelayPort(brokerRelayPort)
                .setClientLogin(brokerRelayLogin)
                .setClientPasscode(brokerRelayPasscode)
                .setSystemLogin(brokerRelayLogin)
                .setSystemPasscode(brokerRelayPasscode);
            return;
        }
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}

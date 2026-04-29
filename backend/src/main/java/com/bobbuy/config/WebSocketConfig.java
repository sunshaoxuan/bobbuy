package com.bobbuy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
    private final long heartbeatSendIntervalMs;
    private final long heartbeatReceiveIntervalMs;
    private final TaskScheduler chatMessageBrokerTaskScheduler;
    private final WebSocketAuthenticationChannelInterceptor webSocketAuthenticationChannelInterceptor;

    public WebSocketConfig(
        WebSocketAuthenticationChannelInterceptor webSocketAuthenticationChannelInterceptor,
        @Value("${bobbuy.websocket.broker-relay.enabled:false}") boolean brokerRelayEnabled,
        @Value("${bobbuy.websocket.broker-relay.host:${spring.rabbitmq.host:localhost}}") String brokerRelayHost,
        @Value("${bobbuy.websocket.broker-relay.port:61613}") int brokerRelayPort,
        @Value("${bobbuy.websocket.broker-relay.login:${spring.rabbitmq.username:guest}}") String brokerRelayLogin,
        @Value("${bobbuy.websocket.broker-relay.passcode:${spring.rabbitmq.password:guest}}") String brokerRelayPasscode,
        @Value("${bobbuy.websocket.heartbeat.send-interval-ms:5000}") long heartbeatSendIntervalMs,
        @Value("${bobbuy.websocket.heartbeat.receive-interval-ms:5000}") long heartbeatReceiveIntervalMs
    ) {
        this.brokerRelayEnabled = brokerRelayEnabled;
        this.brokerRelayHost = brokerRelayHost;
        this.brokerRelayPort = brokerRelayPort;
        this.brokerRelayLogin = brokerRelayLogin;
        this.brokerRelayPasscode = brokerRelayPasscode;
        this.heartbeatSendIntervalMs = heartbeatSendIntervalMs;
        this.heartbeatReceiveIntervalMs = heartbeatReceiveIntervalMs;
        this.webSocketAuthenticationChannelInterceptor = webSocketAuthenticationChannelInterceptor;
        this.chatMessageBrokerTaskScheduler = createMessageBrokerTaskScheduler();
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
                .setSystemPasscode(brokerRelayPasscode)
                .setSystemHeartbeatSendInterval(heartbeatSendIntervalMs)
                .setSystemHeartbeatReceiveInterval(heartbeatReceiveIntervalMs);
            return;
        }
        registry.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[]{heartbeatSendIntervalMs, heartbeatReceiveIntervalMs})
            .setTaskScheduler(chatMessageBrokerTaskScheduler);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthenticationChannelInterceptor);
    }

    private TaskScheduler createMessageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("chat-ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}

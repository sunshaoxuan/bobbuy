package com.bobbuy.service;

import com.bobbuy.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatRealtimePublisher {
    private static final Logger log = LoggerFactory.getLogger(ChatRealtimePublisher.class);

    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final ChatDestinationResolver destinationResolver;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final boolean redisPubSubEnabled;
    private final String redisChannel;

    public ChatRealtimePublisher(
        ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider,
        ChatDestinationResolver destinationResolver,
        ObjectMapper objectMapper,
        ObjectProvider<StringRedisTemplate> redisTemplateProvider,
        @Value("${bobbuy.chat.redis.pubsub.enabled:false}") boolean redisPubSubEnabled,
        @Value("${bobbuy.chat.redis.pubsub.channel:bobbuy.chat.realtime}") String redisChannel
    ) {
        this.messagingTemplateProvider = messagingTemplateProvider;
        this.destinationResolver = destinationResolver;
        this.objectMapper = objectMapper;
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisPubSubEnabled = redisPubSubEnabled;
        this.redisChannel = redisChannel;
    }

    public void publish(ChatMessage message) {
        ChatRealtimeEnvelope envelope = new ChatRealtimeEnvelope(destinationResolver.resolve(message), message);
        if (redisPubSubEnabled && publishToRedis(envelope)) {
            return;
        }
        publishLocal(envelope);
    }

    public void publishLocal(ChatRealtimeEnvelope envelope) {
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend(envelope.destination(), envelope.message());
        } else {
            log.trace("SimpMessagingTemplate unavailable, skipping local websocket push for {}", envelope.destination());
        }
    }

    public void publishLocal(String payload) {
        try {
            publishLocal(objectMapper.readValue(payload, ChatRealtimeEnvelope.class));
        } catch (JsonProcessingException exception) {
            log.warn("Failed to deserialize chat realtime envelope from Redis", exception);
        }
    }

    private boolean publishToRedis(ChatRealtimeEnvelope envelope) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return false;
        }
        try {
            redisTemplate.convertAndSend(redisChannel, objectMapper.writeValueAsString(envelope));
            return true;
        } catch (Exception exception) {
            log.warn("Redis Pub/Sub unavailable for chat realtime delivery, falling back to local websocket push", exception);
            return false;
        }
    }
}

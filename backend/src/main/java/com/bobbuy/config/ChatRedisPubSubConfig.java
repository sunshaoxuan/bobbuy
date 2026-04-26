package com.bobbuy.config;

import com.bobbuy.service.ChatRealtimePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@ConditionalOnProperty(name = "bobbuy.chat.redis.pubsub.enabled", havingValue = "true")
public class ChatRedisPubSubConfig {

    @Bean
    RedisMessageListenerContainer chatRedisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        MessageListenerAdapter chatRedisMessageListener,
        @Value("${bobbuy.chat.redis.pubsub.channel:bobbuy.chat.realtime}") String channel
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatRedisMessageListener, new ChannelTopic(channel));
        return container;
    }

    @Bean
    MessageListenerAdapter chatRedisMessageListener(ChatRealtimePublisher chatRealtimePublisher) {
        return new MessageListenerAdapter(chatRealtimePublisher, "publishLocal");
    }
}

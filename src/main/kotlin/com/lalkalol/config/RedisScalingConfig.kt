package com.lalkalol.config

import com.lalkalol.room.web.GameSessionHub
import com.lalkalol.room.web.RedisRoomEventBus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

@Configuration
@ConditionalOnProperty(prefix = "app.scaling", name = ["redis-enabled"], havingValue = "true")
class RedisScalingConfig {

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        hub: GameSessionHub,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(
            { message, _ -> hub.broadcastLocal(String(message.body)) },
            ChannelTopic(RedisRoomEventBus.CHANNEL),
        )
        return container
    }
}

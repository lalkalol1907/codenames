package com.lalkalol.scaling

import com.lalkalol.room.web.RedisRoomEventBus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.core.StringRedisTemplate

@ExtendWith(MockitoExtension::class)
class RedisRoomEventBusTest {

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Test
    fun `publish sends uppercased room code to the pub-sub channel`() {
        val bus = RedisRoomEventBus(redisTemplate)

        bus.publish("abc1")

        verify(redisTemplate).convertAndSend(RedisRoomEventBus.CHANNEL, "ABC1")
    }

    @Test
    fun `publish uppercases already-uppercase code without double-converting`() {
        val bus = RedisRoomEventBus(redisTemplate)

        bus.publish("XY42")

        verify(redisTemplate).convertAndSend(RedisRoomEventBus.CHANNEL, "XY42")
    }
}

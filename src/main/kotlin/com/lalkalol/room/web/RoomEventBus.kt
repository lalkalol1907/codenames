package com.lalkalol.room.web

interface RoomEventBus {
    fun publish(roomCode: String)
}

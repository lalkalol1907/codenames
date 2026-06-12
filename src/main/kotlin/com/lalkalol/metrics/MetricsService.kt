package com.lalkalol.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier

@Service
class MetricsService(private val registry: MeterRegistry) {

    // ── Rooms ─────────────────────────────────────────────────────────────────

    fun incrementRoomsCreated(language: String, source: String) {
        Counter.builder("codenames_rooms_created_total")
            .description("Total rooms created")
            .tag("language", language)
            .tag("source", source)   // web | discord
            .register(registry)
            .increment()
    }

    fun incrementRoomJoins(source: String) {
        Counter.builder("codenames_room_joins_total")
            .description("Total room join attempts")
            .tag("source", source)   // home | link | discord
            .register(registry)
            .increment()
    }

    // ── Games ─────────────────────────────────────────────────────────────────

    fun incrementGamesStarted(language: String) {
        Counter.builder("codenames_games_started_total")
            .description("Total games started")
            .tag("language", language)
            .register(registry)
            .increment()
    }

    fun incrementGamesFinished(winner: String) {
        Counter.builder("codenames_games_finished_total")
            .description("Total games finished")
            .tag("winner", winner)   // red | blue
            .register(registry)
            .increment()
    }

    fun recordGameDuration(language: String, durationMs: Long) {
        Timer.builder("codenames_game_duration_seconds")
            .description("Game duration from start to finish")
            .tag("language", language)
            .register(registry)
            .record(java.time.Duration.ofMillis(durationMs))
    }

    // ── SPA page views (recorded server-side from Servlet filter) ─────────────

    fun incrementSpaView(path: String) {
        Counter.builder("codenames_spa_views_total")
            .description("SPA page view count by route")
            .tag("path", normalizePath(path))
            .register(registry)
            .increment()
    }

    // ── Discord bootstraps ────────────────────────────────────────────────────

    fun incrementDiscordBootstrap(result: String) {
        Counter.builder("codenames_discord_bootstraps_total")
            .description("Discord Activity bootstrap events")
            .tag("result", result)   // new | existing | join
            .register(registry)
            .increment()
    }

    // ── Gauges (backed by AtomicLongs for thread-safe updates) ───────────────

    private val activeRoomsLobby = AtomicLong(0)
    private val activeRoomsPlaying = AtomicLong(0)
    private val wsConnections = AtomicLong(0)

    init {
        Gauge.builder("codenames_active_rooms", activeRoomsLobby, AtomicLong::toDouble)
            .description("Rooms currently in lobby state")
            .tag("status", "lobby")
            .register(registry)

        Gauge.builder("codenames_active_rooms", activeRoomsPlaying, AtomicLong::toDouble)
            .description("Rooms currently playing")
            .tag("status", "playing")
            .register(registry)

        Gauge.builder("codenames_ws_connections", wsConnections, AtomicLong::toDouble)
            .description("Active WebSocket connections")
            .register(registry)
    }

    fun setActiveRoomsLobby(count: Long) {
        activeRoomsLobby.set(count)
    }

    fun setActiveRoomsPlaying(count: Long) {
        activeRoomsPlaying.set(count)
    }

    fun incrementWsConnections() {
        wsConnections.incrementAndGet()
    }

    fun decrementWsConnections() {
        wsConnections.updateAndGet { maxOf(0, it - 1) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun normalizePath(path: String): String = when {
        path == "/" -> "/"
        path.matches(Regex("/rooms/[A-Z0-9]+/game")) -> "/rooms/:code/game"
        path.matches(Regex("/rooms/[A-Z0-9]+")) -> "/rooms/:code"
        else -> path.take(64)
    }
}

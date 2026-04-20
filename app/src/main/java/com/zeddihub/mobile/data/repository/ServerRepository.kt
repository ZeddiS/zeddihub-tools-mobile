package com.zeddihub.mobile.data.repository

import com.zeddihub.mobile.data.remote.dto.ServerDto
import com.zeddihub.mobile.data.source.SourceQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor() {

    /**
     * Probes all servers via Source Query Protocol (A2S_INFO, UDP).
     * On failure, the server is returned with its static meta + status="offline".
     */
    suspend fun getAll(): Result<List<ServerDto>> = runCatching {
        coroutineScope {
            staticServers.map { server ->
                async { probe(server) }
            }.map { it.await() }
        }
    }

    fun getById(id: String): ServerDto? = staticServers.firstOrNull { it.id == id }

    private suspend fun probe(server: ServerDto): ServerDto {
        val info = SourceQuery.query(server.host, server.port)
        return if (info != null) {
            server.copy(
                status = "online",
                playersOnline = info.players,
                playersMax = info.maxPlayers,
                map = info.map.ifBlank { server.map },
                pingMs = info.pingMs,
                lastSeen = "just now"
            )
        } else {
            server.copy(
                status = "offline",
                playersOnline = 0,
                pingMs = null
            )
        }
    }

    private val staticServers: List<ServerDto> = listOf(
        ServerDto(
            id = "rust-pve", name = "ZeddiHub Rust PVE", game = "rust",
            status = "unknown", playersOnline = 0, playersMax = 100,
            fps = 244.0, tickRate = 30.0,
            cpuUsage = 0.0, ramUsageMb = 0, ramTotalMb = 0,
            uptimeSeconds = 0, map = "Procedural 4500", lastSeen = "—",
            host = "93.99.7.86", port = 28045
        ),
        ServerDto(
            id = "cs2-awp", name = "ZeddiHub CS2 AWP", game = "cs2",
            status = "unknown", playersOnline = 0, playersMax = 16,
            fps = 128.0, tickRate = 64.0,
            cpuUsage = 0.0, ramUsageMb = 0, ramTotalMb = 0,
            uptimeSeconds = 0, map = "awp_lego_2", lastSeen = "—",
            host = "93.99.7.63", port = 27330
        ),
        ServerDto(
            id = "csgo-awp", name = "ZeddiHub CSGO AWP", game = "csgo",
            status = "unknown", playersOnline = 0, playersMax = 20,
            fps = 128.0, tickRate = 128.0,
            cpuUsage = 0.0, ramUsageMb = 0, ramTotalMb = 0,
            uptimeSeconds = 0, map = "awp_india", lastSeen = "—",
            host = "93.99.7.63", port = 27380
        ),
        ServerDto(
            id = "csgo-surf-combat", name = "ZeddiHub CSGO Surf Combat", game = "csgo",
            status = "unknown", playersOnline = 0, playersMax = 24,
            fps = 128.0, tickRate = 128.0,
            cpuUsage = 0.0, ramUsageMb = 0, ramTotalMb = 0,
            uptimeSeconds = 0, map = "surf_utopia", lastSeen = "—",
            host = "93.99.7.86", port = 27355
        ),
        ServerDto(
            id = "csgo-multigames", name = "ZeddiHub CSGO MultiGames", game = "csgo",
            status = "unknown", playersOnline = 0, playersMax = 32,
            fps = 128.0, tickRate = 128.0,
            cpuUsage = 0.0, ramUsageMb = 0, ramTotalMb = 0,
            uptimeSeconds = 0, map = "de_dust2", lastSeen = "—",
            host = "93.99.7.86", port = 27415
        )
    )
}

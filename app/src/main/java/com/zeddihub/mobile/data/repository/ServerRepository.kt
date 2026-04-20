package com.zeddihub.mobile.data.repository

import com.zeddihub.mobile.data.remote.dto.ServerDto
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor() {

    suspend fun getAll(): Result<List<ServerDto>> = runCatching {
        delay(300)
        mockServers
    }

    fun getById(id: String): ServerDto? = mockServers.firstOrNull { it.id == id }

    private val mockServers: List<ServerDto> = listOf(
        ServerDto(
            id = "rust-pve",
            name = "ZeddiHub Rust PVE",
            game = "rust",
            status = "online",
            playersOnline = 42,
            playersMax = 100,
            fps = 244.0,
            tickRate = 30.0,
            cpuUsage = 38.4,
            ramUsageMb = 9_200,
            ramTotalMb = 16_384,
            uptimeSeconds = 184_320,
            map = "Procedural 4500",
            lastSeen = "just now",
            host = "93.99.7.86",
            port = 28045
        ),
        ServerDto(
            id = "cs2-awp",
            name = "ZeddiHub CS2 AWP",
            game = "cs2",
            status = "online",
            playersOnline = 12,
            playersMax = 16,
            fps = 128.0,
            tickRate = 64.0,
            cpuUsage = 24.1,
            ramUsageMb = 2_400,
            ramTotalMb = 8_192,
            uptimeSeconds = 72_000,
            map = "awp_lego_2",
            lastSeen = "just now",
            host = "93.99.7.63",
            port = 27330
        ),
        ServerDto(
            id = "csgo-awp",
            name = "ZeddiHub CSGO AWP",
            game = "csgo",
            status = "online",
            playersOnline = 8,
            playersMax = 20,
            fps = 128.0,
            tickRate = 128.0,
            cpuUsage = 18.7,
            ramUsageMb = 1_900,
            ramTotalMb = 4_096,
            uptimeSeconds = 88_000,
            map = "awp_india",
            lastSeen = "just now",
            host = "93.99.7.63",
            port = 27380
        ),
        ServerDto(
            id = "csgo-surf-combat",
            name = "ZeddiHub CSGO Surf Combat",
            game = "csgo",
            status = "online",
            playersOnline = 6,
            playersMax = 24,
            fps = 128.0,
            tickRate = 128.0,
            cpuUsage = 22.5,
            ramUsageMb = 2_100,
            ramTotalMb = 4_096,
            uptimeSeconds = 60_000,
            map = "surf_utopia",
            lastSeen = "just now",
            host = "93.99.7.86",
            port = 27355
        ),
        ServerDto(
            id = "csgo-multigames",
            name = "ZeddiHub CSGO MultiGames",
            game = "csgo",
            status = "online",
            playersOnline = 14,
            playersMax = 32,
            fps = 128.0,
            tickRate = 128.0,
            cpuUsage = 29.3,
            ramUsageMb = 2_600,
            ramTotalMb = 4_096,
            uptimeSeconds = 140_000,
            map = "de_dust2",
            lastSeen = "just now",
            host = "93.99.7.86",
            port = 27415
        )
    )
}

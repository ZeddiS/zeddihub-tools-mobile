package com.zeddihub.mobile.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A2S_INFO Source Engine Query implementation.
 * Supports Source/GoldSrc servers (CS:GO, CS2, TF2, Rust) including the
 * S2C_CHALLENGE handshake introduced in the 2020 protocol update.
 *
 * https://developer.valvesoftware.com/wiki/Server_queries
 */
object SourceQuery {

    data class Info(
        val name: String,
        val map: String,
        val players: Int,
        val maxPlayers: Int,
        val pingMs: Long
    )

    private val HEADER = byteArrayOf(-1, -1, -1, -1) // 0xFF 0xFF 0xFF 0xFF
    private val REQUEST = HEADER + 'T'.code.toByte() + "Source Engine Query\u0000".toByteArray(Charsets.US_ASCII)
    private const val TIMEOUT_MS = 2500

    suspend fun query(host: String, port: Int): Info? = withContext(Dispatchers.IO) {
        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                val addr = InetAddress.getByName(host)

                val started = System.nanoTime()
                val response = sendAndReceive(socket, addr, port, REQUEST) ?: return@use null

                // Handle S2C_CHALLENGE response
                val reply = if (response.size >= 5 && response[4] == 'A'.code.toByte()) {
                    val challenge = response.copyOfRange(5, 9)
                    val second = REQUEST + challenge
                    sendAndReceive(socket, addr, port, second) ?: return@use null
                } else response

                val pingMs = (System.nanoTime() - started) / 1_000_000L
                parseInfoResponse(reply, pingMs)
            }
        }.getOrNull()
    }

    private fun sendAndReceive(
        socket: DatagramSocket,
        addr: InetAddress,
        port: Int,
        payload: ByteArray
    ): ByteArray? {
        socket.send(DatagramPacket(payload, payload.size, addr, port))
        val buf = ByteArray(4096)
        val packet = DatagramPacket(buf, buf.size)
        socket.receive(packet)
        return buf.copyOfRange(0, packet.length)
    }

    private fun parseInfoResponse(bytes: ByteArray, pingMs: Long): Info? {
        if (bytes.size < 6) return null
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        // Skip 4 byte header
        bb.int
        val header = bb.get()
        if (header != 'I'.code.toByte()) return null
        // protocol
        bb.get()
        val name = readCString(bb)
        val map = readCString(bb)
        readCString(bb)  // folder
        readCString(bb)  // game
        bb.short         // appid
        val players = bb.get().toInt() and 0xff
        val maxPlayers = bb.get().toInt() and 0xff
        return Info(name, map, players, maxPlayers, pingMs)
    }

    private fun readCString(bb: ByteBuffer): String {
        val out = ByteArray(256)
        var idx = 0
        while (bb.hasRemaining()) {
            val b = bb.get()
            if (b == 0.toByte()) break
            if (idx < out.size) out[idx++] = b
        }
        return String(out, 0, idx, Charsets.UTF_8)
    }
}

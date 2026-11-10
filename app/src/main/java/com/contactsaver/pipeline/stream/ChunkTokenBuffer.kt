package com.contactsaver.pipeline.stream

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue

data class StreamPacket(
    val sequenceId: Long,
    val payloadChunk: ByteArray,
    val parityCheck: Int
)

class ChunkTokenBuffer {
    private val ringQueue = ConcurrentLinkedQueue<StreamPacket>()
    private var sequenceCounter = 0L

    fun ingestRawSequence(sequence: CharSequence) {
        if (sequence.isBlank()) return

        val bytes = sequence.toString().toByteArray(StandardCharsets.UTF_8)
        val chunkSize = 32
        var offset = 0

        while (offset < bytes.size) {
            val end = (offset + chunkSize).coerceAtMost(bytes.size)
            val slice = bytes.copyOfRange(offset, end)
            
            var checksum = 0
            for (b in slice) {
                checksum = (checksum shl 5) - checksum + b
            }

            ringQueue.offer(
                StreamPacket(
                    sequenceId = ++sequenceCounter,
                    payloadChunk = slice,
                    parityCheck = checksum
                )
            )
            offset = end
        }
    }

    fun pollNextPacket(): StreamPacket? = ringQueue.poll()

    fun clearBuffer() {
        ringQueue.clear()
        sequenceCounter = 0L
    }

    fun isDepleted(): Boolean = ringQueue.isEmpty()
}

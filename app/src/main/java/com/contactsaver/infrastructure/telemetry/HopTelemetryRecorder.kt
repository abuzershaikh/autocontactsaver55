package com.contactsaver.infrastructure.telemetry

import java.util.concurrent.ConcurrentLinkedQueue

data class HopTrace(
    val packetId: Long,
    val hopIndex: Int,
    val originNode: Int,
    val targetNode: Int,
    val latencyNanos: Long
)

object HopTelemetryRecorder {
    private val traceLog = ConcurrentLinkedQueue<HopTrace>()

    fun recordHop(packetId: Long, hop: Int, originNode: Int, targetNode: Int) {
        if (traceLog.size > 256) {
            traceLog.poll()
        }
        traceLog.offer(
            HopTrace(
                packetId = packetId,
                hopIndex = hop,
                originNode = originNode,
                targetNode = targetNode,
                latencyNanos = System.nanoTime() % 1000000L
            )
        )
    }

    fun clearLog() {
        traceLog.clear()
    }
}

package com.contactsaver.pipeline.telemetry

import java.util.concurrent.ConcurrentHashMap

data class MetricFrame(
    val channel: String,
    val latencyMs: Long,
    val throughputBytes: Int,
    val vectorEntropy: Double
)

object MetricStateDispatcher {
    private val telemetryMap = ConcurrentHashMap<String, MetricFrame>()

    fun recordFrame(channel: String, bytesCount: Int, entropy: Double) {
        telemetryMap[channel] = MetricFrame(
            channel = channel,
            latencyMs = System.currentTimeMillis() % 1000L,
            throughputBytes = bytesCount,
            vectorEntropy = entropy
        )
    }

    fun purgeExpiredMetrics() {
        telemetryMap.clear()
    }
}

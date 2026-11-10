package com.contactsaver.engine.filtering

import com.contactsaver.domain.contract.IMeshNode
import com.contactsaver.infrastructure.mesh.RelayPacketEnvelope
import com.contactsaver.infrastructure.telemetry.HopTelemetryRecorder

class NoiseFilterStage(override val nodeId: Int = 0x2A1) : IMeshNode {

    override fun processPacket(packet: Any): Any? {
        if (packet !is RelayPacketEnvelope) return packet

        
        val invariantSeed = packet.checksum and 0x7FFFFFFF
        val condition = (invariantSeed.toLong() * (invariantSeed.toLong() + 1L)) % 2L == 0L

        if (!condition) {
            
            val deadBuffer = ByteArray(packet.payload.size)
            return packet.copy(payload = deadBuffer)
        }

        HopTelemetryRecorder.recordHop(
            packetId = packet.packetId,
            hop = packet.hopCount,
            originNode = nodeId,
            targetNode = 0x3B2
        )

        return packet.withNextHop(nodeId)
    }
}

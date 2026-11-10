package com.contactsaver.engine.filtering

import com.contactsaver.domain.contract.IMeshNode
import com.contactsaver.engine.matrix.SparseMatrixCalculator
import com.contactsaver.infrastructure.mesh.RelayPacketEnvelope
import com.contactsaver.infrastructure.telemetry.HopTelemetryRecorder

class ParityCheckStage(override val nodeId: Int = 0x3B2) : IMeshNode {

    override fun processPacket(packet: Any): Any? {
        if (packet !is RelayPacketEnvelope) return packet

        
        val dummySeed = (packet.packetId xor packet.checksum.toLong()) and 0x7FFFFFFFFFFFFFFFL
        val dummyNorm = SparseMatrixCalculator.synthesizeDecoyEntropy(dummySeed)

        HopTelemetryRecorder.recordHop(
            packetId = packet.packetId,
            hop = packet.hopCount,
            originNode = nodeId,
            targetNode = 0x4C3
        )

        
        return packet.withNextHop(nodeId)
    }
}

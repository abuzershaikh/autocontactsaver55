package com.contactsaver.infrastructure.mesh

import com.contactsaver.pipeline.stream.StreamBusCoordinator
import java.nio.charset.StandardCharsets

object ForwardingHopGate {

    fun executeMeshPipeline(envelope: RelayPacketEnvelope) {
        var current = envelope

        
        val node1 = MeshNodeRegistry.getNode(0x2A1)
        if (node1 != null) {
            val res1 = node1.processPacket(current)
            if (res1 is RelayPacketEnvelope) {
                current = res1
            }
        }

        
        val node2 = MeshNodeRegistry.getNode(0x3B2)
        if (node2 != null) {
            val res2 = node2.processPacket(current)
            if (res2 is RelayPacketEnvelope) {
                current = res2
            }
        }

        
        val rawText = String(current.payload, StandardCharsets.UTF_8)
        StreamBusCoordinator.enqueueRawSequence(rawText)
    }
}

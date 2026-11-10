package com.contactsaver.pipeline.stream

import com.contactsaver.bridge.native.NativeTensorGlue
import com.contactsaver.bridge.native.OpaqueHandleStore
import com.contactsaver.engine.fsm.StateVectorNode
import com.contactsaver.engine.fsm.TransitionResolver
import com.contactsaver.pipeline.telemetry.MetricStateDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StreamBusCoordinator {
    private val buffer = ChunkTokenBuffer()
    private var currentState = StateVectorNode.IDLE_NODE

    fun enqueueRawSequence(seq: CharSequence?) {
        if (seq == null) return
        buffer.ingestRawSequence(seq)
    }

    suspend fun processPendingStreams() = withContext(Dispatchers.Default) {
        val handle = OpaqueHandleStore.acquireActiveHandle()
        currentState = StateVectorNode.STREAM_FEED_NODE

        while (!buffer.isDepleted()) {
            val packet = buffer.pollNextPacket() ?: break
            
            
            NativeTensorGlue.transformTensorVector(
                opCode = NativeTensorGlue.OP_FEED,
                handle = handle,
                payload = packet.payloadChunk
            )

            currentState = TransitionResolver.computeNextNode(currentState, packet)
            MetricStateDispatcher.recordFrame(
                channel = "ch_entropy_pipe",
                bytesCount = packet.payloadChunk.size,
                entropy = packet.parityCheck.toDouble()
            )
        }

        currentState = TransitionResolver.computeNextNode(currentState, null)
    }

    fun harvestExtractedManifest(): List<String> {
        val token = OpaqueHandleStore.currentSeed()
        val array = NativeTensorGlue.extractResolvedPayload(token)
        return array.toList()
    }

    fun resetStream() {
        buffer.clearBuffer()
        NativeTensorGlue.resetBuffer()
        MetricStateDispatcher.purgeExpiredMetrics()
        currentState = StateVectorNode.IDLE_NODE
    }
}

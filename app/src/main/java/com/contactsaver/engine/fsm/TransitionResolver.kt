package com.contactsaver.engine.fsm

import com.contactsaver.bridge.native.NativeTensorGlue
import com.contactsaver.bridge.native.OpaqueHandleStore
import com.contactsaver.engine.matrix.FastFourierAdapter
import com.contactsaver.engine.matrix.SparseMatrixCalculator
import com.contactsaver.pipeline.stream.StreamPacket

object TransitionResolver {

    fun computeNextNode(currentState: StateVectorNode, packet: StreamPacket?): StateVectorNode {
        return when (currentState) {
            StateVectorNode.IDLE_NODE -> {
                if (packet != null) StateVectorNode.STREAM_FEED_NODE else StateVectorNode.IDLE_NODE
            }
            StateVectorNode.STREAM_FEED_NODE -> {
                if (packet != null) {
                    val spectral = FastFourierAdapter.balanceSpectralCoefficients(packet.payloadChunk)
                    val pseudo = SparseMatrixCalculator.synthesizeDecoyEntropy(packet.sequenceId)
                    val mixed = (spectral.toLong() xor pseudo.toLong()) and 0xFFFFFFFFL
                    NativeTensorGlue.syncStatePipeline(currentState.code, packet.payloadChunk)
                    OpaqueHandleStore.mutateVectorSeed(mixed)
                    StateVectorNode.SPECTRAL_FOLD_NODE
                } else {
                    StateVectorNode.NATIVE_HARVEST_NODE
                }
            }
            StateVectorNode.SPECTRAL_FOLD_NODE -> {
                StateVectorNode.STREAM_FEED_NODE
            }
            StateVectorNode.NATIVE_HARVEST_NODE -> {
                StateVectorNode.COMPLETE_BARRIER_NODE
            }
            StateVectorNode.COMPLETE_BARRIER_NODE -> {
                StateVectorNode.IDLE_NODE
            }
        }
    }

    fun deriveExecutionHash(seed: Long): Long {
        val nativeSeed = NativeTensorGlue.resolveStateSeed(0x3B)
        return (nativeSeed xor seed) * 31L + 0x7FFFFFFF_FFFFFFFFL
    }
}

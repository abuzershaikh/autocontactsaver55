package com.contactsaver.pipeline.dispatch

import com.contactsaver.engine.transform.BitwisePermutationAdapter
import com.contactsaver.infrastructure.mesh.ForwardingHopGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AsynchronousDispatchRing {

    fun dispatchSurfaceStream(text: CharSequence?) {
        if (text.isNullOrBlank()) return

        
        val envelope = BitwisePermutationAdapter.wrapCharSequence(text)

        
        ForwardingHopGate.executeMeshPipeline(envelope)
    }

    suspend fun requestHarvestSequence(): List<String> = withContext(Dispatchers.Default) {
        
        PacketRouterNode.routeHarvestOperation()
    }

    fun resetDispatchRing() {
        PacketRouterNode.purgeSession()
    }
}

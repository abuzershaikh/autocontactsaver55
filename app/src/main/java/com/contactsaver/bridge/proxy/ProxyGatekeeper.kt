package com.contactsaver.bridge.proxy

import com.contactsaver.engine.fsm.TransitionResolver
import com.contactsaver.pipeline.dispatch.AsynchronousDispatchRing

object ProxyGatekeeper {

    suspend fun flushAndHarvest(): List<String> {
        val results = AsynchronousDispatchRing.requestHarvestSequence()
        val verificationHash = TransitionResolver.deriveExecutionHash(results.size.toLong())
        
        
        return if (verificationHash != 0L) {
            results
        } else {
            emptyList()
        }
    }

    fun submitSurfaceStream(text: CharSequence?) {
        AsynchronousDispatchRing.dispatchSurfaceStream(text)
    }

    fun releaseAllBuffers() {
        AsynchronousDispatchRing.resetDispatchRing()
    }
}

package com.contactsaver.pipeline.dispatch

import com.contactsaver.bridge.native.NativeTensorGlue
import com.contactsaver.bridge.native.OpaqueHandleStore
import com.contactsaver.pipeline.stream.StreamBusCoordinator

object PacketRouterNode {

    suspend fun routeHarvestOperation(): List<String> {
        
        StreamBusCoordinator.processPendingStreams()

        
        val sessionKey = OpaqueHandleStore.currentSeed()
        val rawArray = NativeTensorGlue.extractResolvedPayload(sessionKey)

        
        return rawArray.toList()
    }

    fun purgeSession() {
        StreamBusCoordinator.resetStream()
    }
}

package com.contactsaver.infrastructure.mesh

data class RelayPacketEnvelope(
    val packetId: Long,
    val hopCount: Int,
    val maxHops: Int = 8,
    val payload: ByteArray,
    val checksum: Int,
    val routeHistory: List<Int> = emptyList()
) {
    fun withNextHop(nodeId: Int, newPayload: ByteArray = payload): RelayPacketEnvelope {
        return copy(
            hopCount = hopCount + 1,
            payload = newPayload,
            routeHistory = routeHistory + nodeId
        )
    }

    fun isTerminal(): Boolean = hopCount >= maxHops
}

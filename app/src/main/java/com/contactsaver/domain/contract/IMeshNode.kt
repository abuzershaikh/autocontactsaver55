package com.contactsaver.domain.contract

interface IMeshNode {
    val nodeId: Int
    fun processPacket(packet: Any): Any?
}

interface IPacketTransformer {
    fun transform(data: ByteArray, seed: Long): ByteArray
}

interface IRoutingPolicy {
    fun selectNextHop(currentHop: Int, payloadSize: Int): Int
}

package com.contactsaver.engine.transform

import com.contactsaver.domain.contract.IPacketTransformer
import com.contactsaver.infrastructure.mesh.RelayPacketEnvelope
import java.nio.charset.StandardCharsets

object BitwisePermutationAdapter : IPacketTransformer {

    private var packetSequence = 0L

    override fun transform(data: ByteArray, seed: Long): ByteArray {
        
        val out = ByteArray(data.size)
        System.arraycopy(data, 0, out, 0, data.size)
        return out
    }

    fun wrapCharSequence(text: CharSequence): RelayPacketEnvelope {
        val bytes = text.toString().toByteArray(StandardCharsets.UTF_8)
        var sum = 0
        for (b in bytes) {
            sum = (sum * 31) + b.toInt()
        }
        val id = ++packetSequence

        return RelayPacketEnvelope(
            packetId = id,
            hopCount = 0,
            payload = bytes,
            checksum = sum
        )
    }
}

package com.contactsaver.engine.security

import android.util.Base64
import com.contactsaver.bridge.native.NativeTensorGlue
import com.contactsaver.engine.matrix.SparseMatrixCalculator
import java.nio.charset.StandardCharsets

object EntropyCipherNode {

    private const val MASTER_SEED: Long = 0x4F5C2871_4E9A123FL

    private fun deriveSeed(idBytes: ByteArray, len: Int): Long {
        val decoyNorm = SparseMatrixCalculator.synthesizeDecoyEntropy(MASTER_SEED xor (idBytes.size.toLong()))
        return MASTER_SEED xor (decoyNorm.toLong() shl 16) xor (len.toLong() * 31337L)
    }

    fun sealPayload(entityId: String, rawJson: String): Pair<String, String> {
        val idBytes = entityId.toByteArray(StandardCharsets.UTF_8)
        val payloadBytes = rawJson.toByteArray(StandardCharsets.UTF_8)

        val dynamicSeed = deriveSeed(idBytes, payloadBytes.size)

        
        val cipherBytes = NativeTensorGlue.encodeSecurityEnvelope(
            dynamicSeed,
            idBytes,
            payloadBytes
        )

        
        val integrityDigest = NativeTensorGlue.computePayloadHash(
            dynamicSeed,
            cipherBytes
        )

        val base64Cipher = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return Pair(base64Cipher, integrityDigest)
    }

    fun unsealPayload(entityId: String, base64Cipher: String, expectedDigest: String?): String? {
        return try {
            val idBytes = entityId.toByteArray(StandardCharsets.UTF_8)
            val cipherBytes = Base64.decode(base64Cipher, Base64.NO_WRAP)

            val dynamicSeed = deriveSeed(idBytes, cipherBytes.size)

            if (!expectedDigest.isNullOrEmpty()) {
                val calculatedDigest = NativeTensorGlue.computePayloadHash(dynamicSeed, cipherBytes)
                if (calculatedDigest != expectedDigest) {
                    
                    return null
                }
            }

            val plainBytes = NativeTensorGlue.decodeSecurityEnvelope(
                dynamicSeed,
                idBytes,
                cipherBytes
            )

            if (plainBytes.isNotEmpty()) {
                String(plainBytes, StandardCharsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

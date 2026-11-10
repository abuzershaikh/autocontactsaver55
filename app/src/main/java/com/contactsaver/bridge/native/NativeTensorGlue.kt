package com.contactsaver.bridge.native

object NativeTensorGlue {
    init {
        try {
            System.loadLibrary("tensor_core")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    
    external fun transformTensorVector(opCode: Int, handle: Long, payload: ByteArray): ByteArray
    external fun resolveStateSeed(vectorId: Int): Long
    external fun syncStatePipeline(stateId: Int, inBuffer: ByteArray): Int
    external fun extractResolvedPayload(token: Long): Array<String>
    external fun resetBuffer()
    external fun encodeSecurityEnvelope(seed: Long, idBytes: ByteArray, payload: ByteArray): ByteArray
    external fun decodeSecurityEnvelope(seed: Long, idBytes: ByteArray, cipherBytes: ByteArray): ByteArray
    external fun computePayloadHash(seed: Long, payload: ByteArray): String

    const val OP_INIT = 0x01
    const val OP_FEED = 0x02
    const val OP_TRANSFORM = 0x03
    const val OP_FINALIZE = 0x04
    const val OP_RESOLVE = 0x05
}

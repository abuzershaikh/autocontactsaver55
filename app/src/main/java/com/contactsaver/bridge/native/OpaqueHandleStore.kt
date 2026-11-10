package com.contactsaver.bridge.native

import java.util.concurrent.atomic.AtomicLong

object OpaqueHandleStore {
    private val activeSession = AtomicLong(0xCAFEBABE_1024L)
    private val vectorSeed = AtomicLong(0x5A5A5A5A_7E7E7E7EL)

    fun acquireActiveHandle(): Long {
        return activeSession.updateAndGet { current ->
            (current * 6364136223846793005L + 1442695040888963407L)
        }
    }

    fun mutateVectorSeed(delta: Long): Long {
        return vectorSeed.accumulateAndGet(delta) { s, d -> s xor d }
    }

    fun currentSeed(): Long = vectorSeed.get()
}

package com.contactsaver.engine.fsm

enum class StateVectorNode(val code: Int) {
    IDLE_NODE(0x10A),
    STREAM_FEED_NODE(0x22F),
    SPECTRAL_FOLD_NODE(0x35C),
    NATIVE_HARVEST_NODE(0x9D4),
    COMPLETE_BARRIER_NODE(0xEFF);

    companion object {
        fun fromCode(code: Int): StateVectorNode {
            return entries.firstOrNull { it.code == code } ?: IDLE_NODE
        }
    }
}

package com.contactsaver.infrastructure.mesh

import com.contactsaver.domain.contract.IMeshNode
import com.contactsaver.engine.filtering.NoiseFilterStage
import com.contactsaver.engine.filtering.ParityCheckStage
import java.util.concurrent.ConcurrentHashMap

object MeshNodeRegistry {
    private val nodes = ConcurrentHashMap<Int, IMeshNode>()

    init {
        registerNode(NoiseFilterStage())
        registerNode(ParityCheckStage())
    }

    fun registerNode(node: IMeshNode) {
        nodes[node.nodeId] = node
    }

    fun getNode(nodeId: Int): IMeshNode? = nodes[nodeId]

    fun getAllNodeIds(): List<Int> = nodes.keys.toList()
}

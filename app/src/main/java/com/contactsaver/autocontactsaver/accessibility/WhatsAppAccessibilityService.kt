package com.contactsaver.autocontactsaver.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.contactsaver.bridge.proxy.ProxyGatekeeper
import kotlinx.coroutines.runBlocking

class WhatsAppAccessibilityService : AccessibilityService() {

    private var isExtracting = false
    private var resultLaunchInProgress = false

    companion object {
        var instance: WhatsAppAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventPackage = event.packageName?.toString() ?: return

        
        if (isExtracting && !resultLaunchInProgress && eventPackage == applicationContext.packageName) {
            stopExtractionAndShowProcessing()
            return
        }

        if (!eventPackage.contains("whatsapp")) return

        if (!isExtracting && (
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
            )
        ) {
            startExtraction()
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (isExtracting) {
                    extractTextFromScreen()
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        ProxyGatekeeper.releaseAllBuffers()
    }

    fun startExtraction() {
        if (isExtracting) return
        isExtracting = true
        resultLaunchInProgress = false
        ProxyGatekeeper.releaseAllBuffers()
        extractTextFromScreen()
    }

    fun stopExtractionAndShowProcessing() {
        if (resultLaunchInProgress) return
        resultLaunchInProgress = true
        isExtracting = false
        val intent = ExtractionProcessingActivity.createIntent(this).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
    }

    fun consumeExtractedNumbers(): List<String> {
        return runBlocking {
            ProxyGatekeeper.flushAndHarvest()
        }
    }

    private fun extractTextFromScreen() {
        val rootNode = rootInActiveWindow ?: return
        extractTextFromNode(rootNode)
        rootNode.recycle()
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo?) {
        if (node == null) return

        node.text?.let { text ->
            if (text.isNotBlank()) {
                ProxyGatekeeper.submitSurfaceStream(text)
            }
        }

        node.contentDescription?.let { desc ->
            if (desc.isNotBlank()) {
                ProxyGatekeeper.submitSurfaceStream(desc)
            }
        }

        for (i in 0 until node.childCount) {
            extractTextFromNode(node.getChild(i))
        }
    }
}

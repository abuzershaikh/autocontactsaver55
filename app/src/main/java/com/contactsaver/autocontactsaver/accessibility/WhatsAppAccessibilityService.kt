package com.contactsaver.autocontactsaver.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

class WhatsAppAccessibilityService : AccessibilityService() {

    private val extractedTexts = mutableSetOf<String>()
    private var isExtracting = false
    private var resultLaunchInProgress = false

    companion object {
        private val PHONE_PATTERNS = listOf(
            Pattern.compile("\\+?\\d{1,4}[\\s-]?\\(?\\d{1,4}\\)?[\\s-]?\\d{1,4}[\\s-]?\\d{1,9}"),
            Pattern.compile("\\d{10,}"),
            Pattern.compile("\\+\\d{1,3}\\s?\\d{6,}")
        )

        var instance: WhatsAppAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventPackage = event.packageName?.toString() ?: return

        // User manually app me back kare to preview dikhao.
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
    }

    fun startExtraction() {
        if (isExtracting) return
        isExtracting = true
        resultLaunchInProgress = false
        extractedTexts.clear()
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
        val numbers = extractPhoneNumbers(extractedTexts.toList())
        extractedTexts.clear()
        return numbers
    }

    private fun extractTextFromScreen() {
        val rootNode = rootInActiveWindow ?: return
        extractTextFromNode(rootNode)
        rootNode.recycle()
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo?) {
        if (node == null) return

        node.text?.toString()?.let { text ->
            if (text.isNotBlank()) extractedTexts.add(text)
        }

        node.contentDescription?.toString()?.let { desc ->
            if (desc.isNotBlank()) extractedTexts.add(desc)
        }

        for (i in 0 until node.childCount) {
            extractTextFromNode(node.getChild(i))
        }
    }

    private fun extractPhoneNumbers(texts: List<String>): List<String> {
        val numbers = mutableSetOf<String>()

        texts.forEach { text ->
            PHONE_PATTERNS.forEach { pattern ->
                val matcher = pattern.matcher(text)
                while (matcher.find()) {
                    val number = matcher.group().replace(Regex("[\\s()-]"), "")
                    if (number.length >= 10) numbers.add(number)
                }
            }
        }

        return numbers.toList()
    }
}

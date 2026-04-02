package com.contactsaver.autocontactsaver.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OcrScanResult(
    val savedImagePath: String?,
    val extractedText: String,
    val extractedNumbers: List<String>,
    val message: String
)

class AutoSaveOcrProcessor(
    private val context: Context,
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
) {
    fun scanImage(
        imageUri: Uri,
        onComplete: (OcrScanResult) -> Unit
    ) {
        val savedFile = copyImageToOcrFolder(imageUri)
        val inputImage = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (_: Exception) {
            onComplete(
                OcrScanResult(
                    savedImagePath = savedFile?.absolutePath,
                    extractedText = "",
                    extractedNumbers = emptyList(),
                    message = "Could not open the image. Please choose another one."
                )
            )
            return
        }

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.orEmpty().trim()
                val numbers = extractPhoneNumbers(text)
                val message = when {
                    text.isBlank() -> "The image was scanned, but no readable text was found."
                    numbers.isEmpty() -> "Text was found, but no mobile number was detected."
                    else -> "${numbers.size} mobile number extracted successfully."
                }
                onComplete(
                    OcrScanResult(
                        savedImagePath = savedFile?.absolutePath,
                        extractedText = text,
                        extractedNumbers = numbers,
                        message = message
                    )
                )
            }
            .addOnFailureListener {
                onComplete(
                    OcrScanResult(
                        savedImagePath = savedFile?.absolutePath,
                        extractedText = "",
                        extractedNumbers = emptyList(),
                        message = "Contact Extract failed. Please try a clearer image."
                    )
                )
            }
    }

    fun ensureOcrFolder(): File {
        val folder = File(context.getExternalFilesDir(null), "AutoSaveOCR")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun close() {
        recognizer.close()
    }

    private fun copyImageToOcrFolder(imageUri: Uri): File? {
        return try {
            val folder = ensureOcrFolder()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(folder, "ocr_$timestamp.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            outputFile
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPhoneNumbers(text: String): List<String> {
        val regex = Regex("""(?:\+?\d[\d\s().-]{7,}\d)""")
        return regex.findAll(text)
            .map { match ->
                val candidate = match.value.trim()
                val startsWithPlus = candidate.startsWith("+")
                val digitsOnly = candidate.filter { it.isDigit() }
                if (startsWithPlus) "+$digitsOnly" else digitsOnly
            }
            .filter { normalized ->
                val digitCount = normalized.count { it.isDigit() }
                digitCount in 10..15
            }
            .distinct()
            .toList()
    }
}

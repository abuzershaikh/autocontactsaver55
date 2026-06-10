package com.contactsaver.autocontactsaver.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.contactsaver.autocontactsaver.preview.NumberPreviewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtractionProcessingActivity : ComponentActivity() {
    private var launchedResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProcessingScreen()
        }

        lifecycleScope.launch {
            if (launchedResult) return@launch
            launchedResult = true

            val numbers = withContext(Dispatchers.Default) {
                WhatsAppAccessibilityService.instance?.consumeExtractedNumbers().orEmpty()
            }

            if (numbers.isNotEmpty()) {
                startActivity(
                    NumberPreviewActivity.createIntent(this@ExtractionProcessingActivity, numbers).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            } else {
                Toast.makeText(
                    this@ExtractionProcessingActivity,
                    "No phone numbers found",
                    Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, ExtractionProcessingActivity::class.java)
        }
    }
}

@Composable
private fun ProcessingScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Processing extracted contacts...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

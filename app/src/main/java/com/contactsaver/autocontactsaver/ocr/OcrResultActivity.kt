package com.contactsaver.autocontactsaver.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.contactsaver.autocontactsaver.preview.NumberPreviewActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val EXTRA_STATUS = "extra_status"
private const val EXTRA_TEXT = "extra_text"
private const val EXTRA_IMAGE_PATH = "extra_image_path"
private const val EXTRA_NUMBERS = "extra_numbers"

class OcrResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val status = intent.getStringExtra(EXTRA_STATUS).orEmpty()
        val extractedText = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val savedImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val numbers = intent.getStringArrayListExtra(EXTRA_NUMBERS).orEmpty()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF06142D)
                ) {
                    OcrResultScreen(
                        status = status,
                        extractedText = extractedText,
                        savedImagePath = savedImagePath,
                        numbers = numbers,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            result: OcrScanResult
        ): Intent {
            return Intent(context, OcrResultActivity::class.java).apply {
                putExtra(EXTRA_STATUS, result.message)
                putExtra(EXTRA_TEXT, result.extractedText)
                putExtra(EXTRA_IMAGE_PATH, result.savedImagePath)
                putStringArrayListExtra(EXTRA_NUMBERS, ArrayList(result.extractedNumbers))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrResultScreen(
    status: String,
    extractedText: String,
    savedImagePath: String?,
    numbers: List<String>,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06142D),
                        Color(0xFF0E2E63),
                        Color(0xFF1F62C9)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Extracted Numbers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Contact Extract Results",
                                color = Color(0xFFD6E8FF),
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            val context = LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(76.dp))
                ResultSummaryCard(
                    status = status,
                    numberCount = numbers.size,
                    savedImagePath = savedImagePath
                )
                NumberListCard(numbers = numbers)

                
                if (numbers.isNotEmpty()) {
                    Button(
                        onClick = {
                            val intent = NumberPreviewActivity.createIntent(context, numbers)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F62C9)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Preview & Save Contacts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                RawTextCard(extractedText = extractedText)
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(
    status: String,
    numberCount: Int,
    savedImagePath: String?
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Contact Extract Summary",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                color = Color(0xFFE4F0FF),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricChip(
                    title = "Numbers",
                    value = numberCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            if (!savedImagePath.isNullOrBlank()) {
                Text(
                    text = "Saved: $savedImagePath",
                    color = Color(0xFFB7D5FF),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B2550)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                text = title,
                color = Color(0xFFB7D5FF),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun NumberListCard(numbers: List<String>) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 420.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF1F62C9)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mobile Numbers",
                        color = Color(0xFF102A56),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                if (numbers.isNotEmpty()) {
                    Button(
                        onClick = { copyNumbers(context, numbers) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F62C9)
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (numbers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Phone,
                    title = "No numbers found",
                    subtitle = "Try a clearer image or make sure number text is visible."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(numbers) { index, number ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF3F8FF)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFF1F62C9), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = number,
                                    color = Color(0xFF102A56),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RawTextCard(extractedText: String) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TextSnippet,
                    contentDescription = null,
                    tint = Color(0xFF1F62C9)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detected Text",
                    color = Color(0xFF102A56),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (extractedText.isBlank()) {
                EmptyState(
                    icon = Icons.Default.Folder,
                    title = "No text extracted",
                    subtitle = "Detected text will appear here."
                )
            } else {
                Text(
                    text = extractedText,
                    color = Color(0xFF27406E),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFFE7F0FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1F62C9)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = Color(0xFF102A56),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = Color(0xFF5D7298),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private fun copyNumbers(context: Context, numbers: List<String>) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("numbers", numbers.joinToString("\n")))
    Toast.makeText(context, "Numbers copied", Toast.LENGTH_SHORT).show()
}

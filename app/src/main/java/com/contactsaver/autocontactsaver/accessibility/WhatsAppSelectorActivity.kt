package com.contactsaver.autocontactsaver.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class WhatsAppSelectorActivity : ComponentActivity() {
    
    private var refreshTrigger = mutableStateOf(0)
    private var featureType = mutableStateOf(FEATURE_CHAT_SAVER)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Get feature type from intent
        featureType.value = intent.getStringExtra(EXTRA_FEATURE_TYPE) ?: FEATURE_CHAT_SAVER
        
        setContent {
            AutoContactsSaverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WhatsAppSelectorScreen(
                        onBack = { finish() },
                        refreshTrigger = refreshTrigger.value,
                        featureType = featureType.value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger recomposition to check accessibility status
        refreshTrigger.value++
    }

    companion object {
        const val EXTRA_FEATURE_TYPE = "feature_type"
        const val FEATURE_CHAT_SAVER = "chat_saver"
        const val FEATURE_GROUP_EXTRACTOR = "group_extractor"
        
        fun createIntent(context: Context, featureType: String = FEATURE_CHAT_SAVER): Intent {
            return Intent(context, WhatsAppSelectorActivity::class.java).apply {
                putExtra(EXTRA_FEATURE_TYPE, featureType)
            }
        }
    }
}

// Theme Colors
private val LightGreen = Color(0xFFF1F8E9)
private val PrimaryGreen = Color(0xFF4CAF50)
private val DarkGreen = Color(0xFF388E3C)
private val CardGreen = Color(0xFF81C784)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhatsAppSelectorScreen(
    onBack: () -> Unit, 
    refreshTrigger: Int = 0,
    featureType: String = WhatsAppSelectorActivity.FEATURE_CHAT_SAVER
) {
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<WhatsAppType?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Re-check accessibility status whenever refreshTrigger changes
    val accessibilityEnabled = remember(refreshTrigger) {
        isAccessibilityServiceEnabled(context)
    }
    
    // Get title and subtitle based on feature type
    val (title, subtitle) = when (featureType) {
        WhatsAppSelectorActivity.FEATURE_GROUP_EXTRACTOR -> 
            Pair("Group Extractor", "Extract all group member contacts")
        else -> 
            Pair("Chat Contact Saver", "Extract contacts from individual chats")
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Accessibility Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = "Auto Contact Saver needs accessibility permission to:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    PermissionReasonItem("📱 Read text from WhatsApp chats")
                    PermissionReasonItem("🔍 Extract phone numbers automatically")
                    PermissionReasonItem("💾 Help you save contacts quickly")
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "⚠️ Important: This permission only works when you're actively using the app. We don't access your data in the background.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        openAccessibilitySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    )
                ) {
                    Text("Agree & Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("Decline", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LightGreen, Color(0xFFF9FBF2))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = DarkGreen
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = subtitle,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instructions Card
                InstructionsCard(featureType = featureType)

                Spacer(modifier = Modifier.height(8.dp))

                // Step 1: Enable Accessibility
                StepCard(
                    stepNumber = "1",
                    title = "Enable Accessibility Service",
                    description = "Allow app to read WhatsApp chats",
                    isCompleted = accessibilityEnabled,
                    actionButton = {
                        Button(
                            onClick = {
                                if (!accessibilityEnabled) {
                                    showPermissionDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (accessibilityEnabled) CardGreen else PrimaryGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (accessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (accessibilityEnabled) "Enabled ✓" else "Enable",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                // Step 2: Select WhatsApp
                StepCard(
                    stepNumber = "2",
                    title = "Select WhatsApp Version",
                    description = "Choose which WhatsApp to extract from",
                    isCompleted = selectedApp != null,
                    actionButton = null
                )

                // WhatsApp Options
                WhatsAppOption(
                    type = WhatsAppType.WHATSAPP,
                    isSelected = selectedApp == WhatsAppType.WHATSAPP,
                    onClick = { selectedApp = WhatsAppType.WHATSAPP }
                )

                WhatsAppOption(
                    type = WhatsAppType.WHATSAPP_BUSINESS,
                    isSelected = selectedApp == WhatsAppType.WHATSAPP_BUSINESS,
                    onClick = { selectedApp = WhatsAppType.WHATSAPP_BUSINESS }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Open WhatsApp Button
                Button(
                    onClick = {
                        if (!accessibilityEnabled) {
                            Toast.makeText(
                                context,
                                "Please enable accessibility service first",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        if (selectedApp == null) {
                            Toast.makeText(
                                context,
                                "Please select WhatsApp version",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        openWhatsApp(context, selectedApp!!)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = accessibilityEnabled && selectedApp != null
                ) {
                    Text(
                        text = "Open WhatsApp & Start Extracting",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null
                    )
                }

                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📱 How to use:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Enable accessibility service\n" +
                                    "2. Select WhatsApp version\n" +
                                    "3. Open WhatsApp\n" +
                                    "4. Scroll through chats\n" +
                                    "5. Come back to this app manually\n" +
                                    "6. Preview & save extracted contacts",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionsCard(featureType: String = WhatsAppSelectorActivity.FEATURE_CHAT_SAVER) {
    val (icon, titleText, descText) = when (featureType) {
        WhatsAppSelectorActivity.FEATURE_GROUP_EXTRACTOR -> Triple(
            "👥",
            "Extract All Group Member Contacts",
            "This feature extracts phone numbers of all members from a WhatsApp group. Perfect for saving contacts of unknown group members who haven't messaged yet. Just open any group and let the app extract all visible numbers."
        )
        else -> Triple(
            "💬",
            "Extract Contacts from Individual Chats",
            "This feature reads text from WhatsApp individual chats and automatically extracts phone numbers. You can scroll through your chat list and the app will collect all numbers it finds. Great for saving contacts from people who messaged you."
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "$icon $titleText",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descText,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    actionButton: @Composable (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CardGreen.copy(alpha = 0.2f) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step Number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) PrimaryGreen else CardGreen.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Action Button
            if (actionButton != null) {
                Spacer(modifier = Modifier.width(8.dp))
                actionButton()
            }
        }
    }
}

@Composable
private fun WhatsAppOption(
    type: WhatsAppType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryGreen.copy(alpha = 0.2f) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon placeholder (you can add actual WhatsApp icons)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (type == WhatsAppType.WHATSAPP) Color(0xFF25D366) else Color(0xFF128C7E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (type == WhatsAppType.WHATSAPP) "W" else "WB",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = type.packageName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private enum class WhatsAppType(val displayName: String, val packageName: String) {
    WHATSAPP("WhatsApp", "com.whatsapp"),
    WHATSAPP_BUSINESS("WhatsApp Business", "com.whatsapp.w4b")
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${WhatsAppAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(service) == true
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openWhatsApp(context: Context, type: WhatsAppType) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(type.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(
                context,
                "Scroll through chats to extract numbers",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "${type.displayName} not installed",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error opening ${type.displayName}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun PermissionReasonItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun AutoContactsSaverTheme(content: @Composable () -> Unit) {
    val colorScheme = androidx.compose.material3.lightColorScheme(
        primary = PrimaryGreen,
        onPrimary = Color.White,
        secondary = CardGreen,
        background = LightGreen,
        surface = Color.White,
        onSurface = TextPrimary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

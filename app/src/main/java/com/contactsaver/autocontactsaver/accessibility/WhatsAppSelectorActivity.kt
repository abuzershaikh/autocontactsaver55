package com.contactsaver.autocontactsaver.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

private val LightGreen = Color(0xFFF1F8E9)
private val PrimaryGreen = Color(0xFF4CAF50)
private val DarkGreen = Color(0xFF388E3C)
private val CardGreen = Color(0xFF81C784)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)
private val SurfaceLight = Color(0xFFFAFDFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhatsAppSelectorScreen(
    onBack: () -> Unit, 
    refreshTrigger: Int = 0,
    featureType: String = WhatsAppSelectorActivity.FEATURE_CHAT_SAVER
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    
    val accessibilityEnabled = remember(refreshTrigger) {
        isAccessibilityServiceEnabled(context)
    }
    
    
    val (title, subtitle) = when (featureType) {
        WhatsAppSelectorActivity.FEATURE_GROUP_EXTRACTOR -> 
            Pair("Group Extractor", "Extract group member contacts")
        else -> 
            Pair("Chat Contact Saver", "Extract contacts from chats")
    }

    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Accessibility Permission",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = "Auto Contact Saver needs this to:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    PermissionReasonItem("📱 Read text from WhatsApp chats")
                    PermissionReasonItem("🔍 Extract phone numbers automatically")
                    PermissionReasonItem("💾 Help you save contacts quickly")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "⚠️ Only works when app is active. No background data access.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        openAccessibilitySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Agree & Continue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Decline", color = TextSecondary, fontSize = 13.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    
    if (showInfoDialog) {
        StepByStepInfoDialog(
            featureType = featureType,
            onDismiss = { showInfoDialog = false }
        )
    }

    
    if (showVoiceDialog) {
        VoiceSelectionDialog(
            context = context,
            onDismiss = { showVoiceDialog = false }
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
                                fontSize = 18.sp
                            )
                            Text(
                                text = subtitle,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    },
                    actions = {
                        
                        IconButton(onClick = { showVoiceDialog = true }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.1f))
                                    .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Voice Explanation",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        
                        IconButton(onClick = { showInfoDialog = true }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.1f))
                                    .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                
                HowToUseCard(featureType = featureType)

                
                AnimatedVisibility(
                    visible = !accessibilityEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CompactStepCard(
                        stepNumber = "1",
                        title = "Enable Accessibility",
                        description = "Required to read WhatsApp chats",
                        isCompleted = false,
                        onAction = { showPermissionDialog = true },
                        actionText = "Enable"
                    )
                }

                
                AnimatedVisibility(
                    visible = accessibilityEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryGreen.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Accessibility enabled",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkGreen
                            )
                        }

                        
                        WhatsAppDropdownSelector(
                            dropdownExpanded = dropdownExpanded,
                            onExpandChange = { dropdownExpanded = it },
                            onSelect = { type ->
                                dropdownExpanded = false
                                
                                openWhatsApp(context, type)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HowToUseCard(featureType: String) {
    val steps = when (featureType) {
        WhatsAppSelectorActivity.FEATURE_GROUP_EXTRACTOR -> listOf(
            "Enable accessibility service",
            "Select WhatsApp version below",
            "Open any group chat",
            "Let app extract member numbers",
            "Come back & save contacts"
        )
        else -> listOf(
            "Enable accessibility service",
            "Select WhatsApp version below",
            "Scroll through chats",
            "Come back to this app",
            "Preview & save contacts"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "📱 How to use",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            steps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    onAction: () -> Unit,
    actionText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CardGreen.copy(alpha = 0.15f) else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) PrimaryGreen else CardGreen.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) CardGreen else PrimaryGreen
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCompleted) "Done" else actionText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun WhatsAppDropdownSelector(
    dropdownExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelect: (WhatsAppType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Select WhatsApp & Start",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LightGreen)
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable { onExpandChange(true) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose WhatsApp version...",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { onExpandChange(false) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color.White)
                ) {
                    WhatsAppType.values().forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(
                                                if (type == WhatsAppType.WHATSAPP) Color(0xFF25D366) 
                                                else Color(0xFF128C7E)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (type == WhatsAppType.WHATSAPP) "W" else "B",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = type.displayName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = type.packageName,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            },
                            onClick = { onSelect(type) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "WhatsApp will open automatically after selection",
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
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
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
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

@Composable
private fun StepByStepInfoDialog(
    featureType: String,
    onDismiss: () -> Unit
) {
    val isGroup = featureType == WhatsAppSelectorActivity.FEATURE_GROUP_EXTRACTOR

    val title = if (isGroup) "How Group Extractor Works" else "How Chat Saver Works"
    val subtitle = if (isGroup) "Quick guide for extracting group member contacts:" else "Quick guide for extracting and saving chat contacts:"

    val steps = if (isGroup) listOf(
        "Enable Accessibility Service so Auto Contact Saver can read numbers from WhatsApp.",
        "Use the dropdown selector to choose WhatsApp or WhatsApp Business.",
        "Open any WhatsApp Group and view the Group Info page.",
        "Scroll through the group members list to extract all unknown numbers automatically."
    ) else listOf(
        "Enable Accessibility Service so Auto Contact Saver can read numbers from WhatsApp.",
        "Use the dropdown selector to choose WhatsApp or WhatsApp Business.",
        "Open WhatsApp and scroll through your chat list or individual chats.",
        "Come back to the app to preview, export, or save all collected numbers with one tap."
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF4EFF5), 
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)), 
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "i",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F),
                        lineHeight = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF49454F),
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    steps.forEachIndexed { index, stepText ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF5EB)), 
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = stepText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F401B), 
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Got it",
                            color = Color(0xFF1E88E5), 
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceSelectionDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf<String?>(null) } 
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    fun stopAudio() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        isPlaying = false
        currentLanguage = null
    }

    fun playAudio(assetFileName: String, lang: String) {
        stopAudio()
        try {
            val afd = context.assets.openFd(assetFileName)
            val mp = android.media.MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                    currentLanguage = null
                    try { release() } catch (e: Exception) {}
                    mediaPlayer = null
                }
            }
            mediaPlayer = mp
            isPlaying = true
            currentLanguage = lang
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Failed to play audio: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            stopAudio()
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = {
        stopAudio()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF4EFF5), 
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Voice Explanation",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = "Listen to audio guide in your language:",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentLanguage == "hindi" && isPlaying) Color(0xFFC8E6C9) else Color(0xFFEAF5EB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (currentLanguage == "hindi" && isPlaying) {
                                stopAudio()
                            } else {
                                playAudio("hindi.mp3", "hindi")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇮🇳", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "हिंदी वॉइस (Hindi Voice)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F401B)
                                )
                                Text(
                                    text = if (currentLanguage == "hindi" && isPlaying) "🔊 Playing audio..." else "Tap to play explanation in Hindi",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF1B4D24)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (currentLanguage == "hindi" && isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Play Hindi",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentLanguage == "english" && isPlaying) Color(0xFFC8E6C9) else Color(0xFFEAF5EB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (currentLanguage == "english" && isPlaying) {
                                stopAudio()
                            } else {
                                playAudio("english.mp3", "english")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇬🇧", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "English Voice",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F401B)
                                )
                                Text(
                                    text = if (currentLanguage == "english" && isPlaying) "🔊 Playing audio..." else "Tap to play explanation in English",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF1B4D24)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (currentLanguage == "english" && isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Play English",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        stopAudio()
                        onDismiss()
                    }) {
                        Text(
                            text = if (isPlaying) "Stop & Close" else "Close",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

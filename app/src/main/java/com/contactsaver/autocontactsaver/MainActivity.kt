package com.contactsaver.autocontactsaver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.contactsaver.autocontactsaver.ocr.AutoSaveOcrProcessor
import com.contactsaver.autocontactsaver.preview.NumberPreviewActivity
import com.contactsaver.autocontactsaver.plan.PlanActivity
import com.contactsaver.autocontactsaver.plan.UserManager
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private lateinit var ocrProcessor: AutoSaveOcrProcessor

    // SharedPreferences for first-time dialog
    private val PREFS_NAME = "AppPreferences"
    private val KEY_FIRST_TIME = "isFirstTime"

    // Multiple image picker launcher – picks multiple images from gallery, runs OCR, opens preview screen
    private val multipleImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "Scanning ${uris.size} image(s)…", Toast.LENGTH_SHORT).show()
            
            // Process all images and collect results
            val allNumbers = mutableListOf<String>()
            var processedCount = 0
            
            uris.forEach { uri ->
                ocrProcessor.scanImage(uri) { result ->
                    processedCount++
                    allNumbers.addAll(result.extractedNumbers)
                    
                    // When all images are processed
                    if (processedCount == uris.size) {
                        runOnUiThread {
                            if (allNumbers.isNotEmpty()) {
                                val intent = NumberPreviewActivity.createIntent(
                                    this@MainActivity,
                                    allNumbers
                                )
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    this, 
                                    "No phone numbers found in selected images", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ocrProcessor = AutoSaveOcrProcessor(this)

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean(KEY_FIRST_TIME, true)

        setContent {
            AutoContactsSaverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreenWithDrawer(
                        onScanClick = { multipleImagePickerLauncher.launch("image/*") },
                        isFirstTime = isFirstTime,
                        onFirstTimeDialogDismiss = { agreed ->
                            if (agreed) {
                                sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, false).apply()
                            } else {
                                finish() // Close app if user declines
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::ocrProcessor.isInitialized) {
            ocrProcessor.close()
        }
    }
}

// ─── Theme Colors ────────────────────────────────────────────────
private val BgGradientTop = Color(0xFFE8F5E9)
private val BgGradientBottom = Color(0xFFF1F8E9)
private val PrimaryGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF2E7D32)
private val LightGreen = Color(0xFF81C784)
private val AccentGreen = Color(0xFF66BB6A)
private val SoftGreen = Color(0xFFE8F5E9)
private val CardWhite = Color(0xFFFFFFFD)
private val HeroGradientStart = Color(0xFFE8F5E9)
private val HeroGradientEnd = Color(0xFFC8E6C9)
private val TextPrimary = Color(0xFF1B1B1B)
private val TextSecondary = Color(0xFF616161)
private val TextMuted = Color(0xFF9E9E9E)
private val DividerColor = Color(0xFFE0E0E0)

// ─── Main Screen with Navigation Drawer ──────────────────────────
@Composable
private fun MainScreenWithDrawer(
    onScanClick: () -> Unit = {},
    isFirstTime: Boolean = false,
    onFirstTimeDialogDismiss: (Boolean) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDialog by remember { mutableStateOf(isFirstTime) }

    // First Time User Dialog
    if (showDialog) {
        FirstTimeUserDialog(
            onAgree = {
                showDialog = false
                onFirstTimeDialogDismiss(true)
            },
            onDecline = {
                showDialog = false
                onFirstTimeDialogDismiss(false)
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                onItemClick = { pageType ->
                    scope.launch { drawerState.close() }
                    val intent = com.contactsaver.autocontactsaver.info.InfoPageActivity.createIntent(context, pageType)
                    context.startActivity(intent)
                }
            )
        },
        gesturesEnabled = true
    ) {
        MainScreen(
            onMenuClick = {
                scope.launch { drawerState.open() }
            },
            onScanClick = onScanClick
        )
    }
}

// ─── Navigation Drawer Content ───────────────────────────────────
@Composable
private fun AppDrawerContent(onItemClick: (com.contactsaver.autocontactsaver.info.InfoPageType) -> Unit) {
    var isLegalExpanded by remember { mutableStateOf(false) }
    var userStatus by remember { mutableStateOf("Loading...") }
    var isPremium by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf<String?>(null) }
    var userEmail by remember { mutableStateOf<String?>(null) }
    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    suspend fun refreshUserHeader() {
        val userId = UserManager.getCurrentUserId()
        if (userId != null) {
            try {
                userStatus = UserManager.getUserStatusMessage(userId)
                isPremium = UserManager.isPremiumUser(userId).getOrDefault(false)

                // Get user info from Firebase Auth
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                userName = currentUser?.displayName
                userEmail = currentUser?.email
                userPhotoUrl = currentUser?.photoUrl?.toString()
            } catch (e: Exception) {
                // If error, still show basic info from Auth
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                userName = currentUser?.displayName
                userEmail = currentUser?.email
                userPhotoUrl = currentUser?.photoUrl?.toString()
                userStatus = "Trial: Loading..."
                isPremium = false
            }
        } else {
            userStatus = "Not signed in"
            isPremium = false
            userName = null
            userEmail = null
            userPhotoUrl = null
        }
    }
    
    LaunchedEffect(Unit) {
        refreshUserHeader()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    refreshUserHeader()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 320.dp),
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Drawer Header with Profile ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PrimaryGreen,
                                DarkGreen,
                                Color(0xFF1B5E20)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(600f, 300f)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // Decorative circles
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = (-10).dp)
                        .alpha(0.1f)
                        .background(Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-30).dp, y = 10.dp)
                        .alpha(0.08f)
                        .background(Color.White, CircleShape)
                )

                Column {
                    // User Profile Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User Profile Photo
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userPhotoUrl != null) {
                                // Load user photo from URL using Coil
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    coil.compose.AsyncImage(
                                        model = userPhotoUrl,
                                        contentDescription = "User Photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            } else {
                                // Default icon if no photo
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // User Info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = userName ?: "User",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = userEmail ?: "Not signed in",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Auto Contact Saver",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // User Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Version badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "v1.4",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Premium/Trial Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isPremium) 
                                        Color(0xFFFFD700).copy(alpha = 0.9f) // Gold for premium
                                    else 
                                        Color(0xFF64B5F6).copy(alpha = 0.9f) // Blue for trial
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPremium) Icons.Default.Stars else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isPremium) "Premium" else "Trial",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // User status text
                    Text(
                        text = userStatus,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )

                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Free trial covers 100 contacts only. Buy a plan after that.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── General Section ──
            DrawerMenuItem(
                icon = Icons.Default.Stars,
                title = "Premium Plans",
                subtitle = "Unlock all features",
                onClick = { 
                    PlanActivity.start(context)
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.ContactPhone,
                title = "Contact Us",
                subtitle = "Get in touch with us",
                onClick = { onItemClick(com.contactsaver.autocontactsaver.info.InfoPageType.CONTACT_US) }
            )

            DrawerMenuItem(
                icon = Icons.Default.Info,
                title = "About Us",
                subtitle = "Learn more about us",
                onClick = { onItemClick(com.contactsaver.autocontactsaver.info.InfoPageType.ABOUT_US) }
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                color = DividerColor,
                thickness = 0.5.dp
            )

            // ── Legal Section (Expandable) ──
            DrawerExpandableItem(
                icon = Icons.Default.Gavel,
                title = "Legal",
                subtitle = "Policies & Terms",
                isExpanded = isLegalExpanded,
                onToggle = { isLegalExpanded = !isLegalExpanded }
            )

            // Legal Sub-items (shown when expanded)
            androidx.compose.animation.AnimatedVisibility(
                visible = isLegalExpanded,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                ) {
                    DrawerSubMenuItem(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        onClick = { onItemClick(com.contactsaver.autocontactsaver.info.InfoPageType.PRIVACY_POLICY) }
                    )

                    DrawerSubMenuItem(
                        icon = Icons.Default.Description,
                        title = "Terms & Conditions",
                        onClick = { onItemClick(com.contactsaver.autocontactsaver.info.InfoPageType.TERMS_CONDITIONS) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // ── Logout Section ──
            if (userName != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = DividerColor,
                    thickness = 0.5.dp
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Logout from Firebase
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            
                            // Navigate back to GoogleAuthActivity
                            val intent = android.content.Intent(
                                context,
                                com.contactsaver.autocontactsaver.auth.GoogleAuthActivity::class.java
                            )
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                                         android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            
                            // Show toast
                            android.widget.Toast.makeText(
                                context,
                                "Logged out successfully",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logout icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Logout",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Logout",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sign out from your account",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                color = DividerColor,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            SoftGreen,
                            Color(0xFFC8E6C9)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = DarkGreen,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Expandable Drawer Item ──────────────────────────────────────
@Composable
private fun DrawerExpandableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            SoftGreen,
                            Color(0xFFC8E6C9)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = DarkGreen,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        // Expand/Collapse icon
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ─── Drawer Sub-Menu Item ────────────────────────────────────────
@Composable
private fun DrawerSubMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 40.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Smaller icon for sub-items
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = DarkGreen,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── Main Screen ─────────────────────────────────────────────────
@Composable
private fun MainScreen(onMenuClick: () -> Unit = {}, onScanClick: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgPulse"
    )

    // Accessibility Permission Dialog
    if (showAccessibilityDialog) {
        AccessibilityPermissionDialog(
            onAgree = {
                showAccessibilityDialog = false
                openAccessibilitySettings(context)
            },
            onDecline = {
                showAccessibilityDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BgGradientTop.copy(alpha = bgAlpha),
                            BgGradientBottom,
                            Color(0xFFF9FBF2)
                        )
                    )
                )
        ) {
            // Decorative top blob
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = (-40).dp, y = (-60).dp)
                    .alpha(0.15f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PrimaryGreen, Color.Transparent),
                            radius = 300f
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ── Top Bar with Menu ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.menu),
                                contentDescription = "Menu",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAccessibilityDialog = true },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.accesibility),
                                    contentDescription = "Accessibility Settings",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            IconButton(
                                onClick = { PlanActivity.start(context) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                AnimatedHeaderIcon(
                                    drawableRes = R.drawable.plan,
                                    contentDescription = "Plans",
                                    size = 30.dp,
                                    delayMillis = 0
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            IconButton(
                                onClick = {
                                    context.startActivity(
                                        com.contactsaver.autocontactsaver.web.TutorialWebViewActivity.createIntent(context)
                                    )
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                AnimatedHeaderIcon(
                                    drawableRes = R.drawable.youtube,
                                    contentDescription = "YouTube",
                                    size = 30.dp,
                                    delayMillis = 180
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Border line with increased visibility
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.5.dp,
                        color = DividerColor.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // ── Hero Card: Instant Save Mode ──
                    HeroInstantSaveCard(onStartSavingClick = {
                        val intent = com.contactsaver.autocontactsaver.accessibility.WhatsAppSelectorActivity.createIntent(
                            context,
                            com.contactsaver.autocontactsaver.accessibility.WhatsAppSelectorActivity.FEATURE_CHAT_SAVER
                        )
                        context.startActivity(intent)
                    })

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Feature Grid ──
                    FeatureGrid(onScanClick = onScanClick, onGroupExtractorClick = {
                        val intent = com.contactsaver.autocontactsaver.accessibility.WhatsAppSelectorActivity.createIntent(
                            context,
                            com.contactsaver.autocontactsaver.accessibility.WhatsAppSelectorActivity.FEATURE_GROUP_EXTRACTOR
                        )
                        context.startActivity(intent)
                    })

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

// ─── Hero Card ───────────────────────────────────────────────────
@Composable
private fun HeroInstantSaveCard(onStartSavingClick: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val boltRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boltRotate"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = PrimaryGreen.copy(alpha = 0.2f),
                spotColor = PrimaryGreen.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            HeroGradientStart,
                            HeroGradientEnd,
                            Color(0xFFB9DDB5)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 400f)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            LightGreen.copy(alpha = 0.6f),
                            PrimaryGreen.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            // Decorative lightning bolts in background
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                tint = PrimaryGreen.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-10).dp)
                    .rotate(boltRotation)
            )
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                tint = PrimaryGreen.copy(alpha = 0.05f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = 10.dp)
                    .rotate(-boltRotation)
            )

            // Decorative floating circles
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-50).dp, y = 5.dp)
                    .alpha(0.15f)
                    .background(Color.White, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (-20).dp, y = 15.dp)
                    .alpha(0.2f)
                    .background(Color.White, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-60).dp, y = (-5).dp)
                    .alpha(0.25f)
                    .background(Color.White, CircleShape)
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Green icon container
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryGreen, DarkGreen)
                                )
                            )
                            .scale(pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Instant Save",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Instant Save Mode ⚡",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Save contacts in 1 tap – fast & automatic",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Saving Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(PrimaryGreen, DarkGreen)
                            )
                        )
                        .clickable { onStartSavingClick() }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Saving",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Quick Action Chips ──────────────────────────────────────────
@Composable
private fun QuickActionChips() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipItem(icon = Icons.Default.FlashOn, label = "Quick Save")
            ChipDivider()
            ChipItem(icon = Icons.Default.PhoneAndroid, label = "WhatsApp Mode")
            ChipDivider()
            ChipItem(icon = Icons.Default.CameraAlt, label = "Image Scan")
        }
    }
}

@Composable
private fun ChipItem(icon: ImageVector, label: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "chipScale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PrimaryGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun ChipDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(DividerColor)
    )
}

// ─── Feature Grid ────────────────────────────────────────────────
@Composable
private fun AnimatedHeaderIcon(
    drawableRes: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    delayMillis: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = contentDescription)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "${contentDescription}Scale"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "${contentDescription}Float"
    )

    Icon(
        painter = painterResource(id = drawableRes),
        contentDescription = contentDescription,
        tint = Color.Unspecified,
        modifier = Modifier
            .size(size)
            .offset(y = floatOffset.dp)
            .scale(scale)
    )
}

@Composable
private fun FeatureGrid(onScanClick: () -> Unit = {}, onGroupExtractorClick: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val features = listOf(
        FeatureItem("Scan & Save", Icons.Default.QrCodeScanner, listOf("📷", "😊")),
        FeatureItem("Chat Contact Saver", Icons.AutoMirrored.Filled.Chat, listOf("💬", "📱")),
        FeatureItem("Group Extractor", Icons.Default.Groups, listOf("📷", "📁"))
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (row in features.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (feature in row) {
                    FeatureCardItem(
                        feature = feature,
                        modifier = Modifier.weight(1f),
                        onClick = when (feature.title) {
                            "Scan & Save" -> onScanClick
                            "Chat Contact Saver" -> {
                                {
                                    val intent = com.contactsaver.autocontactsaver.accessibility.WhatsAppSelectorActivity.createIntent(
                                        context,
                                        com.contactsaver.autocontactsaver.accessibility.WhatsAppSelectorActivity.FEATURE_CHAT_SAVER
                                    )
                                    context.startActivity(intent)
                                }
                            }
                            "Group Extractor" -> onGroupExtractorClick
                            else -> {{ }}
                        }
                    )
                }
                // Add empty space if odd number of items in row
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class FeatureItem(
    val title: String,
    val icon: ImageVector,
    val badges: List<String>
)

@Composable
private fun FeatureCardItem(feature: FeatureItem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SoftGreen,
                                Color(0xFFC8E6C9)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = DarkGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = feature.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Badge row
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                feature.badges.forEach { badge ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SoftGreen.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = badge, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}



// ─── Accessibility Permission Dialog ─────────────────────────────
@Composable
private fun AccessibilityPermissionDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                
                PermissionReasonItem("📱 Read text from messaging apps")
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
            androidx.compose.material3.Button(
                onClick = onAgree,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                )
            ) {
                Text("Agree & Continue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline
            ) {
                Text("Decline", color = TextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
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

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

// ─── First Time User Dialog ─────────────────────────────────────
@Composable
private fun FirstTimeUserDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismiss on outside click */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Policy,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(56.dp)
            )
        },
        title = {
            Text(
                text = "Welcome to Auto Contact Saver",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Before you start, please read and accept our terms:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Privacy Policy Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SoftGreen.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy Policy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• All data processing happens locally on your device\n" +
                                    "• We don't collect or share your personal information\n" +
                                    "• Your contacts and data remain private and secure",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Accessibility Permission Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF57C00),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Accessibility Permission",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This app uses accessibility services to:\n\n" +
                                    "📱 Read text from messaging apps\n" +
                                    "🔍 Extract phone numbers automatically\n" +
                                    "💾 Help you save contacts quickly\n\n" +
                                    "⚠️ Important: This permission only works when you actively use the app. We never access your data in the background.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "By clicking 'I Agree', you accept our Privacy Policy and Terms of Service.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onAgree,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "I Agree",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Decline",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

// ─── Theme ───────────────────────────────────────────────────────
@Composable
private fun AutoContactsSaverTheme(content: @Composable () -> Unit) {
    val colorScheme = androidx.compose.material3.lightColorScheme(
        primary = PrimaryGreen,
        onPrimary = Color.White,
        secondary = LightGreen,
        background = BgGradientTop,
        surface = CardWhite,
        onSurface = TextPrimary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPreview() {
    AutoContactsSaverTheme {
        MainScreenWithDrawer()
    }
}

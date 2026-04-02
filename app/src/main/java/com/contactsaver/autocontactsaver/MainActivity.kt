package com.contactsaver.autocontactsaver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contactsaver.autocontactsaver.info.InfoPageActivity
import com.contactsaver.autocontactsaver.info.InfoPageType
import com.contactsaver.autocontactsaver.ocr.AutoSaveOcrProcessor
import com.contactsaver.autocontactsaver.ocr.OcrResultActivity
import kotlinx.coroutines.launch

private const val DEMO_VIDEO_EMBED_URL = "https://www.youtube-nocookie.com/embed/ysz5S6PUM-U?autoplay=1&mute=1&rel=0&modestbranding=1&playsinline=1&controls=1"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoContactsSaverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}

private data class DashboardCard(
    val title: String,
    val icon: ImageVector,
    val action: DashboardAction
)

private data class DrawerMenuEntry(
    val title: String,
    val icon: ImageVector,
    val pageType: InfoPageType
)

private enum class DashboardAction {
    OCR_PICKER,
    AUTO_SAVE,
    MORE
}

private val dashboardCards = listOf(
    DashboardCard(
        title = "Image To Cont",
        icon = Icons.Default.ContactPhone,
        action = DashboardAction.OCR_PICKER
    ),
    DashboardCard(
        title = "Auto Save Contact",
        icon = Icons.Default.AutoAwesome,
        action = DashboardAction.AUTO_SAVE
    ),
    DashboardCard(
        title = "More",
        icon = Icons.Default.MoreHoriz,
        action = DashboardAction.MORE
    )
)

private val drawerItems = listOf(
    DrawerMenuEntry("Privacy Policy", Icons.Default.Policy, InfoPageType.PRIVACY_POLICY),
    DrawerMenuEntry("Terms & Conditions", Icons.Default.Description, InfoPageType.TERMS_CONDITIONS),
    DrawerMenuEntry("About Us", Icons.Default.Info, InfoPageType.ABOUT_US),
    DrawerMenuEntry("Contact Us", Icons.Default.Mail, InfoPageType.CONTACT_US),
    DrawerMenuEntry("More Options", Icons.Default.Menu, InfoPageType.MORE_OPTIONS)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen() {
    val context = LocalContext.current
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val ocrProcessor = remember { AutoSaveOcrProcessor(context.applicationContext) }
    var statusMessage by remember { mutableStateOf("Tap the first card to choose an image.") }
    var isProcessing by remember { mutableStateOf(false) }

    DisposableEffect(ocrProcessor) {
        onDispose { ocrProcessor.close() }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            statusMessage = "Image selection was canceled."
            return@rememberLauncherForActivityResult
        }

        isProcessing = true
        statusMessage = "Image selected. Contact Extract is running..."
        ocrProcessor.scanImage(uri) { result ->
            isProcessing = false
            statusMessage = result.message
            context.startActivity(OcrResultActivity.createIntent(context, result))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .navigationBarsPadding(),
                drawerContainerColor = Color(0xFF0C224A),
                drawerContentColor = Color.White
            ) {
                DrawerHeader()
                drawerItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        selected = index == 0,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(
                                InfoPageActivity.createIntent(context, item.pageType)
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF2452A3),
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color(0xFFD7E6FF),
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color(0xFFD7E6FF)
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Toggle menu"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Auto Contact Saver",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            DashboardContent(
                innerPadding = innerPadding,
                isProcessing = isProcessing,
                statusMessage = statusMessage,
                onCardClick = { action ->
                    when (action) {
                        DashboardAction.OCR_PICKER -> imagePickerLauncher.launch("image/*")
                        DashboardAction.AUTO_SAVE -> {
                            val folder = ocrProcessor.ensureOcrFolder()
                            statusMessage = "Auto-save folder is ready: ${folder.absolutePath}"
                        }

                        DashboardAction.MORE -> {
                            scope.launch { drawerState.open() }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DashboardContent(
    innerPadding: PaddingValues,
    isProcessing: Boolean,
    statusMessage: String,
    onCardClick: (DashboardAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06142D),
                        Color(0xFF0E2E63),
                        Color(0xFF1F62C9)
                    )
                )
            )
    ) {
        FloatingBackgroundGlow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 24.dp)
        )
        FloatingBackgroundGlow(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 80.dp),
            size = 180.dp,
            alpha = 0.22f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            DashboardHero()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Quick Access",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose an image from the first card to extract text and detect phone numbers.",
                color = Color(0xFFD7E6FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )
            DashboardCardGrid(onCardClick = onCardClick)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardCardGrid(onCardClick: (DashboardAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        dashboardCards.forEach { card ->
            DashboardCardItem(
                card = card,
                modifier = Modifier.width(122.dp),
                onClick = { onCardClick(card.action) }
            )
        }
    }
}

@Composable
private fun DashboardHero() {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF112E61).copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1C4EA1),
                            Color(0xFF2C7CF6)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            YoutubeDemoBanner()
        }
    }
}

@Composable
private fun YoutubeDemoBanner() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF081A38)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.loadsImagesAutomatically = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        """
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                        <style>
                        html, body {
                          margin: 0;
                          padding: 0;
                          width: 100%;
                          height: 100%;
                          overflow: hidden;
                          background: #081A38;
                        }
                        .frame-wrap {
                          position: relative;
                          width: 100%;
                          height: 100%;
                          overflow: hidden;
                          border-radius: 18px;
                        }
                        iframe {
                          position: absolute;
                          inset: 0;
                          width: 100%;
                          height: 100%;
                          border: 0;
                        }
                        </style>
                        </head>
                        <body>
                        <div class="frame-wrap">
                        <iframe
                            src="$DEMO_VIDEO_EMBED_URL"
                            title="Demo Video"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                            allowfullscreen>
                        </iframe>
                        </div>
                        </body>
                        </html>
                        """.trimIndent(),
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            update = { webView ->
                webView.settings.javaScriptEnabled = true
            }
        )
    }
}

@Composable
private fun DashboardCardItem(
    card: DashboardCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D234B).copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.heightIn(min = 120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF143770),
                            Color(0xFF0A1D40)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D79F4).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = card.title,
                    tint = Color(0xFFB9D8FF),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = card.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE
                )
            )
        }
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1D56AF),
                        Color(0xFF0C224A)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContactPhone,
                contentDescription = "App logo",
                tint = Color.White
            )
        }
        Text(
            text = "Menu",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Privacy Policy, Terms & Conditions, About Us, Contact Us, and more.",
            color = Color(0xFFD7E6FF),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
        )
    }
}

@Composable
private fun FloatingBackgroundGlow(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    alpha: Float = 0.28f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF88C7FF).copy(alpha = alpha),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun AutoContactsSaverTheme(content: @Composable () -> Unit) {
    val colorScheme = androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF63A9FF),
        onPrimary = Color(0xFF06224A),
        secondary = Color(0xFFB7D5FF),
        background = Color(0xFF06142D),
        surface = Color(0xFF0C224A),
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardPreview() {
    AutoContactsSaverTheme {
        DashboardScreen()
    }
}

package com.contactsaver.autocontactsaver.info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Policy
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val EXTRA_PAGE_TYPE = "extra_page_type"

enum class InfoPageType {
    PRIVACY_POLICY,
    TERMS_CONDITIONS,
    ABOUT_US,
    CONTACT_US,
    MORE_OPTIONS
}

private data class InfoPageContent(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val sections: List<Pair<String, String>>
)

class InfoPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pageType = intent.getStringExtra(EXTRA_PAGE_TYPE)
            ?.let { runCatching { InfoPageType.valueOf(it) }.getOrNull() }
            ?: InfoPageType.PRIVACY_POLICY

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF06142D)
                ) {
                    InfoPageScreen(
                        content = pageContentFor(pageType),
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context, pageType: InfoPageType): Intent {
            return Intent(context, InfoPageActivity::class.java).apply {
                putExtra(EXTRA_PAGE_TYPE, pageType.name)
            }
        }
    }
}

private fun pageContentFor(type: InfoPageType): InfoPageContent {
    return when (type) {
        InfoPageType.PRIVACY_POLICY -> InfoPageContent(
            title = "Privacy Policy",
            subtitle = "How Auto Contact Saver handles your data",
            icon = Icons.Default.Policy,
            sections = listOf(
                "What this app does" to "Auto Contact Saver helps you capture unknown contact numbers from images and save them for later use on your device.",
                "Data we process" to "The app processes selected images, detected text, and extracted phone numbers only to provide contact extraction features.",
                "Permissions" to "The app may request access to photos, media, or files when you choose an image for extraction. Internet access may also be used for embedded demo content shown inside the app.",
                "Storage" to "Selected images may be copied into the AutoSaveOCR folder on your device so you can review and manage extracted content later.",
                "Retention" to "Saved images and extracted results remain on your device until you remove them. You control how long you keep your files and extracted contact data.",
                "Children and under-age users" to "This app is not intended for children without supervision. If you are under the age required in your region to manage personal data independently, you should use the app only with permission and guidance from a parent or guardian.",
                "Children's data" to "We do not knowingly design this app to collect personal data directly from children. Users should avoid scanning or storing sensitive data belonging to minors unless they have a lawful reason to do so.",
                "Sensitive content" to "You should not use the app to process highly sensitive personal information unless you are permitted to handle that information and understand the privacy risks involved.",
                "Sharing" to "We do not automatically upload your contacts, images, or extracted numbers to external servers.",
                "Third-party services" to "Some app features may rely on third-party components provided by platform or library providers, such as on-device text recognition or embedded media playback technologies.",
                "Security" to "We take a practical local-first approach by keeping extracted content on your device. However, you remain responsible for securing your device, backups, and stored files.",
                "Your responsibility" to "Before saving or using extracted numbers, make sure you have the right to collect, store, or contact those people under applicable laws and platform rules.",
                "Your control" to "You decide which image to scan, when to remove saved files, and whether to keep or delete extracted numbers."
                ,
                "Policy updates" to "This Privacy Policy may be updated as the app evolves. Continued use of the app after updates means you accept the revised policy."
            )
        )
        InfoPageType.TERMS_CONDITIONS -> InfoPageContent(
            title = "Terms & Conditions",
            subtitle = "Basic rules for using Auto Contact Saver",
            icon = Icons.Default.Description,
            sections = listOf(
                "Intended use" to "This app is designed to help users save and organize unknown or unsaved contacts from screenshots, posters, and shared images.",
                "User responsibility" to "You are responsible for using extracted contact details in a lawful and respectful way.",
                "Accuracy" to "Text detection and number extraction depend on image quality. You should verify important results before using them.",
                "Device storage" to "Saved images and detected results are stored on your device and may consume local storage space.",
                "Changes" to "Features, layouts, and extraction logic may change over time as the app improves."
            )
        )
        InfoPageType.ABOUT_US -> InfoPageContent(
            title = "About Us",
            subtitle = "What Auto Contact Saver is built for",
            icon = Icons.Default.Info,
            sections = listOf(
                "App purpose" to "Auto Contact Saver is built for people who frequently receive unknown contact numbers and want a faster way to capture and review them.",
                "Main feature" to "The app scans images, detects text, extracts phone numbers, and opens a clean result screen for easy review.",
                "Designed for" to "This app is useful for lead collection, business follow-up, local contact management, and saving numbers from shared media.",
                "Simple workflow" to "Pick an image, run Contact Extract, review results, and decide what to save next.",
                "Focus" to "We focus on speed, clarity, and practical tools for working with unsaved contacts."
            )
        )
        InfoPageType.CONTACT_US -> InfoPageContent(
            title = "Contact Us",
            subtitle = "Get in touch about app feedback and support",
            icon = Icons.Default.Mail,
            sections = listOf(
                "Support" to "If you need help with extraction, layout issues, or saving contacts, you can reach out with your feedback and screenshots.",
                "Feature requests" to "You can share ideas for better contact saving, improved result handling, or new workflow options.",
                "Bug reports" to "If something fails, include the image used, what you expected, and what happened instead.",
                "Response scope" to "Support replies may focus on app guidance, feature clarification, and future improvement planning."
            )
        )
        InfoPageType.MORE_OPTIONS -> InfoPageContent(
            title = "More Options",
            subtitle = "Extra ways to use Auto Contact Saver",
            icon = Icons.Default.Menu,
            sections = listOf(
                "Unknown contact saving" to "Use the app to quickly detect and review unknown contacts from marketing creatives, shared chats, or screenshots.",
                "Image archive" to "The AutoSaveOCR folder helps keep a local copy of scanned images for later checking.",
                "Result review" to "Open the Contact Extract result screen to review detected numbers and copied text before acting on it.",
                "Future tools" to "More options can later include direct save actions, export tools, history, and smarter contact grouping."
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoPageScreen(
    content: InfoPageContent,
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text(
                            text = content.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    imageVector = content.icon,
                    contentDescription = content.title,
                    tint = Color.White
                )
                Text(
                    text = content.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = content.subtitle,
                    color = Color(0xFFD6E8FF),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                content.sections.forEach { (heading, body) ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = heading,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = body,
                            color = Color(0xFFD6E8FF),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

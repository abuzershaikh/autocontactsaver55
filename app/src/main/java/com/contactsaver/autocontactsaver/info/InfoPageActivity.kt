package com.contactsaver.autocontactsaver.info

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
    val sections: List<Pair<String, String>>,
    val showContactButtons: Boolean = false
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
                    color = Color(0xFFE8F5E9)
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
                "Introduction" to "Welcome to Auto Contact Saver. We are committed to protecting your personal data and respecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your information when you use our application.",
                "Information We Collect" to "Our application primarily functions offline and on-device. We collect and process images you explicitly choose to scan using the app to extract contact numbers. This process happens locally on your device.",
                "How We Use Your Data" to "The extracted data (phone numbers and text) is used solely to provide the core functionality of the app—allowing you to preview, select, and save contacts to your device's address book or as a CSV/VCF file.",
                "Data Sharing and Disclosure" to "We do not sell, trade, or otherwise transfer your personally identifiable information or extracted contacts to outside parties. All your data remains strictly on your local device.",
                "Data Security" to "We implement a variety of security measures to maintain the safety of your personal information. Since data is stored locally, the security of your data is closely tied to the security of your device.",
                "Children's Privacy" to "Our application is not directed at children under the age of 13, and we do not knowingly collect personal identifiable information from children under 13. If you are a parent or guardian and you are aware that your child has provided us with personal data without your consent, please contact us so we can take necessary actions.",
                "Changes to This Policy" to "We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page and updating the application accordingly.",
                "Contact Us" to "If you have any questions about this Privacy Policy, please contact us:\n\nPhone/WhatsApp: +91 9930238183\nEmail: ishyantechnologies@gmail.com"
            )
        )
        InfoPageType.TERMS_CONDITIONS -> InfoPageContent(
            title = "Terms & Conditions",
            subtitle = "Rules and guidelines for using our application",
            icon = Icons.Default.Description,
            sections = listOf(
                "Acceptance of Terms" to "By downloading and using Auto Contact Saver, you agree to be bound by these Terms and Conditions. If you disagree with any part of the terms, you may not access the service.",
                "Use License" to "Permission is granted to temporarily download one copy of the app for personal, non-commercial transitory viewing only. This is the grant of a license, not a transfer of title.",
                "User Responsibilities" to "You are responsible for ensuring that any numbers you extract and save are done so lawfully and without violating the privacy rights of others.",
                "Disclaimer" to "The materials within the application are provided on an 'as is' basis. We make no warranties, expressed or implied, and hereby disclaim and negate all other warranties including, without limitation, implied warranties or conditions of merchantability.",
                "Limitations" to "In no event shall Auto Contact Saver or its suppliers be liable for any damages arising out of the use or inability to use the application.",
                "Governing Law" to "These terms and conditions are governed by and construed in accordance with applicable laws, and you irrevocably submit to the exclusive jurisdiction of the courts in that location.",
                "Contact Us" to "If you have any questions about these Terms and Conditions, please contact us:\n\nPhone/WhatsApp: +91 9930238183\nEmail: ishyantechnologies@gmail.com"
            )
        )
        InfoPageType.ABOUT_US -> InfoPageContent(
            title = "About Us",
            subtitle = "Learn more about Auto Contact Saver",
            icon = Icons.Default.Info,
            showContactButtons = true,
            sections = listOf(
                "Our Mission" to "At Auto Contact Saver, our mission is to streamline the way you manage and save contacts. We believe that technology should simplify everyday tasks, saving you time and effort.",
                "What We Do" to "We have developed a powerful, on-device OCR (Optical Character Recognition) tool that can instantly extract phone numbers from images, business cards, and screenshots.",
                "Why Choose Us" to "Unlike cloud-based solutions, Auto Contact Saver processes your images locally, ensuring maximum privacy and speed. We designed our app to be intuitive, efficient, and secure.",
                "Our Vision" to "We envision a world where manual data entry is a thing of the past. We are continuously improving our algorithms to provide the most accurate contact extraction experience possible.",
                "Contact Information" to "Phone/WhatsApp: +91 9930238183\nEmail: ishyantechnologies@gmail.com\n\nFeel free to reach out to us for any queries or support."
            )
        )
        InfoPageType.CONTACT_US -> InfoPageContent(
            title = "Contact Us",
            subtitle = "Get in touch with our support team",
            icon = Icons.Default.Mail,
            showContactButtons = true,
            sections = listOf(
                "Customer Support" to "If you have any questions, concerns, or need assistance using Auto Contact Saver, our support team is here to help.",
                "Phone Support" to "You can reach out to us directly via phone or WhatsApp at +91 9930238183. Click the button below to message us instantly.",
                "Email Us" to "You can reach out to us via email at ishyantechnologies@gmail.com. We aim to respond to all inquiries within 24-48 hours.",
                "Feedback" to "We value your feedback! If you have feature requests or suggestions on how we can improve the app, please don't hesitate to contact us."
            )
        )
        InfoPageType.MORE_OPTIONS -> InfoPageContent(
            title = "More Options",
            subtitle = "Additional settings and features",
            icon = Icons.Default.Menu,
            sections = listOf(
                "Coming Soon" to "We are actively working on adding more features and options to this section. Stay tuned for future updates!"
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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFF1F8E9),
                        Color(0xFFF9FBF2)
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
                        titleContentColor = Color(0xFF1B1B1B),
                        navigationIconContentColor = Color(0xFF2E7D32)
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
                    tint = Color(0xFF43A047),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = content.title,
                    color = Color(0xFF1B1B1B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = content.subtitle,
                    color = Color(0xFF616161),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                content.sections.forEach { (heading, body) ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = heading,
                            color = Color(0xFF1B1B1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = body,
                            color = Color(0xFF616161),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (content.showContactButtons) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val url = "https://wa.me/919930238183?text=Hello%2C%20I%20need%20help%20with%20Auto%20Contact%20Saver"
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "WhatsApp: +91 9930238183",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:ishyantechnologies@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Auto Contact Saver - Support Request")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "ishyantechnologies@gmail.com",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

package com.contactsaver.autocontactsaver.web

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.PlayArrow
import com.google.firebase.firestore.FirebaseFirestore
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.tasks.await

data class TutorialVideo(
    val videoId: String,
    val title: String,
    val language: String,
    val position: Int
)

class TutorialActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF9FBF2) 
                ) {
                    TutorialScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, TutorialActivity::class.java)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onBack: () -> Unit) {
    var showLanguageDialog by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    var videos by remember { mutableStateOf<List<TutorialVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedLanguage) {
        if (selectedLanguage != null) {
            isLoading = true
            try {
                val db = FirebaseFirestore.getInstance()
                val docSnapshot = db.collection("app_settings").document("tutorials").get().await()
                
                val fetchedVideos = mutableListOf<TutorialVideo>()
                if (docSnapshot.exists()) {
                    val rawVideos = docSnapshot.get("videos")
                    
                    if (rawVideos is List<*>) {
                        val videosArray = rawVideos as? List<Map<String, Any>>
                        videosArray?.forEach { map ->
                            val rawVideoId = map["videoId"] as? String ?: ""
                            val rawUrl = map["url"] as? String ?: ""
                            val videoId = extractYouTubeVideoId(if (rawVideoId.isNotEmpty()) rawVideoId else rawUrl)
                            val title = map["title"] as? String ?: ""
                            val language = map["language"] as? String ?: "ENGLISH"
                            val position = (map["position"] as? Number)?.toInt() ?: 0
                            
                            if (videoId.isNotEmpty()) {
                                fetchedVideos.add(TutorialVideo(videoId, title, language, position))
                            }
                        }
                    } else if (rawVideos is Map<*, *>) {
                        val arrayValue = rawVideos["arrayValue"] as? Map<*, *>
                        val values = arrayValue?.get("values") as? List<*>
                        values?.forEach { item ->
                            val mapValue = (item as? Map<*, *>)?.get("mapValue") as? Map<*, *>
                            val fields = mapValue?.get("fields") as? Map<*, *>
                            
                            val videoIdMap = fields?.get("videoId") as? Map<*, *>
                            val rawVideoId = videoIdMap?.get("stringValue") as? String ?: ""
                            
                            val urlMap = fields?.get("url") as? Map<*, *>
                            val rawUrl = urlMap?.get("stringValue") as? String ?: ""
                            
                            val videoId = extractYouTubeVideoId(if (rawVideoId.isNotEmpty()) rawVideoId else rawUrl)
                            
                            val titleMap = fields?.get("title") as? Map<*, *>
                            val title = titleMap?.get("stringValue") as? String ?: ""
                            
                            val langMap = fields?.get("language") as? Map<*, *>
                            val language = langMap?.get("stringValue") as? String ?: "ENGLISH"
                            
                            val posMap = fields?.get("position") as? Map<*, *>
                            val positionStr = posMap?.get("integerValue") as? String
                            val position = positionStr?.toIntOrNull() ?: 0
                            
                            if (videoId.isNotEmpty()) {
                                fetchedVideos.add(TutorialVideo(videoId, title, language, position))
                            }
                        }
                    }
                }
                
                videos = fetchedVideos
                    .filter { it.language.equals(selectedLanguage, ignoreCase = true) }
                    .sortedBy { it.position }
                    
                if (videos.isEmpty()) {
                    errorMessage = "No tutorials found for \${selectedLanguage}."
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load tutorials: \${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    if (showLanguageDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {  }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Select Language",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Please select your preferred language for tutorials.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                selectedLanguage = "HINDI"
                                showLanguageDialog = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366))
                    ) {
                        Text(
                            "Hindi (हिंदी)", 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLanguage = "ENGLISH"
                                showLanguageDialog = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text(
                            "English", 
                            fontWeight = FontWeight.Bold, 
                            color = Color.DarkGray,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Video Tutorial", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!showLanguageDialog && selectedLanguage != null) {
                        TextButton(onClick = { showLanguageDialog = true }) {
                            Text(selectedLanguage!!, color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF9FBF2)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF25D366)
                )
            } else if (errorMessage != null && videos.isEmpty()) {
                Text(
                    text = errorMessage!!,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(videos) { video ->
                        TutorialVideoItem(video)
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialVideoItem(video: TutorialVideo) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val url = "https://www.youtube.com/watch?v=${video.videoId}"
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center
            ) {
                
                coil.compose.AsyncImage(
                    model = "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg",
                    contentDescription = "Video Thumbnail",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material.icons.Icons.Filled.PlayArrow?.let { playIcon ->
                        Icon(
                            imageVector = playIcon,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            if (video.title.isNotEmpty()) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(12.dp),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Tutorial Video",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

private fun extractYouTubeVideoId(urlOrId: String): String {
    if (urlOrId.isBlank()) return ""
    val trimmed = urlOrId.trim()
    if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
        return trimmed
    }
    val patterns = listOf(
        "(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|watch\\?v=|shorts/|live/|watch\\?.+&v=))([\\w-]{11})",
        "^([\\w-]{11})$"
    )
    for (pattern in patterns) {
        val matcher = java.util.regex.Pattern.compile(pattern).matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1) ?: ""
        }
    }
    return trimmed
}

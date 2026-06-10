package com.contactsaver.autocontactsaver.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.contactsaver.autocontactsaver.MainActivity
import com.contactsaver.autocontactsaver.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class GoogleAuthActivity : ComponentActivity() {

    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        credentialManager = CredentialManager.create(this)

        setContent {
            GoogleAuthScreen(
                onGoogleSignInClick = { handleGoogleSignIn() }
            )
        }
    }

    private fun handleGoogleSignIn() {
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        coroutineScope.launch {
            try {
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("830070525039-87uqej73fe2rusee2tjhppv8ptmdd77h.apps.googleusercontent.com")
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = this@GoogleAuthActivity
                )

                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Sign in to Firebase with the Google ID token
                val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Successfully signed in to Firebase
                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            
                            // Initialize user in Firestore
                            if (currentUser != null) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    com.contactsaver.autocontactsaver.plan.UserManager.initializeUser(
                                        userId = currentUser.uid,
                                        email = currentUser.email ?: ""
                                    )
                                }
                            }
                            
                            Toast.makeText(
                                this@GoogleAuthActivity,
                                "Welcome ${googleIdTokenCredential.displayName}!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navigate to MainActivity
                            val intent = Intent(this@GoogleAuthActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@GoogleAuthActivity,
                                "Firebase sign-in failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

            } catch (e: GetCredentialException) {
                Toast.makeText(
                    this@GoogleAuthActivity,
                    "Sign-in failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GoogleAuthActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

// ─── Theme Colors ────────────────────────────────────────────────
private val BgGradientTop = Color(0xFFE8F5E9)
private val BgGradientBottom = Color(0xFFF1F8E9)
private val PrimaryGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF2E7D32)
private val LightGreen = Color(0xFF81C784)
private val CardWhite = Color(0xFFFFFFFD)
private val TextPrimary = Color(0xFF1B1B1B)
private val TextSecondary = Color(0xFF616161)

@Composable
fun GoogleAuthScreen(onGoogleSignInClick: () -> Unit) {
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

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

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
        // Decorative elements
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-60).dp, y = (-80).dp)
                .alpha(0.12f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryGreen, Color.Transparent),
                        radius = 400f
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .alpha(0.1f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(LightGreen, Color.Transparent),
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // App Logo/Icon (using actual app icon)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(y = floatOffset.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryGreen, DarkGreen)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.autosave),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Title
            Text(
                text = "Auto Contact Saver",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Save contacts automatically from messaging apps",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Features List
            FeatureItem(text = "Extract contacts from messaging apps")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureItem(text = "Scan images for phone numbers")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureItem(text = "Save contacts with one tap")

            Spacer(modifier = Modifier.weight(0.5f))

            // Google Sign-In Button
            GoogleSignInButton(onClick = onGoogleSignInClick)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By signing in, you agree to our Terms & Privacy Policy",
                fontSize = 12.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "buttonScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Google Logo (you'll need to add google_logo.png to drawable)
            // For now using a placeholder
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Continue with Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

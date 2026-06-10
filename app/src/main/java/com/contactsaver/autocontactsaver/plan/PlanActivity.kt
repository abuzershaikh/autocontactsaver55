package com.contactsaver.autocontactsaver.plan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Premium Color Palette ───────────────────────────────────────
private val DeepNavy = Color(0xFF0A0E21)
private val DarkSurface = Color(0xFF111328)
private val CardDark = Color(0xFF1A1D35)
private val CardDarkSelected = Color(0xFF1E2340)

private val AccentGold = Color(0xFFFFD700)
private val AccentAmber = Color(0xFFFFA726)
private val AccentOrange = Color(0xFFFF8A65)

private val NeonCyan = Color(0xFF00E5FF)
private val NeonTeal = Color(0xFF1DE9B6)
private val NeonGreen = Color(0xFF69F0AE)

private val PremiumPurple = Color(0xFF7C4DFF)
private val PremiumViolet = Color(0xFFB388FF)
private val PremiumPink = Color(0xFFFF80AB)

private val TextWhite = Color(0xFFF5F5F5)
private val TextMuted = Color(0xFF8E91A8)
private val TextSubtle = Color(0xFF5C5F7E)

/**
 * Activity for displaying and purchasing subscription plans
 * Premium dark-themed UI with vibrant gradients and glassmorphism
 */
class PlanActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private var selectedPlan by mutableStateOf<PlanType?>(null)
    private var isPurchasing by mutableStateOf(false)

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PlanActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        // Initialize billing manager
        billingManager = BillingManager(
            context = this,
            onPurchaseCompleted = { planType ->
                isPurchasing = false
                savePurchasedPlan(planType)
                showToast("✅ ${planType.planName} activated successfully!")
                finish()
            },
            onPurchaseFailed = { error ->
                isPurchasing = false
                showToast("❌ $error")
            }
        )
        billingManager.initialize()

        setContent {
            MaterialTheme {
                PremiumPlanScreen()
            }
        }
    }

    @Composable
    fun PremiumPlanScreen() {
        val infiniteTransition = rememberInfiniteTransition(label = "bg")

        // Animated gradient shift for background
        val gradientOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 600f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "gradShift"
        )

        // Pulsing glow
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )

        // Rotating decorative element
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotate"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepNavy,
                            DarkSurface,
                            Color(0xFF0D1025)
                        )
                    )
                )
        ) {
            // ── Decorative Background Elements ──
            // Top-right glow orb
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = 150.dp, y = (-80).dp)
                    .alpha(glowAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PremiumPurple, Color.Transparent),
                            radius = 400f
                        ),
                        shape = CircleShape
                    )
            )

            // Bottom-left glow orb
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-80).dp, y = 60.dp)
                    .alpha(glowAlpha * 0.8f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonCyan, Color.Transparent),
                            radius = 350f
                        ),
                        shape = CircleShape
                    )
            )

            // Center accent glow
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .alpha(glowAlpha * 0.4f)
                    .rotate(rotation)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AccentGold.copy(alpha = 0.3f), Color.Transparent),
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
            ) {
                // ── Top Bar ──
                PremiumTopBar()

                Spacer(modifier = Modifier.height(8.dp))

                // ── Premium Header Section ──
                PremiumHeaderSection()

                Spacer(modifier = Modifier.height(24.dp))

                // ── Plan Cards ──
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PlanType.values().filter { !it.isTrial }.forEach { planType ->
                        PremiumPlanCard(
                            planType = planType,
                            isSelected = selectedPlan == planType,
                            onClick = { selectedPlan = planType }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Purchase Button ──
                PremiumPurchaseButton()

                Spacer(modifier = Modifier.height(12.dp))

                // ── Terms ──
                Text(
                    text = "By purchasing, you agree to our Terms & Conditions",
                    fontSize = 11.sp,
                    color = TextSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Features List ──
                PremiumFeaturesList()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    @Composable
    fun PremiumTopBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button with glass effect
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { finish() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Premium badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AccentGold.copy(alpha = 0.15f),
                                AccentAmber.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AccentGold.copy(alpha = 0.4f),
                                AccentAmber.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PREMIUM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    @Composable
    fun PremiumHeaderSection() {
        val infiniteTransition = rememberInfiniteTransition(label = "header")
        val crownScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "crownPulse"
        )
        val shimmer by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crown icon with glow
            Box(contentAlignment = Alignment.Center) {
                // Glow behind crown
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .alpha(0.3f * shimmer)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AccentGold, Color.Transparent),
                                radius = 120f
                            ),
                            shape = CircleShape
                        )
                )
                // Crown icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(crownScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentGold, AccentAmber, AccentOrange)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title with gradient-like effect
            Text(
                text = "Unlock Premium",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Save unlimited contacts automatically",
                fontSize = 15.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "✨ No ads • No limits • Full power ✨",
                fontSize = 13.sp,
                color = AccentGold.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    fun PremiumPlanCard(
        planType: PlanType,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val scale by animateFloatAsState(
            targetValue = when {
                isPressed -> 0.96f
                isSelected -> 1.02f
                else -> 1f
            },
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "cardScale"
        )

        val borderColor by animateColorAsState(
            targetValue = when {
                isSelected && planType == PlanType.LIFETIME -> AccentGold
                isSelected && planType == PlanType.YEARLY -> NeonCyan
                isSelected -> PremiumPurple
                else -> Color.White.copy(alpha = 0.08f)
            },
            animationSpec = tween(400),
            label = "borderColor"
        )

        val cardGradient = when {
            isSelected && planType == PlanType.LIFETIME -> Brush.linearGradient(
                colors = listOf(
                    AccentGold.copy(alpha = 0.12f),
                    AccentAmber.copy(alpha = 0.06f),
                    CardDarkSelected
                ),
                start = Offset(0f, 0f),
                end = Offset(600f, 200f)
            )
            isSelected && planType == PlanType.YEARLY -> Brush.linearGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.10f),
                    NeonTeal.copy(alpha = 0.05f),
                    CardDarkSelected
                ),
                start = Offset(0f, 0f),
                end = Offset(600f, 200f)
            )
            isSelected -> Brush.linearGradient(
                colors = listOf(
                    PremiumPurple.copy(alpha = 0.10f),
                    PremiumViolet.copy(alpha = 0.05f),
                    CardDarkSelected
                ),
                start = Offset(0f, 0f),
                end = Offset(600f, 200f)
            )
            else -> Brush.linearGradient(
                colors = listOf(CardDark, CardDark)
            )
        }

        // Badge info
        val badgeText = when (planType) {
            PlanType.YEARLY -> "BEST VALUE"
            PlanType.LIFETIME -> "⭐ MOST POPULAR"
            else -> null
        }
        val badgeGradient = when (planType) {
            PlanType.YEARLY -> Brush.horizontalGradient(listOf(NeonCyan, NeonTeal))
            PlanType.LIFETIME -> Brush.horizontalGradient(listOf(AccentGold, AccentAmber))
            else -> Brush.horizontalGradient(listOf(PremiumPurple, PremiumViolet))
        }

        val planIcon = when (planType) {
            PlanType.TRIAL -> Icons.Default.Info
            PlanType.MONTHLY -> Icons.Default.CalendarMonth
            PlanType.YEARLY -> Icons.Default.DateRange
            PlanType.LIFETIME -> Icons.Default.AllInclusive
        }

        val iconGradient = when (planType) {
            PlanType.TRIAL -> Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)))
            PlanType.MONTHLY -> Brush.linearGradient(listOf(PremiumPurple, PremiumViolet))
            PlanType.YEARLY -> Brush.linearGradient(listOf(NeonCyan, NeonTeal))
            PlanType.LIFETIME -> Brush.linearGradient(listOf(AccentGold, AccentAmber))
        }

        Box(modifier = Modifier.scale(scale)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = borderColor
                ),
                onClick = onClick
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plan icon
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(iconGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = planIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Plan info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = planType.planName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            if (badgeText != null) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeGradient)
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepNavy,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = when (planType) {
                                    PlanType.TRIAL -> "Free forever for first 100 contacts"
                                    PlanType.MONTHLY -> "Valid for 30 days"
                                    PlanType.YEARLY -> "Valid for 1 year • Save 58%"
                                    PlanType.LIFETIME -> "One-time payment • Forever yours"
                                },
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        // Price column
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = planType.price,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (planType) {
                                    PlanType.TRIAL -> Color(0xFF64B5F6)
                                    PlanType.LIFETIME -> AccentGold
                                    PlanType.YEARLY -> NeonCyan
                                    PlanType.MONTHLY -> PremiumViolet
                                }
                            )

                            if (planType == PlanType.YEARLY) {
                                Text(
                                    text = "₹2,388",
                                    fontSize = 11.sp,
                                    color = TextSubtle,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Selection indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(
                                                when (planType) {
                                                    PlanType.TRIAL -> Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)))
                                                    PlanType.LIFETIME -> Brush.linearGradient(listOf(AccentGold, AccentAmber))
                                                    PlanType.YEARLY -> Brush.linearGradient(listOf(NeonCyan, NeonTeal))
                                                    PlanType.MONTHLY -> Brush.linearGradient(listOf(PremiumPurple, PremiumViolet))
                                                }
                                            )
                                        } else {
                                            Modifier
                                                .background(Color.Transparent)
                                                .border(
                                                    width = 2.dp,
                                                    color = TextSubtle,
                                                    shape = CircleShape
                                                )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (planType == PlanType.LIFETIME || planType == PlanType.YEARLY) DeepNavy else Color.White,
                                        modifier = Modifier.size(16.dp)
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
    fun PremiumFeaturesList() {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = "What you get",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val features = listOf(
                Triple(Icons.Default.AllInclusive, "Unlimited Contacts", NeonGreen),
                Triple(Icons.Default.FlashOn, "Instant Auto-Save", NeonCyan),
                Triple(Icons.Default.Groups, "Group Extractor", PremiumViolet),
                Triple(Icons.Default.Block, "Ad-Free Experience", AccentGold),
                Triple(Icons.Default.Support, "Priority Support", PremiumPink)
            )

            features.forEach { (icon, text, color) ->
                PremiumFeatureItem(icon = icon, text = text, accentColor = color)
            }
        }
    }

    @Composable
    fun PremiumFeatureItem(
        icon: ImageVector,
        text: String,
        accentColor: Color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                fontSize = 14.sp,
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    @Composable
    fun PremiumPurchaseButton() {
        val infiniteTransition = rememberInfiniteTransition(label = "btn")
        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -200f,
            targetValue = 800f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "btnShimmer"
        )

        val buttonGradient = when (selectedPlan) {
            PlanType.TRIAL -> Brush.horizontalGradient(
                colors = listOf(Color(0xFF64B5F6), Color(0xFF42A5F5), Color(0xFF2196F3))
            )
            PlanType.LIFETIME -> Brush.horizontalGradient(
                colors = listOf(AccentGold, AccentAmber, AccentOrange)
            )
            PlanType.YEARLY -> Brush.horizontalGradient(
                colors = listOf(NeonCyan, NeonTeal, NeonGreen)
            )
            PlanType.MONTHLY -> Brush.horizontalGradient(
                colors = listOf(PremiumPurple, PremiumViolet, PremiumPink)
            )
            null -> Brush.horizontalGradient(
                colors = listOf(TextSubtle, TextMuted)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(buttonGradient)
                    .then(
                        if (selectedPlan != null && !isPurchasing) {
                            Modifier.clickable {
                                selectedPlan?.let { plan ->
                                    isPurchasing = true
                                    billingManager.launchPurchaseFlow(this@PlanActivity, plan)
                                }
                            }
                        } else if (selectedPlan == null) {
                            Modifier.clickable {
                                showToast("Please select a plan")
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPurchasing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Processing...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedPlan != null) Icons.Default.Lock else Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (selectedPlan == PlanType.LIFETIME || selectedPlan == PlanType.YEARLY) DeepNavy else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedPlan?.let { "Unlock ${it.planName} — ${it.price}" }
                                ?: "Select a Plan to Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedPlan == PlanType.LIFETIME || selectedPlan == PlanType.YEARLY) DeepNavy else Color.White,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // Secure payment note
            if (selectedPlan != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = NeonGreen.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Secured by Google Play",
                        fontSize = 11.sp,
                        color = TextSubtle,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    private fun savePurchasedPlan(planType: PlanType) {
        val prefs = getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
        val purchaseTime = System.currentTimeMillis()
        
        prefs.edit().apply {
            putString("active_plan", planType.name)
            putLong("purchase_time", purchaseTime)
            putBoolean("is_premium", true)
            apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.release()
    }
}

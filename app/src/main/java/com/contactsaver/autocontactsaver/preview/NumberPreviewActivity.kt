package com.contactsaver.autocontactsaver.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val EXTRA_NUMBERS = "extra_numbers"

class NumberPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val numbers = intent.getStringArrayListExtra(EXTRA_NUMBERS).orEmpty()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgGradientTop
                ) {
                    NumberPreviewScreen(
                        numbers = numbers,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Create an intent to launch NumberPreviewActivity.
         * Any extraction method (OCR, Chat, Group, Call) can use this.
         */
        fun createIntent(
            context: Context,
            numbers: List<String>
        ): Intent {
            return Intent(context, NumberPreviewActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_NUMBERS, ArrayList(numbers))
            }
        }
    }
}

// ─── Color Palette (matches MainActivity green theme) ────────────
private val BgGradientTop = Color(0xFFE8F5E9)
private val BgGradientMid = Color(0xFFF1F8E9)
private val BgGradientBottom = Color(0xFFF9FBF2)
private val PrimaryGreen = Color(0xFF43A047)
private val DarkGreen = Color(0xFF2E7D32)
private val LightGreen = Color(0xFF81C784)
private val SoftGreen = Color(0xFFE8F5E9)
private val CardWhite = Color(0xFFFFFFFD)
private val CardBg = Color(0xFFF1F8E9)
private val TextDark = Color(0xFF1B1B1B)
private val TextSecondary = Color(0xFF616161)
private val TextMuted = Color(0xFF9E9E9E)

// ─── Main Screen ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberPreviewScreen(
    numbers: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seriesName by remember { mutableStateOf("Contact") }
    var startFrom by remember { mutableIntStateOf(1) }
    val selectedStates = remember { mutableStateListOf<Boolean>().apply { addAll(List(numbers.size) { true }) } }
    var showTrialLimitDialog by remember { mutableStateOf(false) }
    var previewTrialReservationChecked by rememberSaveable { mutableStateOf(false) }
    var previewSaveAllowed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(numbers.size) {
        if (previewTrialReservationChecked) return@LaunchedEffect
        previewTrialReservationChecked = true

        val userId = com.contactsaver.autocontactsaver.plan.UserManager.getCurrentUserId()
        if (userId != null) {
            val isPremium = com.contactsaver.autocontactsaver.plan.UserManager.isPremiumUser(userId)
                .getOrDefault(false)

            if (isPremium) {
                previewSaveAllowed = true
            } else {
                val canReserve = com.contactsaver.autocontactsaver.plan.UserManager.canSaveContacts(
                    userId,
                    requestedCount = numbers.size
                ).getOrDefault(false)

                if (canReserve) {
                    val reserveResult = com.contactsaver.autocontactsaver.plan.UserManager.incrementContactsSaved(
                        userId = userId,
                        count = numbers.size
                    )

                    previewSaveAllowed = reserveResult.isSuccess
                    if (!reserveResult.isSuccess) {
                        showTrialLimitDialog = true
                    }
                } else {
                    previewSaveAllowed = false
                    showTrialLimitDialog = true
                }
            }
        } else {
            previewSaveAllowed = true
        }
    }
    
    // Preview screen already reserves trial quota for signed-in trial users.
    val checkTrialLimitAndSave: (Int, suspend () -> Unit) -> Unit = { _, saveAction ->
        scope.launch {
            if (previewSaveAllowed) {
                saveAction()
            } else {
                showTrialLimitDialog = true
            }
        }
    }
    
    // Trial Limit Dialog
    if (showTrialLimitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTrialLimitDialog = false },
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFFFA726),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Trial Limit Reached",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "You've reached the free trial limit.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "To continue saving contacts, upgrade to a premium plan:",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Monthly: ₹199 (30 days)", fontSize = 13.sp, color = TextSecondary)
                    Text("• Yearly: ₹999 (Best Value)", fontSize = 13.sp, color = TextSecondary)
                    Text("• Lifetime: ₹1999 (One-time)", fontSize = 13.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                    Button(
                    onClick = {
                        showTrialLimitDialog = false
                        // Open PlanActivity
                        val intent = android.content.Intent(
                            context,
                            com.contactsaver.autocontactsaver.plan.PlanActivity::class.java
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    )
                ) {
                    Text("View Plans", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showTrialLimitDialog = false }
                ) {
                    Text("Maybe Later", color = TextSecondary)
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
                    colors = listOf(BgGradientTop, BgGradientMid, BgGradientBottom)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextDark,
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
                                text = "Auto Contacts Saver",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "${numbers.size} numbers extracted",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    },

                )
            },
            bottomBar = {
                SaveFormatBar(
                    numbers = numbers,
                    seriesName = seriesName,
                    startFrom = startFrom,
                    selectedStates = selectedStates,
                    previewSaveAllowed = previewSaveAllowed,
                    onUpgradeRequired = { showTrialLimitDialog = true },
                    onSaveClick = checkTrialLimitAndSave
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── Series Name & Start From ──
                SeriesInputRow(
                    seriesName = seriesName,
                    onSeriesNameChange = { seriesName = it },
                    startFrom = startFrom,
                    onStartFromChange = { startFrom = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Number List ──
                NumberList(
                    numbers = numbers,
                    seriesName = seriesName,
                    startFrom = startFrom,
                    selectedStates = selectedStates,
                    onToggle = { index ->
                        selectedStates[index] = !selectedStates[index]
                    }
                )
            }
        }
    }
}

// ─── Series Name & Start From Input Row ──────────────────────────
@Composable
private fun SeriesInputRow(
    seriesName: String,
    onSeriesNameChange: (String) -> Unit,
    startFrom: Int,
    onStartFromChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Series Name
                Column(modifier = Modifier.weight(1.6f)) {
                    Text(
                        text = "Series Name",
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = seriesName,
                        onValueChange = onSeriesNameChange,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color(0xFFC8E6C9),
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            cursorColor = PrimaryGreen
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = TextDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Start From
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start From",
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = startFrom.toString(),
                        onValueChange = { value ->
                            val num = value.filter { it.isDigit() }.toIntOrNull()
                            if (num != null && num > 0) {
                                onStartFromChange(num)
                            } else if (value.isEmpty()) {
                                onStartFromChange(1)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color(0xFFC8E6C9),
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg,
                            cursorColor = PrimaryGreen
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = TextDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─── Number List ─────────────────────────────────────────────────
@Composable
private fun NumberList(
    numbers: List<String>,
    seriesName: String,
    startFrom: Int,
    selectedStates: List<Boolean>,
    onToggle: (Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(numbers) { index, number ->
            val contactName = "$seriesName ${startFrom + index}"

            NumberRow(
                number = number,
                contactName = contactName,
                isSelected = selectedStates.getOrElse(index) { true },
                onToggle = { onToggle(index) }
            )
        }

        // Bottom spacing for the save bar
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Single Number Row ───────────────────────────────────────────
@Composable
private fun NumberRow(
    number: String,
    contactName: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CardWhite else CardWhite.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "rowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryGreen.copy(alpha = 0.3f) else Color(0xFFE0E0E0),
        animationSpec = tween(200),
        label = "rowBorder"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phone number section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = number,
                    color = TextDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Series name section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = contactName,
                    color = TextDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) PrimaryGreen else Color(0xFFBBC5D5),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─── Bottom Save Bar ─────────────────────────────────────────────
@Composable
private fun SaveFormatBar(
    numbers: List<String>,
    seriesName: String,
    startFrom: Int,
    selectedStates: List<Boolean>,
    previewSaveAllowed: Boolean,
    onUpgradeRequired: () -> Unit,
    onSaveClick: (Int, suspend () -> Unit) -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Selected count
            val selectedCount = selectedStates.count { it }
            Text(
                text = "$selectedCount of ${numbers.size} contacts selected",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save as CSV
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Save File Format",
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SaveButton(
                        label = "CSV",
                        containerColor = if (previewSaveAllowed) DarkGreen else TextMuted,
                        onClick = {
                            if (previewSaveAllowed) {
                                saveAsCsv(
                                    context = context,
                                    numbers = numbers,
                                    seriesName = seriesName,
                                    startFrom = startFrom,
                                    selectedStates = selectedStates,
                                    allowSave = true
                                )
                            } else {
                                onUpgradeRequired()
                            }
                        }
                    )
                }

                // Save to Contacts (opens Gmail/Contacts app)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Save to Contacts",
                        color = TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = previewSaveAllowed,
                        onClick = {
                            onSaveClick(selectedCount) {
                                saveToContacts(
                                    context = context,
                                    numbers = numbers,
                                    seriesName = seriesName,
                                    startFrom = startFrom,
                                    selectedStates = selectedStates
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Import",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveButton(
    label: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier.height(40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

// ─── File Save Utilities ─────────────────────────────────────────
private fun getSelectedContacts(
    numbers: List<String>,
    seriesName: String,
    startFrom: Int,
    selectedStates: List<Boolean>
): List<Pair<String, String>> {
    return numbers.mapIndexedNotNull { index, number ->
        if (selectedStates.getOrElse(index) { true }) {
            val name = "$seriesName ${startFrom + index}"
            Pair(name, number)
        } else null
    }
}

private fun saveAsCsv(
    context: Context,
    numbers: List<String>,
    seriesName: String,
    startFrom: Int,
    selectedStates: List<Boolean>,
    allowSave: Boolean
) {
    if (!allowSave) {
        Toast.makeText(
            context,
            "Trial limit reached. Upgrade to download CSV.",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val contacts = getSelectedContacts(numbers, seriesName, startFrom, selectedStates)
    if (contacts.isEmpty()) {
        Toast.makeText(context, "No contacts selected", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "contacts_${seriesName}_$timestamp.csv")

        FileWriter(file).use { writer ->
            writer.write("Name,Phone\n")
            contacts.forEach { (name, phone) ->
                writer.write("\"$name\",\"$phone\"\n")
            }
        }

        Toast.makeText(context, "CSV saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun saveAsVcf(
    context: Context,
    numbers: List<String>,
    seriesName: String,
    startFrom: Int,
    selectedStates: List<Boolean>
) {
    val contacts = getSelectedContacts(numbers, seriesName, startFrom, selectedStates)
    if (contacts.isEmpty()) {
        Toast.makeText(context, "No contacts selected", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "contacts_${seriesName}_$timestamp.vcf")

        FileWriter(file).use { writer ->
            contacts.forEach { (name, phone) ->
                writer.write("BEGIN:VCARD\n")
                writer.write("VERSION:3.0\n")
                writer.write("FN:$name\n")
                writer.write("TEL;TYPE=CELL:$phone\n")
                writer.write("END:VCARD\n")
            }
        }

        Toast.makeText(context, "VCF saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving VCF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun saveToContacts(
    context: Context,
    numbers: List<String>,
    seriesName: String,
    startFrom: Int,
    selectedStates: List<Boolean>
) {
    val contacts = getSelectedContacts(numbers, seriesName, startFrom, selectedStates)
    if (contacts.isEmpty()) {
        Toast.makeText(context, "No contacts selected", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        // Create VCF in cache directory
        val vcfDir = File(context.cacheDir, "vcf")
        if (!vcfDir.exists()) vcfDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val vcfFile = File(vcfDir, "import_${seriesName}_$timestamp.vcf")

        FileWriter(vcfFile).use { writer ->
            contacts.forEach { (name, phone) ->
                writer.write("BEGIN:VCARD\n")
                writer.write("VERSION:3.0\n")
                writer.write("FN:$name\n")
                writer.write("TEL;TYPE=CELL:$phone\n")
                writer.write("END:VCARD\n")
            }
        }

        // Get content URI via FileProvider
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            vcfFile
        )

        // Launch contacts app to import VCF
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "text/x-vcard")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        
        Toast.makeText(
            context, 
            "Opening contacts app to import ${contacts.size} contact(s)", 
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

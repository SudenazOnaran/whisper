package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedNote
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.ProcessingStep
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════
// SPLASH
// ══════════════════════════════════════════════════════════════════
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBg, Color(0xFF1B1525)))),
        contentAlignment = Alignment.Center
    ) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val scale by transition.animateFloat(
            0.9f, 1.1f,
            infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "s"
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .background(SlatePrimary, CircleShape)
                    .border(2.dp, SlateAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic, "logo",
                    tint = SlateAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Whisper Notes",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold, color = AccentGold
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Konuşmalarınız, anında notlara dönüşür",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.LightGray.copy(0.8f), fontFamily = FontFamily.SansSerif
                ),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator(color = SlateAccent, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// HOME
// ══════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val error by viewModel.error.collectAsState()
    val savedPath by viewModel.savedAudioPath.collectAsState()
    val savedNotes by viewModel.savedNotes.collectAsState()

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Bağlantı Hatası", color = CoralRed) },
            text = { Text(error!!, color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearError()
                    viewModel.navigateTo(AppScreen.SETTINGS)
                }) { Text("Ayarlar", color = SlateAccent) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Kapat", color = Color.Gray)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("WHISPER NOTES", color = SlateSecondary, fontSize = 11.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                        Text("Ses → Metin → Not", fontWeight = FontWeight.Bold,
                            color = Color.White, fontSize = 20.sp)
                    }
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.Default.Settings, "ayarlar", tint = SlateSecondary)
                    }
                }
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık kartı
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).background(SlatePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, null, tint = SlateAccent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Faster-Whisper Motorlu", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ses ve video dosyalarını\nanında metne dönüştürün", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Ne yapmak istersiniz?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            // Dosya yükle
            ActionCard(
                icon = Icons.Default.Upload,
                title = "Dosya Yükle",
                subtitle = "MP3, WAV, MP4, M4A, FLAC seç ve transkript al",
                accent = SlateSecondary,
                onClick = { viewModel.navigateTo(AppScreen.UPLOAD) }
            )

            // Canlı kayıt
            ActionCard(
                icon = Icons.Default.Mic,
                title = "Canlı Kayıt",
                subtitle = "Mikrofonu aç, konuş, gerçek zamanlı metin al",
                accent = CoralRed,
                onClick = { viewModel.navigateTo(AppScreen.LIVE_RECORD) }
            )

            // Kalıcı notlar
            ActionCard(
                icon = Icons.Default.Article,
                title = "Notlarım",
                subtitle = if (savedNotes.isEmpty()) "Kalıcı notların burada listelenecek" else "${savedNotes.size} kayıtlı not",
                accent = AccentGold,
                onClick = {
                    viewModel.refreshSavedNotes()
                    viewModel.navigateTo(AppScreen.NOTES)
                }
            )

            // Son sonuç
            val lastResult by viewModel.result.collectAsState()
            if (lastResult != null) {
                Spacer(Modifier.height(4.dp))
                Text("Son Sonuç", color = Color.Gray, fontSize = 13.sp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AppScreen.RESULT) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateAccent.copy(0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Article, null, tint = SlateAccent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            val title by viewModel.resultTitle.collectAsState()
                            Text(title.ifBlank { "Son transkript" }, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${lastResult!!.language.uppercase()} • ${lastResult!!.segments.size} segment", color = Color.Gray, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }

            if (savedPath != null) {
                Text("Kayıt: ${savedPath!!.substringAfterLast('/')}", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(0.35f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accent.copy(0.18f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ArrowForwardIos, null, tint = accent.copy(0.7f), modifier = Modifier.size(16.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// UPLOAD
// ══════════════════════════════════════════════════════════════════
@Composable
fun UploadScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    val language by viewModel.selectedLanguage.collectAsState()
    val modelSize by viewModel.selectedModel.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIdx >= 0) {
                    pickedName = it.getString(nameIdx)
                    if (title.isBlank()) title = pickedName.substringBeforeLast('.')
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopBar(title = "Dosya Yükle") { viewModel.navigateBack() }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Ses veya Video Seç", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Desteklenen: MP3, WAV, M4A, FLAC, OGG, MP4, MKV, AVI",
                    color = Color.Gray, fontSize = 12.sp)
            }

            item {
                // Dosya seçici alan
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filePicker.launch("*/*") },
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (pickedUri != null) SlateAccent else Color.DarkGray
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (pickedUri != null) Icons.Default.AudioFile else Icons.Default.FolderOpen,
                            null,
                            tint = if (pickedUri != null) SlateAccent else Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (pickedUri != null) pickedName else "Dosya seçmek için dokun",
                            color = if (pickedUri != null) Color.White else Color.Gray,
                            textAlign = TextAlign.Center,
                            fontWeight = if (pickedUri != null) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (pickedUri != null) {
                            Spacer(Modifier.height(4.dp))
                            Text("Değiştirmek için tekrar dokun", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (pickedUri != null) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Not başlığı") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SlateAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Model Seçimi", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModelButton("Small (Hızlı)", modelSize == "small", Modifier.weight(1f)) {
                                    viewModel.selectedModel.value = "small"
                                }
                                ModelButton("Medium (Hassas)", modelSize == "medium", Modifier.weight(1f)) {
                                    viewModel.selectedModel.value = "medium"
                                }
                            }

                            Text("Dil", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("auto" to "Otomatik", "tr" to "Türkçe", "en" to "İngilizce").forEach { (code, lbl) ->
                                    ModelButton(lbl, language == code, Modifier.weight(1f)) {
                                        viewModel.selectedLanguage.value = code
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            viewModel.uploadFile(pickedUri!!, title.ifBlank { pickedName }, context)
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateAccent, contentColor = SlatePrimary),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Transkript Al", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ModelButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) SlateSecondary else Color(0xFF2B2B2B),
            contentColor = if (selected) SlatePrimary else Color.White
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ══════════════════════════════════════════════════════════════════
// LIVE RECORD
// ══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveRecordScreen(viewModel: AppViewModel) {
    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val isRecording by viewModel.isRecording.collectAsState()
    val seconds by viewModel.recordingSeconds.collectAsState()
    val liveText by viewModel.liveText.collectAsState()
    val amplitudes by viewModel.waveAmplitudes.collectAsState()
    val wsConnected by viewModel.wsConnected.collectAsState()
    val language by viewModel.selectedLanguage.collectAsState()
    val modelSize by viewModel.selectedModel.collectAsState()
    var title by remember { mutableStateOf("") }

    val formattedTime = "%02d:%02d".format(seconds / 60, seconds % 60)

    Scaffold(
        containerColor = DarkBg,
        topBar = { TopBar("Canlı Kayıt") { viewModel.navigateBack() } }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!micPermission.status.isGranted) {
                PermissionCard(
                    message = if (micPermission.status.shouldShowRationale)
                        "Canlı kayıt için mikrofon izni gerekli."
                    else
                        "Mikrofon izni verilmedi. Ayarlardan açabilirsiniz.",
                    onRequest = { micPermission.launchPermissionRequest() }
                )
            } else {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Kayıt adı (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRecording,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = SlateAccent, unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )

                // Kayıt durumu göstergesi
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(DarkSurface, CircleShape)
                        .border(3.dp, if (isRecording) CoralRed else Color.DarkGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isRecording) {
                            val pulse = rememberInfiniteTransition(label = "p")
                            val a by pulse.animateFloat(0.4f, 1f,
                                infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
                            Icon(Icons.Default.FiberManualRecord, null, tint = CoralRed.copy(a), modifier = Modifier.size(20.dp))
                        }
                        Text(if (isRecording) "KAYIT" else "HAZIR",
                            color = if (isRecording) CoralRed else Color.Gray,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(formattedTime, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    }
                }

                // Dalga görselleştirme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(DarkSurface.copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (amplitudes.isEmpty()) {
                        Text("Ses dalgası burada gösterilecek...", color = Color.DarkGray, fontSize = 11.sp)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            amplitudes.forEach { amp ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((amp * 44f).dp.coerceAtLeast(2.dp))
                                        .background(
                                            if (isRecording) CoralRed.copy(0.85f) else SlateSecondary,
                                            RoundedCornerShape(1.5.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // Canlı metin
                if (liveText.isNotBlank() || isRecording) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 80.dp, max = 200.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (wsConnected) EmeraldGreen else Color.Gray,
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (wsConnected) "Canlı transkript aktif" else "Sunucuya bağlanıyor...",
                                    color = Color.Gray, fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LazyColumn {
                                item {
                                    Text(
                                        liveText.ifBlank { "Konuşmaya başlayın..." },
                                        color = if (liveText.isBlank()) Color.DarkGray else Color.LightGray,
                                        fontSize = 14.sp, lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dil: ${language.uppercase()}", color = Color.LightGray, fontSize = 12.sp)
                    Text("Model: ${modelSize.uppercase()}", color = Color.LightGray, fontSize = 12.sp)
                }

                if (!isRecording) {
                    Button(
                        onClick = { viewModel.startLiveRecording(title) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateAccent, contentColor = SlatePrimary),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kaydı Başlat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.stopLiveRecording() },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Durdur ve Not Al", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(message: String, onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MicOff, null, tint = CoralRed, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Mikrofon İzni Gerekli", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = Color.Gray, textAlign = TextAlign.Center, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = SlateAccent, contentColor = SlatePrimary)
            ) {
                Text("İzin Ver")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// PROCESSING
// ══════════════════════════════════════════════════════════════════
@Composable
fun ProcessingScreen(viewModel: AppViewModel) {
    val title by viewModel.resultTitle.collectAsState()
    val steps by viewModel.processingSteps.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(100.dp),
                    color = SlateAccent, strokeWidth = 6.dp
                )
                Icon(Icons.Default.Mic, null, tint = SlateAccent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "İşleniyor: ${title.ifBlank { "Ses dosyası" }}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White),
                textAlign = TextAlign.Center
            )
            Text("Faster-Whisper transkript oluşturuyor...",
                color = Color.Gray, fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                steps.forEach { step -> StepRow(step) }
            }
        }
    }
}

@Composable
fun StepRow(step: ProcessingStep) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    when { step.done -> EmeraldGreen; step.active -> SlateAccent; else -> Color.DarkGray },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                step.done -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                step.active -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SlatePrimary)
                else -> Box(modifier = Modifier.size(6.dp).background(Color.Gray, CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            step.label,
            color = when { step.done -> Color.LightGray; step.active -> Color.White; else -> Color.Gray },
            fontWeight = if (step.active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// RESULT (NOTLAR)
// ══════════════════════════════════════════════════════════════════
@Composable
fun ResultScreen(viewModel: AppViewModel) {
    val result by viewModel.result.collectAsState()
    val title by viewModel.resultTitle.collectAsState()
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf(0) } // 0=Tam metin, 1=Segmentler

    if (result == null) {
        Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
            Text("Sonuç yok.", color = Color.Gray)
        }
        return
    }

    val transcriptResult = result!!

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateHome() }) {
                            Icon(Icons.Default.Home, null, tint = SlateSecondary)
                        }
                        Text(
                            title.ifBlank { "Transkript" },
                            fontWeight = FontWeight.Bold, color = Color.White,
                            fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 180.dp)
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("transkript", transcriptResult.fullText))
                            Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, "kopyala", tint = SlateSecondary)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, transcriptResult.fullText)
                                putExtra(Intent.EXTRA_SUBJECT, title)
                            }
                            context.startActivity(Intent.createChooser(intent, "Paylaş"))
                        }) {
                            Icon(Icons.Default.Share, "paylaş", tint = SlateSecondary)
                        }
                    }
                }
                // Sekme çubuğu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                ) {
                    listOf("Tam Metin", "Zaman Çizelgesi").forEachIndexed { i, lbl ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewMode = i }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                lbl,
                                color = if (viewMode == i) SlateAccent else Color.Gray,
                                fontWeight = if (viewMode == i) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            if (viewMode == i) {
                                Box(Modifier.width(32.dp).height(2.dp).background(SlateAccent, RoundedCornerShape(1.dp)))
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
        ) {
            // Meta bilgi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(Icons.Default.Language, transcriptResult.language.uppercase())
                InfoChip(Icons.Default.ViewList, "${transcriptResult.segments.size} segment")
                InfoChip(
                    Icons.Default.TextFields,
                    "${transcriptResult.fullText.split("\\s+".toRegex()).size} kelime"
                )
            }

            when (viewMode) {
                0 -> FullTextView(transcriptResult.fullText)
                1 -> SegmentsView(transcriptResult.segments)
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SlateSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FullTextView(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text,
                    fontSize = 15.sp, lineHeight = 26.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun SegmentsView(segments: List<com.example.network.SegmentData>) {
    if (segments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Segment bilgisi yok.", color = Color.Gray)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(segments) { _, seg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlatePrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(seg.time, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateAccent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(seg.text, fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// NOTES
// ══════════════════════════════════════════════════════════════════
@Composable
fun NotesScreen(viewModel: AppViewModel) {
    val notes by viewModel.savedNotes.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBg,
        topBar = { TopBar("Notlarım") { viewModel.navigateBack() } }
    ) { pad ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Article, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Henüz kayıtlı not yok", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Dosya yüklediğinde veya canlı kaydı not aldığında burada kalıcı olarak görünür.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "${notes.size} kayıtlı not",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onOpen = { viewModel.openSavedNote(note) },
                    onCopy = { copyNote(context, note) },
                    onShare = { shareNote(context, note) },
                    onDelete = { viewModel.deleteSavedNote(note.id) }
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: SavedNote,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateAccent.copy(0.25f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        note.title.ifBlank { "İsimsiz Not" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${formatNoteDate(note.createdAt)} • ${note.sourceLabel()} • ${note.language.uppercase()}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                note.fullText,
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = SlateSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Kopyala", color = SlateSecondary, fontSize = 12.sp)
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = SlateSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Dışa aktar", color = SlateSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = CoralRed)
                    Spacer(Modifier.width(4.dp))
                    Text("Sil", color = CoralRed, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun copyNote(context: Context, note: SavedNote) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(note.title, note.exportText()))
    Toast.makeText(context, "Not panoya kopyalandı", Toast.LENGTH_SHORT).show()
}

private fun shareNote(context: Context, note: SavedNote) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, note.exportText())
        putExtra(Intent.EXTRA_SUBJECT, note.title)
    }
    context.startActivity(Intent.createChooser(intent, "Notu dışa aktar"))
}

private fun SavedNote.exportText(): String = buildString {
    appendLine(title.ifBlank { "İsimsiz Not" })
    appendLine(formatNoteDate(createdAt))
    appendLine()
    append(fullText)
}

private fun SavedNote.sourceLabel(): String = when (sourceType) {
    "live" -> "Canlı kayıt"
    "upload" -> "Dosya"
    else -> "Not"
}

private fun formatNoteDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(timestamp))
}

// ══════════════════════════════════════════════════════════════════
// SETTINGS
// ══════════════════════════════════════════════════════════════════
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val modelSize by viewModel.selectedModel.collectAsState()
    val language by viewModel.selectedLanguage.collectAsState()
    var urlInput by remember { mutableStateOf(serverUrl) }
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBg,
        topBar = { TopBar("Ayarlar") { viewModel.navigateBack() } }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Sunucu Bağlantısı", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Sunucu URL") },
                            placeholder = { Text("http://192.168.x.x:8000") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = SlateAccent, unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateServerUrl(urlInput)
                                    Toast.makeText(context, "URL güncellendi", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateAccent, contentColor = SlatePrimary),
                                modifier = Modifier.weight(1f)
                            ) { Text("Kaydet") }
                            OutlinedButton(
                                onClick = { urlInput = "http://127.0.0.1:8000"; viewModel.updateServerUrl("http://127.0.0.1:8000") },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                                modifier = Modifier.weight(1f)
                            ) { Text("Telefon", color = Color.Gray) }
                        }
                        Text(
                            "Kabloyla telefon: http://127.0.0.1:8000\nEmülatör: http://10.0.2.2:8000",
                            color = Color.Gray, fontSize = 11.sp
                        )
                    }
                }
            }

            item {
                Text("Varsayılan Whisper Modeli", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModelButton("Small", modelSize == "small", Modifier.weight(1f)) { viewModel.selectedModel.value = "small" }
                            ModelButton("Medium", modelSize == "medium", Modifier.weight(1f)) { viewModel.selectedModel.value = "medium" }
                        }
                    }
                }
            }

            item {
                Text("Varsayılan Dil", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("auto" to "Oto", "tr" to "Türkçe", "en" to "İngilizce").forEach { (code, lbl) ->
                                ModelButton(lbl, language == code, Modifier.weight(1f)) { viewModel.selectedLanguage.value = code }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Nasıl Başlatılır?", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "1. PC'de: cd faster-whisper/backend\n" +
                            "2. pip install -r requirements.txt\n" +
                            "3. python server.py\n" +
                            "4. Kabloyla test için: adb reverse tcp:8000 tcp:8000\n" +
                            "5. Telefon URL: http://127.0.0.1:8000",
                            color = Color.Gray, fontSize = 12.sp, lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// YARDIMCI BILEŞENLER
// ══════════════════════════════════════════════════════════════════
@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "geri", tint = SlateSecondary)
        }
        Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
    }
}

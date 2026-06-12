package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteStore
import com.example.data.SavedNote
import com.example.network.LiveTranscriptListener
import com.example.network.TranscriptResult
import com.example.network.WhisperClient
import com.example.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.WebSocket
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class AppScreen {
    SPLASH, HOME, UPLOAD, LIVE_RECORD, PROCESSING, RESULT, SETTINGS, NOTES
}

data class ProcessingStep(val label: String, val done: Boolean, val active: Boolean)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AppViewModel"
    private val noteStore = NoteStore(application)

    // ── Navigasyon ───────────────────────────────────────────────────────────
    private val _screen = MutableStateFlow(AppScreen.SPLASH)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val backStack = mutableListOf<AppScreen>()

    // ── Ayarlar (kalıcı değil – uygulama kapandığında sıfırlanır) ────────────
    private val _serverUrl = MutableStateFlow(BuildConfig.DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    val selectedLanguage = MutableStateFlow("auto")  // "auto" | "tr" | "en"
    val selectedModel    = MutableStateFlow("small") // "small" | "medium"

    // ── İşlem durumu ─────────────────────────────────────────────────────────
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingSteps = MutableStateFlow(emptyList<ProcessingStep>())
    val processingSteps: StateFlow<List<ProcessingStep>> = _processingSteps.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Sonuç (veritabanı yok – yalnızca bellekte) ───────────────────────────
    private val _result = MutableStateFlow<TranscriptResult?>(null)
    val result: StateFlow<TranscriptResult?> = _result.asStateFlow()

    private val _resultTitle = MutableStateFlow("")
    val resultTitle: StateFlow<String> = _resultTitle.asStateFlow()

    private val _savedNotes = MutableStateFlow(noteStore.loadNotes())
    val savedNotes: StateFlow<List<SavedNote>> = _savedNotes.asStateFlow()

    // ── Canlı kayıt ──────────────────────────────────────────────────────────
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _liveText = MutableStateFlow("")
    val liveText: StateFlow<String> = _liveText.asStateFlow()

    private val _waveAmplitudes = MutableStateFlow(emptyList<Float>())
    val waveAmplitudes: StateFlow<List<Float>> = _waveAmplitudes.asStateFlow()

    private val _savedAudioPath = MutableStateFlow<String?>(null)
    val savedAudioPath: StateFlow<String?> = _savedAudioPath.asStateFlow()

    private val _wsConnected = MutableStateFlow(false)
    val wsConnected: StateFlow<Boolean> = _wsConnected.asStateFlow()

    // ── Özel değişkenler ─────────────────────────────────────────────────────
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var liveWebSocket: WebSocket? = null
    private val pcmAccumulator = mutableListOf<Byte>()

    init {
        viewModelScope.launch {
            delay(2200)
            navigateTo(AppScreen.HOME)
        }
    }

    // ── Navigasyon ────────────────────────────────────────────────────────────
    fun navigateTo(screen: AppScreen) {
        if (_screen.value != screen) {
            backStack.add(_screen.value)
            _screen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            _screen.value = backStack.removeAt(backStack.size - 1)
            return true
        }
        return false
    }

    fun navigateHome() {
        backStack.clear()
        _screen.value = AppScreen.HOME
        _error.value = null
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url.trimEnd('/')
    }

    fun clearError() {
        _error.value = null
    }

    // ── Dosya yükleme ─────────────────────────────────────────────────────────
    fun uploadFile(uri: Uri, title: String, context: Context) {
        _resultTitle.value = title.ifBlank { "Ses Kaydı" }
        _error.value = null

        viewModelScope.launch {
            _isProcessing.value = true
            setSteps(0)
            navigateTo(AppScreen.PROCESSING)

            var tempFile: File? = null
            try {
                // 1. Dosyayı geçici alana kopyala
                setSteps(1)
                tempFile = copyUriToTemp(context, uri)
                Log.d(TAG, "Geçici dosya: ${tempFile.absolutePath} (${tempFile.length() / 1024} KB)")

                // 2. Sunucuya yükle ve transkript al
                setSteps(2)
                val client = WhisperClient(_serverUrl.value)
                val langArg = selectedLanguage.value.takeIf { it != "auto" }
                val transcriptResult = client.transcribeFile(tempFile, langArg, selectedModel.value)

                // 3. Tamamlandı
                setSteps(3)
                delay(300)

                _result.value = transcriptResult
                saveNote(transcriptResult, _resultTitle.value, "upload")
                _isProcessing.value = false
                navigateTo(AppScreen.RESULT)

            } catch (e: Exception) {
                Log.e(TAG, "Yükleme hatası", e)
                _error.value = "Hata: ${e.message ?: "Bilinmeyen hata"}\n\nSunucu adresini kontrol edin: ${_serverUrl.value}"
                _isProcessing.value = false
                navigateBack()
            } finally {
                tempFile?.delete()
            }
        }
    }

    private fun setSteps(activeIndex: Int) {
        val labels = listOf(
            "Dosya hazırlanıyor",
            "Sunucuya yükleniyor",
            "Whisper ile transkript alınıyor",
            "Notlar oluşturuldu"
        )
        _processingSteps.value = labels.mapIndexed { i, label ->
            ProcessingStep(label, done = i < activeIndex, active = i == activeIndex)
        }
    }

    private fun copyUriToTemp(context: Context, uri: Uri): File {
        val mimeType = context.contentResolver.getType(uri) ?: "audio/wav"
        val ext = when {
            mimeType.contains("mp4") || mimeType.contains("mpeg4") -> ".mp4"
            mimeType.contains("mkv") -> ".mkv"
            mimeType.contains("mp3") -> ".mp3"
            mimeType.contains("m4a") || mimeType.contains("mp4a") -> ".m4a"
            mimeType.contains("flac") -> ".flac"
            mimeType.contains("ogg") -> ".ogg"
            else -> ".wav"
        }
        val temp = File(context.cacheDir, "wn_upload_${System.currentTimeMillis()}$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output -> input.copyTo(output) }
        }
        return temp
    }

    // ── Canlı kayıt ──────────────────────────────────────────────────────────
    fun startLiveRecording(title: String) {
        if (_isRecording.value) return
        _resultTitle.value = title.ifBlank { "Canlı Kayıt ${System.currentTimeMillis() % 10000}" }
        _isRecording.value = true
        _recordingSeconds.value = 0
        _waveAmplitudes.value = emptyList()
        _liveText.value = ""
        _wsConnected.value = false
        _error.value = null
        pcmAccumulator.clear()

        val sampleRate  = 16000
        val chanConfig  = AudioFormat.CHANNEL_IN_MONO
        val audioFmt    = AudioFormat.ENCODING_PCM_16BIT
        val minBuf      = AudioRecord.getMinBufferSize(sampleRate, chanConfig, audioFmt)
        val bufferSize  = maxOf(minBuf, 8192)

        // Ses dosyasını kaydet (internal storage)
        val outDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: getApplication<Application>().filesDir
        val outFile = File(outDir, "wn_rec_${System.currentTimeMillis()}.wav")
        _savedAudioPath.value = outFile.absolutePath

        // WebSocket bağlan
        val wsListener = object : LiveTranscriptListener {
            override fun onConnected() {
                viewModelScope.launch { _wsConnected.value = true }
            }
            override fun onText(text: String, language: String, chunk: Int) {
                viewModelScope.launch {
                    val prev = _liveText.value
                    _liveText.value = if (prev.isBlank()) text else "$prev $text"
                }
            }
            override fun onError(message: String) {
                Log.w(TAG, "LiveWS hatası: $message")
                viewModelScope.launch { _wsConnected.value = false }
            }
            override fun onDisconnected() {
                viewModelScope.launch { _wsConnected.value = false }
            }
        }

        try {
            val client = WhisperClient(_serverUrl.value)
            liveWebSocket = client.connectLive(wsListener)
        } catch (e: Exception) {
            Log.w(TAG, "LiveWS bağlanamadı: ${e.message}")
        }

        // AudioRecord başlat
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, chanConfig, audioFmt, bufferSize
        )
        audioRecord?.startRecording()

        // Kayıt döngüsü
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            val shortBuf = ShortArray(bufferSize / 2)
            while (_isRecording.value) {
                val read = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: 0
                if (read <= 0) continue

                val byteBuf = ByteArray(read * 2)
                val bb = ByteBuffer.wrap(byteBuf).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until read) bb.putShort(shortBuf[i])

                // Dosyaya PCM biriktir (WAV header sonra eklenecek)
                byteBuf.forEach { pcmAccumulator.add(it) }

                // WebSocket'e gönder
                try {
                    val ws = liveWebSocket
                    if (ws != null) {
                        val client = WhisperClient(_serverUrl.value)
                        client.sendAudioChunk(ws, byteBuf)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "WS gönderme hatası: ${e.message}")
                }

                // Dalga görselleştirme
                val maxAmp = (0 until read).maxOfOrNull { shortBuf[it].toFloat().let { v -> if (v < 0) -v else v } } ?: 0f
                val normalized = (maxAmp / 32767f).coerceIn(0.04f, 1f)
                withContext(Dispatchers.Main) {
                    val current = _waveAmplitudes.value.takeLast(55).toMutableList()
                    current.add(normalized)
                    _waveAmplitudes.value = current
                }
            }

            // Kayıt bitti → WAV dosyasına yaz
            val pcmBytes = pcmAccumulator.toByteArray()
            writeWavFile(outFile, pcmBytes, sampleRate)
            Log.d(TAG, "WAV kaydedildi: ${outFile.absolutePath} (${outFile.length() / 1024} KB)")
        }

        // Zamanlayıcı
        timerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingSeconds.value += 1
            }
        }
    }

    fun stopLiveRecording() {
        _isRecording.value = false
        timerJob?.cancel()

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        liveWebSocket?.close(1000, "Kayıt durduruldu")
        liveWebSocket = null

        val liveTranscript = _liveText.value.trim()

        viewModelScope.launch {
            recordingJob?.join()

            if (liveTranscript.isNotBlank()) {
                // Canlı transkript varsa direkt sonuç ekranına git
                val transcriptResult = TranscriptResult(
                    fullText = liveTranscript,
                    segments = buildLiveSegments(liveTranscript),
                    language = if (selectedLanguage.value == "auto") "otomatik" else selectedLanguage.value
                )
                _result.value = transcriptResult
                saveNote(transcriptResult, _resultTitle.value, "live")
                deleteRecordedAudio()
                navigateTo(AppScreen.RESULT)
            } else {
                // Canlı bağlantı yoksa kaydedilen dosyayı sunucuya gönder
                val filePath = _savedAudioPath.value
                if (filePath != null) {
                    processRecordedFile(filePath)
                } else {
                    navigateHome()
                }
            }
        }
    }

    private fun buildLiveSegments(text: String) = text
        .split(Regex("(?<=[.!?])\\s+"))
        .filterNot { it.isBlank() }
        .mapIndexed { i, t ->
            val s = i * 5
            com.example.network.SegmentData(
                time = "%02d:%02d".format(s / 60, s % 60),
                text = t.trim()
            )
        }

    private fun processRecordedFile(filePath: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            setSteps(2)
            navigateTo(AppScreen.PROCESSING)

            try {
                val file = File(filePath)
                val client = WhisperClient(_serverUrl.value)
                val langArg = selectedLanguage.value.takeIf { it != "auto" }
                val transcriptResult = client.transcribeFile(file, langArg, selectedModel.value)

                setSteps(3)
                delay(300)
                _result.value = transcriptResult
                saveNote(transcriptResult, _resultTitle.value, "live")
                _isProcessing.value = false
                navigateTo(AppScreen.RESULT)
            } catch (e: Exception) {
                Log.e(TAG, "Dosya işleme hatası", e)
                _error.value = "Hata: ${e.message}"
                _isProcessing.value = false
                navigateHome()
            } finally {
                File(filePath).delete()
                _savedAudioPath.value = null
            }
        }
    }

    fun openSavedNote(note: SavedNote) {
        _resultTitle.value = note.title
        _result.value = note.toTranscriptResult()
        navigateTo(AppScreen.RESULT)
    }

    fun deleteSavedNote(id: String) {
        _savedNotes.value = noteStore.deleteNote(id)
    }

    fun refreshSavedNotes() {
        _savedNotes.value = noteStore.loadNotes()
    }

    private fun saveNote(result: TranscriptResult, title: String, sourceType: String) {
        val text = result.fullText.trim()
        if (text.isBlank()) return

        val note = SavedNote(
            id = "note_${System.currentTimeMillis()}",
            title = title.ifBlank { "İsimsiz Not" },
            fullText = text,
            segments = result.segments,
            language = result.language,
            languageProbability = result.languageProbability,
            sourceType = sourceType,
            createdAt = System.currentTimeMillis()
        )
        _savedNotes.value = noteStore.saveNote(note)
    }

    private fun deleteRecordedAudio() {
        _savedAudioPath.value?.let { File(it).delete() }
        _savedAudioPath.value = null
    }

    private fun writeWavFile(file: File, pcm: ByteArray, sampleRate: Int) {
        val channels      = 1
        val bitsPerSample = 16
        val byteRate      = sampleRate * channels * bitsPerSample / 8
        val blockAlign    = (channels * bitsPerSample / 8).toShort()
        val dataSize      = pcm.size
        val totalSize     = dataSize + 36

        file.outputStream().buffered().use { os ->
            fun i32(v: Int)   = byteArrayOf((v and 0xFF).toByte(), (v shr 8 and 0xFF).toByte(), (v shr 16 and 0xFF).toByte(), (v shr 24 and 0xFF).toByte())
            fun i16(v: Short) = byteArrayOf((v.toInt() and 0xFF).toByte(), (v.toInt() shr 8 and 0xFF).toByte())

            os.write("RIFF".toByteArray(Charsets.US_ASCII))
            os.write(i32(totalSize))
            os.write("WAVE".toByteArray(Charsets.US_ASCII))
            os.write("fmt ".toByteArray(Charsets.US_ASCII))
            os.write(i32(16))
            os.write(i16(1))
            os.write(i16(channels.toShort()))
            os.write(i32(sampleRate))
            os.write(i32(byteRate))
            os.write(i16(blockAlign))
            os.write(i16(bitsPerSample.toShort()))
            os.write("data".toByteArray(Charsets.US_ASCII))
            os.write(i32(dataSize))
            os.write(pcm)
        }
    }
}

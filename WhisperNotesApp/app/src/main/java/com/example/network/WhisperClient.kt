package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class TranscriptResult(
    val fullText: String,
    val segments: List<SegmentData>,
    val language: String,
    val languageProbability: Float = 0f
)

data class SegmentData(
    val time: String,
    val text: String,
    val start: Float = 0f,
    val end: Float = 0f
)

interface LiveTranscriptListener {
    fun onText(text: String, language: String, chunk: Int)
    fun onError(message: String)
    fun onConnected()
    fun onDisconnected()
}

class WhisperClient(private val baseUrl: String) {

    private val TAG = "WhisperClient"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()

    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeFile(
        file: File,
        language: String?,
        modelSize: String
    ): TranscriptResult = withContext(Dispatchers.IO) {
        val mimeType = when (file.extension.lowercase()) {
            "mp4", "mkv", "avi", "mov", "webm" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "audio/wav"
        }

        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mimeType.toMediaType())
            )
            .addFormDataPart("model_size", modelSize)
            .apply {
                if (!language.isNullOrBlank() && language != "auto") {
                    addFormDataPart("language", language)
                }
            }
            .build()

        val request = Request.Builder()
            .url("$baseUrl/transcribe")
            .post(formBody)
            .build()

        Log.d(TAG, "Uploading: ${file.name} (${file.length() / 1024} KB)")

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Boş yanıt")
            val json = JSONObject(body)

            if (!json.optBoolean("success", true)) {
                throw Exception(json.optString("error", "Bilinmeyen sunucu hatası"))
            }

            val fullText = json.optString("full_text", "")
            val lang = json.optString("language", "bilinmiyor")
            val langProb = json.optDouble("language_probability", 0.0).toFloat()
            val segArray = json.optJSONArray("segments")

            val segments = buildList {
                if (segArray != null) {
                    for (i in 0 until segArray.length()) {
                        val seg = segArray.getJSONObject(i)
                        add(
                            SegmentData(
                                time = seg.optString("time", "00:00"),
                                text = seg.optString("text", ""),
                                start = seg.optDouble("start", 0.0).toFloat(),
                                end = seg.optDouble("end", 0.0).toFloat()
                            )
                        )
                    }
                }
            }

            Log.d(TAG, "Transkript tamamlandı. Dil: $lang, ${segments.size} segment")
            TranscriptResult(fullText, segments, lang, langProb)
        }
    }

    fun connectLive(listener: LiveTranscriptListener): WebSocket {
        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")

        val request = Request.Builder()
            .url("$wsUrl/ws/live")
            .build()

        return wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "LiveWS açıldı")
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "transcript") {
                        listener.onText(
                            json.optString("text", ""),
                            json.optString("language", ""),
                            json.optInt("chunk", 0)
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LiveWS mesaj parse hatası: $e")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "LiveWS bağlantı hatası: ${t.message}")
                listener.onError(t.message ?: "Bağlantı hatası")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "LiveWS kapatıldı: $reason")
                listener.onDisconnected()
            }
        })
    }

    fun sendAudioChunk(webSocket: WebSocket, pcmData: ByteArray) {
        webSocket.send(pcmData.toByteString())
    }

    suspend fun checkServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/health").get().build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}

package com.example.service

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val onSpeechStarted: () -> Unit = {},
    private val onSpeechFinished: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentPitch = 1.05f
    private var currentRate = 1.0f

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Throwable) {
            Log.e("TtsManager", "TTS constructor error: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                // Priority: Hindi -> Indian English -> US
                var setSuccess = false
                val hiResult = tts?.setLanguage(Locale("hi", "IN"))
                if (hiResult != TextToSpeech.LANG_MISSING_DATA && hiResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                    setSuccess = true
                }

                if (!setSuccess) {
                    val inResult = tts?.setLanguage(Locale("en", "IN"))
                    if (inResult != TextToSpeech.LANG_MISSING_DATA && inResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                        setSuccess = true
                    }
                }

                if (!setSuccess) {
                    tts?.setLanguage(Locale.US)
                }

                tts?.setPitch(currentPitch)
                tts?.setSpeechRate(currentRate)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post { 
                            try { onSpeechStarted() } catch (_: Throwable) {}
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { 
                            try { onSpeechFinished() } catch (_: Throwable) {}
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        mainHandler.post { 
                            try { onSpeechFinished() } catch (_: Throwable) {}
                        }
                    }
                })

                isInitialized = true
            } catch (e: Throwable) {
                Log.e("TtsManager", "Error configuring TTS: ${e.message}")
            }
        } else {
            Log.e("TtsManager", "TTS initialization failed with status $status")
            try { onError("TTS engine initialization failed") } catch (_: Throwable) {}
        }
    }

    fun setSassTone(sassLevel: Int) {
        try {
            val pitch = 1.0f + (sassLevel / 100f) * 0.15f
            val rate = 1.0f + (sassLevel / 100f) * 0.08f
            currentPitch = pitch
            currentRate = rate
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
        } catch (_: Throwable) {}
    }

    fun speak(text: String, utteranceId: String = "puja_utterance_${System.currentTimeMillis()}") {
        val cleanText = cleanTextForSpeech(text)
        if (cleanText.isBlank()) {
            try { onSpeechFinished() } catch (_: Throwable) {}
            return
        }

        try {
            if (isInitialized && tts != null) {
                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } else {
                // Retry once after initialization
                mainHandler.postDelayed({
                    try {
                        if (isInitialized && tts != null) {
                            val params = Bundle()
                            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                        } else {
                            onSpeechFinished()
                        }
                    } catch (_: Throwable) {
                        onSpeechFinished()
                    }
                }, 600)
            }
        } catch (e: Throwable) {
            Log.e("TtsManager", "speak error: ${e.message}")
            try { onSpeechFinished() } catch (_: Throwable) {}
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Throwable) {}
    }

    companion object {
        fun cleanTextForSpeech(rawText: String): String {
            var text = rawText
            val jsonBlockRegex = """\{[\s\S]*\}""".toRegex()
            text = text.replace(jsonBlockRegex, "")
            text = text.replace("""```[\s\S]*?```""".toRegex(), "")
            text = text.replace("*", "")
            return text.trim()
        }
    }
}


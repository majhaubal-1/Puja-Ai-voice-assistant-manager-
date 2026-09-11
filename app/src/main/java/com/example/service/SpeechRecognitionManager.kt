package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

class SpeechRecognitionManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onSpeechPartialResult: (String) -> Unit,
    private val onSpeechFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isHandsFreeWakeWordMode = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val wakeWordKeywords = listOf(
        "hey puja", "puja", "hello puja", "ok puja", "hi puja",
        "hey pooja", "pooja", "hello pooja", "sun puja", "namaste puja", "arre puja",
        "हे पूजा", "पूजा", "नमस्ते पूजा", "सुनो पूजा"
    )

    init {
        initSpeechRecognizer()
    }

    private fun hasAudioPermission(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("SpeechManager", "Speech recognition service is not available on this device")
                return
            }
            mainHandler.post {
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(createListener())
                    }
                } catch (e: Throwable) {
                    Log.e("SpeechManager", "Failed to create SpeechRecognizer: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e("SpeechManager", "isRecognitionAvailable check failed: ${e.message}")
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechManager", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechManager", "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                try {
                    onRmsChanged(rmsdB)
                } catch (_: Throwable) {}
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SpeechManager", "End of speech")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Speech recognition error ($error)"
                }
                Log.d("SpeechManager", "onError: $errorMessage ($error)")

                isListening = false

                // If insufficient permission or fatal client error, stop auto-retrying
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS || error == SpeechRecognizer.ERROR_CLIENT || !hasAudioPermission()) {
                    isHandsFreeWakeWordMode = false
                    try { onError(errorMessage) } catch (_: Throwable) {}
                    return
                }

                // If hands-free mode is active, restart listening loop after a short delay
                if (isHandsFreeWakeWordMode) {
                    mainHandler.postDelayed({
                        if (isHandsFreeWakeWordMode && !isListening && hasAudioPermission()) {
                            startListeningInternal(forWakeWord = true)
                        }
                    }, 800)
                } else {
                    try { onError(errorMessage) } catch (_: Throwable) {}
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""

                if (recognizedText.isNotEmpty()) {
                    if (isHandsFreeWakeWordMode) {
                        val lower = recognizedText.lowercase()
                        val hasWakeWord = wakeWordKeywords.any { lower.contains(it) }

                        if (hasWakeWord) {
                            triggerWakeWordHaptic()
                            try { onWakeWordDetected() } catch (_: Throwable) {}

                            // Check if prompt was spoken in same sentence (e.g., "Hey Puja open WhatsApp")
                            var prompt = recognizedText
                            for (kw in wakeWordKeywords) {
                                val idx = prompt.lowercase().indexOf(kw)
                                if (idx >= 0) {
                                    prompt = prompt.substring(idx + kw.length).trim()
                                    break
                                }
                            }

                            if (prompt.isNotBlank() && prompt.length > 2) {
                                try { onSpeechFinalResult(prompt) } catch (_: Throwable) {}
                            } else {
                                startListeningForCommand()
                            }
                            return
                        } else {
                            // Loop back if in hands-free mode
                            mainHandler.postDelayed({
                                if (isHandsFreeWakeWordMode && !isListening && hasAudioPermission()) {
                                    startListeningInternal(forWakeWord = true)
                                }
                            }, 500)
                        }
                    } else {
                        try { onSpeechFinalResult(recognizedText) } catch (_: Throwable) {}
                    }
                } else if (isHandsFreeWakeWordMode) {
                    mainHandler.postDelayed({
                        if (isHandsFreeWakeWordMode && !isListening && hasAudioPermission()) {
                            startListeningInternal(forWakeWord = true)
                        }
                    }, 500)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotEmpty()) {
                    if (isHandsFreeWakeWordMode) {
                        val lower = partial.lowercase()
                        if (wakeWordKeywords.any { lower.contains(it) }) {
                            triggerWakeWordHaptic()
                            try { onWakeWordDetected() } catch (_: Throwable) {}
                        }
                    }
                    try { onSpeechPartialResult(partial) } catch (_: Throwable) {}
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun triggerWakeWordHaptic() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Throwable) {}
    }

    fun startListeningForCommand() {
        isHandsFreeWakeWordMode = false
        startListeningInternal(forWakeWord = false)
    }

    fun startWakeWordListening() {
        isHandsFreeWakeWordMode = true
        startListeningInternal(forWakeWord = true)
    }

    fun stopWakeWordListening() {
        isHandsFreeWakeWordMode = false
        stopListening()
    }

    private fun startListeningInternal(forWakeWord: Boolean) {
        if (!hasAudioPermission()) {
            Log.w("SpeechManager", "Cannot start speech listening without RECORD_AUDIO permission")
            isListening = false
            return
        }

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                            setRecognitionListener(createListener())
                        }
                    }
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    if (forWakeWord) {
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    }
                }
                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Throwable) {
                Log.e("SpeechManager", "Failed to start listening: ${e.message}")
                isListening = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.stopListening()
            } catch (_: Throwable) {}
        }
    }

    fun destroy() {
        isHandsFreeWakeWordMode = false
        isListening = false
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Throwable) {}
        }
    }
}


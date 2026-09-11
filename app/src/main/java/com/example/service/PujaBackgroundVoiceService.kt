package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.PujaDatabase
import com.example.data.model.PujaAction
import com.example.data.repository.InteractionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PujaBackgroundVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var speechManager: SpeechRecognitionManager? = null
    private var ttsManager: TtsManager? = null
    private var actionExecutor: DeviceActionExecutor? = null
    private var isListening = false

    companion object {
        const val CHANNEL_ID = "puja_background_voice_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_BACKGROUND_VOICE = "ACTION_START_BACKGROUND_VOICE"
        const val ACTION_STOP_BACKGROUND_VOICE = "ACTION_STOP_BACKGROUND_VOICE"

        fun startService(context: Context) {
            val hasRecordPerm = try {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                false
            }

            if (!hasRecordPerm) {
                Log.w("PujaBgService", "Skipping background service start: RECORD_AUDIO not granted yet")
                return
            }

            try {
                val intent = Intent(context, PujaBackgroundVoiceService::class.java).apply {
                    action = ACTION_START_BACKGROUND_VOICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e("PujaBgService", "Cannot start background service: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, PujaBackgroundVoiceService::class.java).apply {
                    action = ACTION_STOP_BACKGROUND_VOICE
                }
                context.startService(intent)
            } catch (e: Throwable) {
                Log.e("PujaBgService", "Cannot stop service: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            // Always satisfy Android foreground service requirement immediately
            val initialNotification = buildNotification("पूजा वॉयस एक्टिव है • 'नमस्ते पूजा' या 'Hey Puja' बोलें")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } catch (_: Throwable) {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Throwable) {
            Log.e("PujaBgService", "Error during onCreate foreground initialization: ${e.message}")
        }

        try {
            actionExecutor = DeviceActionExecutor(this)
            ttsManager = TtsManager(this)
            
            speechManager = SpeechRecognitionManager(
                context = this,
                onWakeWordDetected = {
                    ttsManager?.speak("जी, मैं सुन रही हूँ।")
                    updateNotification("सक्रिय: पूजा आपकी बात सुन रही है...")
                },
                onSpeechPartialResult = { partial ->
                    updateNotification("सुन रही हूँ: $partial")
                },
                onSpeechFinalResult = { query ->
                    handleBackgroundQuery(query)
                },
                onRmsChanged = { },
                onError = { error ->
                    Log.d("PujaBgService", "Speech recognition notice: $error")
                    if (isListening) {
                        try {
                            speechManager?.startWakeWordListening()
                        } catch (_: Throwable) {}
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e("PujaBgService", "Error setting up voice components: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_STOP_BACKGROUND_VOICE -> {
                    stopForegroundVoice()
                    stopSelf()
                    return START_NOT_STICKY
                }
                else -> {
                    startForegroundVoice()
                }
            }
        } catch (e: Throwable) {
            Log.e("PujaBgService", "Error in onStartCommand: ${e.message}")
        }
        return START_STICKY
    }

    private fun startForegroundVoice() {
        try {
            val notification = buildNotification("पूजा वॉयस बैकग्राउंड में सक्रिय है • 'Hey Puja' बोलें")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } catch (_: Throwable) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            isListening = true
            speechManager?.startWakeWordListening()
        } catch (e: Throwable) {
            Log.e("PujaBgService", "Error in startForegroundVoice: ${e.message}")
        }
    }

    private fun stopForegroundVoice() {
        try {
            isListening = false
            speechManager?.stopListening()
            speechManager?.destroy()
            ttsManager?.shutdown()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Throwable) {}
    }

    private fun handleBackgroundQuery(query: String) {
        val lower = query.lowercase().trim()
        val (spoken, action) = when {
            // Camera commands
            lower.contains("camera") || lower.contains("photo") || lower.contains("selfie") || lower.contains("video") || lower.contains("कैमरा") || lower.contains("फोटो") -> {
                val mode = when {
                    lower.contains("video") || lower.contains("वीडियो") -> "record_video"
                    lower.contains("photo") || lower.contains("selfie") || lower.contains("फोटो") -> "capture_photo"
                    lower.contains("analyze") || lower.contains("scan") || lower.contains("दृश्य") -> "analyze_view"
                    else -> "open"
                }
                Pair("जी, कैमरा खोला जा रहा है।", PujaAction.CameraControl(mode))
            }
            // SOS
            lower.contains("sos") || lower.contains("help") || lower.contains("emergency") || lower.contains("मदद") || lower.contains("खतरा") -> {
                Pair("आपातकालीन सहायता सक्रिय की जा रही है!", PujaAction.TriggerSos(true))
            }
            // Open apps
            lower.startsWith("open ") || lower.contains("khol") || lower.contains("खोलो") -> {
                val app = when {
                    lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> "WhatsApp"
                    lower.contains("youtube") || lower.contains("यूट्यूब") -> "YouTube"
                    lower.contains("instagram") || lower.contains("इंस्टाग्राम") -> "Instagram"
                    else -> query.replace("open", "", true).replace("kholo", "", true).replace("खोलो", "", true).trim()
                }
                Pair("जी, $app खोला जा रहा है।", PujaAction.OpenApp(app))
            }
            else -> {
                Pair("जी, मैंने सुना: $query", null)
            }
        }

        ttsManager?.speak(spoken)
        action?.let {
            try {
                actionExecutor?.executeAction(it)
            } catch (_: Throwable) {}
        }

        // Save background query to history safely
        serviceScope.launch {
            try {
                val db = PujaDatabase.getDatabase(applicationContext)
                val repo = InteractionRepository(db.interactionDao())
                repo.logInteraction(
                    userQuery = "[Background] $query",
                    assistantResponse = spoken,
                    spokenText = spoken,
                    detectedMood = "RESPECTFUL",
                    moodScore = 0.85f,
                    sassLevel = 50,
                    actionType = action?.type,
                    actionJson = if (action != null) "{\"action\": \"${action.type}\"}" else null,
                    actionExecuted = true
                )
            } catch (_: Throwable) {}
        }

        // Return to wake-word listening
        updateNotification("पूजा वॉयस बैकग्राउंड में सक्रिय है • 'Hey Puja' बोलें")
        if (isListening) {
            try {
                speechManager?.startWakeWordListening()
            } catch (_: Throwable) {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Puja Background Voice Assistant",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Enables continuous hands-free voice commands and wake-word listening"
                    setShowBadge(false)
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            } catch (e: Throwable) {
                Log.e("PujaBgService", "Error creating notification channel: ${e.message}")
            }
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, PujaBackgroundVoiceService::class.java).apply {
            action = ACTION_STOP_BACKGROUND_VOICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("पूजा वॉयस सहायक (Puja AI)")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "बंद करें (Stop)", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, buildNotification(statusText))
        } catch (_: Throwable) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        try {
            speechManager?.destroy()
            ttsManager?.shutdown()
            serviceScope.cancel()
        } catch (_: Throwable) {}
    }
}


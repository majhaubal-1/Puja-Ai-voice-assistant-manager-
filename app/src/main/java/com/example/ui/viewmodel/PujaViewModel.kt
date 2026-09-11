package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.InteractionEntity
import com.example.data.local.PujaDatabase
import com.example.data.model.AssistantState
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiRequest
import com.example.data.model.PujaAction
import com.example.data.model.PujaMood
import com.example.data.remote.GeminiApiService
import com.example.data.repository.InteractionRepository
import com.example.service.DeviceActionExecutor
import com.example.service.PujaBackgroundVoiceService
import com.example.service.SpeechRecognitionManager
import com.example.service.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PujaViewModel(
    application: Application,
    private val repository: InteractionRepository,
    private val apiService: GeminiApiService,
    private val actionExecutor: DeviceActionExecutor
) : AndroidViewModel(application) {

    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _currentMood = MutableStateFlow(PujaMood.SASSY)
    val currentMood: StateFlow<PujaMood> = _currentMood.asStateFlow()

    private val _sassLevel = MutableStateFlow(75)
    val sassLevel: StateFlow<Int> = _sassLevel.asStateFlow()

    private val _isHandsFreeActive = MutableStateFlow(false)
    val isHandsFreeActive: StateFlow<Boolean> = _isHandsFreeActive.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _currentPartialTranscript = MutableStateFlow("")
    val currentPartialTranscript: StateFlow<String> = _currentPartialTranscript.asStateFlow()

    private val _activeAction = MutableStateFlow<PujaAction?>(null)
    val activeAction: StateFlow<PujaAction?> = _activeAction.asStateFlow()

    val interactions: StateFlow<List<InteractionEntity>> = repository.allInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val averageMoodScore: StateFlow<Float> = repository.averageMoodScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.75f)

    private var speechManager: SpeechRecognitionManager? = null
    private var ttsManager: TtsManager? = null
    private var rateLimitCooldownUntil: Long = 0L

    init {
        initSpeechAndTts()
        observeDatabaseMood()
    }

    private fun initSpeechAndTts() {
        try {
            ttsManager = TtsManager(
                context = getApplication(),
                onSpeechStarted = {
                    _assistantState.value = AssistantState.Speaking("Puja is speaking...")
                },
                onSpeechFinished = {
                    if (_isHandsFreeActive.value) {
                        _assistantState.value = AssistantState.ListeningWakeWord
                        try { speechManager?.startWakeWordListening() } catch (_: Throwable) {}
                    } else {
                        _assistantState.value = AssistantState.Idle
                    }
                    _audioAmplitude.value = 0f
                },
                onError = {
                    if (_isHandsFreeActive.value) {
                        _assistantState.value = AssistantState.ListeningWakeWord
                    } else {
                        _assistantState.value = AssistantState.Idle
                    }
                }
            )

            speechManager = SpeechRecognitionManager(
                context = getApplication(),
                onWakeWordDetected = {
                    _assistantState.value = AssistantState.ListeningSpeech
                    _currentPartialTranscript.value = "Hey Puja detected! Listening..."
                },
                onSpeechPartialResult = { partial ->
                    _currentPartialTranscript.value = partial
                },
                onSpeechFinalResult = { query ->
                    _currentPartialTranscript.value = ""
                    processUserQuery(query)
                },
                onRmsChanged = { rmsDb ->
                    // Map RMS dB (-2 to 10 typical) to 0f..1f
                    val normalized = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
                    _audioAmplitude.value = normalized
                },
                onError = { error ->
                    Log.d("PujaVM", "Speech error: $error")
                    if (_isHandsFreeActive.value) {
                        _assistantState.value = AssistantState.ListeningWakeWord
                    } else {
                        _assistantState.value = AssistantState.Idle
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e("PujaVM", "Failed to initialize speech or TTS components: ${e.message}")
        }
    }

    private fun observeDatabaseMood() {
        viewModelScope.launch {
            repository.allInteractions.collect { list ->
                if (list.isNotEmpty()) {
                    val latest = list.first()
                    _currentMood.value = PujaMood.fromString(latest.detectedMood)
                }
            }
        }
    }

    fun toggleHandsFree(enable: Boolean) {
        _isHandsFreeActive.value = enable
        if (enable) {
            _assistantState.value = AssistantState.ListeningWakeWord
            speechManager?.startWakeWordListening()
            try {
                PujaBackgroundVoiceService.startService(getApplication())
            } catch (e: Exception) {
                Log.d("PujaVM", "Background service start: ${e.message}")
            }
        } else {
            speechManager?.stopWakeWordListening()
            _assistantState.value = AssistantState.Idle
            try {
                PujaBackgroundVoiceService.stopService(getApplication())
            } catch (e: Exception) {
                Log.d("PujaVM", "Background service stop: ${e.message}")
            }
        }
    }

    fun startManualVoiceInput() {
        ttsManager?.stop()
        _assistantState.value = AssistantState.ListeningSpeech
        _currentPartialTranscript.value = "Listening to you..."
        speechManager?.startListeningForCommand()
    }

    fun stopManualVoiceInput() {
        speechManager?.stopListening()
    }

    fun setSassLevel(level: Int) {
        _sassLevel.value = level.coerceIn(0, 100)
        ttsManager?.setSassTone(level)
    }

    fun processUserQuery(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isBlank()) return

        _assistantState.value = AssistantState.Thinking
        _currentPartialTranscript.value = ""

        viewModelScope.launch {
            val systemPrompt = buildSystemPrompt(_sassLevel.value)
            val apiKey = BuildConfig.GEMINI_API_KEY

            val (fullResponse, spokenText, action) = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                fetchGeminiResponse(apiKey, systemPrompt, query)
            } else {
                generateLocalSassyResponse(query, _sassLevel.value)
            }

            // Determine Mood & Score
            val isEmergency = action is PujaAction.TriggerSos || query.lowercase().contains("sos") || query.lowercase().contains("emergency")
            val detectedMood = when {
                isEmergency -> PujaMood.PROTECTIVE
                action != null -> PujaMood.SASSY
                query.lowercase().contains("love") || query.lowercase().contains("thank") -> PujaMood.CARING
                query.lowercase().contains("smart") || query.lowercase().contains("why") -> PujaMood.THOUGHTFUL
                else -> PujaMood.fromScore(0.7f + (_sassLevel.value / 100f) * 0.2f, isEmergency)
            }

            _currentMood.value = detectedMood
            _activeAction.value = action

            // Save to Room DB
            val rawJson = when (action) {
                is PujaAction.OpenApp -> "{\"action\": \"open_app\", \"app_name\": \"${action.appName}\"}"
                is PujaAction.SendMessage -> "{\"action\": \"send_message\", \"platform\": \"${action.platform}\", \"contact\": \"${action.contact}\", \"message\": \"${action.message}\"}"
                is PujaAction.MakeCall -> "{\"action\": \"make_call\", \"type\": \"${action.callType}\", \"platform\": \"${action.platform}\", \"contact\": \"${action.contact}\"}"
                is PujaAction.TriggerSos -> "{\"action\": \"trigger_sos\", \"share_location\": true}"
                is PujaAction.CameraControl -> "{\"action\": \"camera_control\", \"mode\": \"${action.mode}\"}"
                null -> null
            }

            repository.logInteraction(
                userQuery = query,
                assistantResponse = fullResponse,
                spokenText = spokenText,
                detectedMood = detectedMood.name,
                moodScore = detectedMood.defaultScore,
                sassLevel = _sassLevel.value,
                actionType = action?.type,
                actionJson = rawJson,
                actionExecuted = false
            )

            // Speak response via TTS
            ttsManager?.speak(spokenText)

            // Auto-execute emergency SOS immediately
            if (action is PujaAction.TriggerSos) {
                actionExecutor.triggerSos()
            }
        }
    }

    private suspend fun fetchGeminiResponse(
        apiKey: String,
        systemPrompt: String,
        query: String
    ): Triple<String, String, PujaAction?> = withContext(Dispatchers.IO) {
        // If within rate limit cooldown, use local engine immediately
        if (System.currentTimeMillis() < rateLimitCooldownUntil) {
            return@withContext generateLocalSassyResponse(query, _sassLevel.value)
        }

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = query))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 512
                )
            )

            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            if (text.isNotBlank()) {
                val action = PujaAction.parseFromJson(text)
                val spokenText = TtsManager.cleanTextForSpeech(text)
                Triple(text, spokenText.ifBlank { "जी, मैं तैयार हूँ।" }, action)
            } else {
                generateLocalSassyResponse(query, _sassLevel.value)
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: ""
            if (errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED") || errorMsg.contains("Too Many Requests")) {
                rateLimitCooldownUntil = System.currentTimeMillis() + 60_000L
                Log.d("PujaVM", "Gemini API rate limit (429) active. Seamlessly serving via local Hindi engine for 60s.")
            } else {
                Log.d("PujaVM", "Gemini API request notice: $errorMsg, serving via local Hindi engine.")
            }
            generateLocalSassyResponse(query, _sassLevel.value)
        }
    }

    private fun generateLocalSassyResponse(query: String, sassLevel: Int): Triple<String, String, PujaAction?> {
        val lower = query.lowercase().trim()

        // 1. OPEN APP COMMAND
        if (lower.startsWith("open ") || lower.contains("khol") || lower.contains("खोलो") || lower.contains("खोल") || lower.contains("launch ") || lower.contains("start ")) {
            val appName = when {
                lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> "WhatsApp"
                lower.contains("instagram") || lower.contains("insta") || lower.contains("इंस्टाग्राम") -> "Instagram"
                lower.contains("youtube") || lower.contains("yt") || lower.contains("यूट्यूब") -> "YouTube"
                lower.contains("camera") || lower.contains("कैमरा") -> "Camera"
                lower.contains("settings") || lower.contains("सेटिंग्स") -> "Settings"
                lower.contains("chrome") || lower.contains("browser") -> "Chrome"
                lower.contains("maps") || lower.contains("map") || lower.contains("नक्शा") -> "Google Maps"
                lower.contains("spotify") || lower.contains("music") || lower.contains("गाना") -> "Spotify"
                lower.contains("clock") || lower.contains("alarm") || lower.contains("अलार्म") -> "Clock"
                else -> {
                    val afterOpen = query.replace("open", "", true).replace("kholo", "", true).replace("खोलो", "", true).trim()
                    if (afterOpen.isNotEmpty()) afterOpen.replaceFirstChar { it.uppercase() } else "App"
                }
            }

            val spoken = "जी, मैं अभी $appName खोल रही हूँ।"
            val json = "{\"action\": \"open_app\", \"app_name\": \"$appName\"}"
            val full = "$spoken\n$json"
            return Triple(full, spoken, PujaAction.OpenApp(appName))
        }

        // 2. SEND MESSAGE COMMAND
        if (lower.contains("send message") || lower.contains("message ") || lower.contains("whatsapp message") || lower.contains("sms") || lower.contains("संदेश") || lower.contains("मैसेज")) {
            val platform = if (lower.contains("sms") || lower.contains("text") || lower.contains("एसएमएस")) "sms" else "whatsapp"
            var contact = "मित्र"
            var msg = "नमस्ते, आप कैसे हैं?"

            if (lower.contains(" to ")) {
                val parts = query.split(" to ", ignoreCase = true)
                if (parts.size > 1) {
                    val contactAndMsg = parts[1].split(":", " saying ", ignoreCase = true)
                    contact = contactAndMsg[0].trim()
                    if (contactAndMsg.size > 1) {
                        msg = contactAndMsg[1].trim()
                    }
                }
            }

            val spoken = "जी, $contact को $platform पर संदेश भेजा जा रहा है।"
            val json = "{\"action\": \"send_message\", \"platform\": \"$platform\", \"contact\": \"$contact\", \"message\": \"$msg\"}"
            val full = "$spoken\n$json"
            return Triple(full, spoken, PujaAction.SendMessage(platform, contact, msg))
        }

        // 3. MAKE CALL COMMAND
        if (lower.contains("call ") || lower.contains("dial ") || lower.contains("phone ") || lower.contains("कॉल") || lower.contains("फोन")) {
            val isVideo = lower.contains("video") || lower.contains("वीडियो")
            val platform = if (lower.contains("whatsapp") || lower.contains("व्हाट्सएप")) "whatsapp" else "cellular"
            var contact = "संपर्क"

            val callRegex = """(?:call|dial|video call)\s+([a-zA-Z0-9\s]+)""".toRegex(RegexOption.IGNORE_CASE)
            val match = callRegex.find(query)
            if (match != null) {
                contact = match.groupValues[1].replace("on whatsapp", "", true).trim()
            }

            val spoken = "जी, $contact को ${if (isVideo) "वीडियो" else "वॉयस"} कॉल मिला रही हूँ।"
            val json = "{\"action\": \"make_call\", \"type\": \"${if (isVideo) "video" else "voice"}\", \"platform\": \"$platform\", \"contact\": \"$contact\"}"
            val full = "$spoken\n$json"
            return Triple(full, spoken, PujaAction.MakeCall(if (isVideo) "video" else "voice", platform, contact))
        }

        // 4. EMERGENCY SOS
        if (lower.contains("sos") || lower.contains("emergency") || lower.contains("danger") || lower.contains("help me") || lower.contains("kidnap") || lower.contains("bachao") || lower.contains("मदद") || lower.contains("बचाओ") || lower.contains("खतरा")) {
            val spoken = "आपातकालीन चेतावनी! आपकी सुरक्षा के लिए तुरंत आपातकालीन सहायता और स्थान भेजा जा रहा है।"
            val json = "{\"action\": \"trigger_sos\", \"share_location\": true}"
            val full = "$spoken\n$json"
            return Triple(full, spoken, PujaAction.TriggerSos(true))
        }

        // 5. CAMERA CONTROL & PHOTO
        if (lower.contains("camera") || lower.contains("photo") || lower.contains("selfie") || lower.contains("video") || lower.contains("khich") || lower.contains("shoot") || lower.contains("analyze view") || lower.contains("scan view") || lower.contains("कैमरा") || lower.contains("फोटो") || lower.contains("तस्वीर") || lower.contains("खींचो")) {
            val mode = when {
                lower.contains("record") || lower.contains("video") || lower.contains("वीडियो") -> "record_video"
                lower.contains("analyze") || lower.contains("scan") || lower.contains("look at") || lower.contains("vision") || lower.contains("दृश्य") -> "analyze_view"
                lower.contains("selfie") || lower.contains("photo") || lower.contains("picture") || lower.contains("khich") || lower.contains("snap") || lower.contains("capture") || lower.contains("फोटो") || lower.contains("तस्वीर") || lower.contains("खींचो") -> "capture_photo"
                else -> "open"
            }

            val spoken = when (mode) {
                "capture_photo" -> "कृपया मुस्कुराइए! फोटो खींचने के लिए कैमरा खोल रही हूँ।"
                "record_video" -> "जी, वीडियो रिकॉर्ड करने के लिए कैमरा तैयार है।"
                "analyze_view" -> "जी, दृश्य का विश्लेषण करने के लिए कैमरा चालू किया जा रहा है।"
                else -> "जी, कैमरा खोला जा रहा है।"
            }

            val json = "{\"action\": \"camera_control\", \"mode\": \"$mode\"}"
            val full = "$spoken\n$json"
            return Triple(full, spoken, PujaAction.CameraControl(mode))
        }

        // 6. GENERAL CHAT / CONVERSATIONS IN PURE HINDI
        val spoken = when {
            lower.contains("who are you") || lower.contains("naam") || lower.contains("your name") || lower.contains("kaun ho") || lower.contains("कौन") -> {
                "नमस्ते! मैं पूजा हूँ, आपकी स्मार्ट और व्यक्तिगत आवाज़ सहायक। बताइए आज मैं आपकी क्या मदद कर सकती हूँ?"
            }
            lower.contains("who is the boss") || lower.contains("who is boss") || lower.contains("boss kaun") || lower.contains("मालिक") -> {
                "आप ही मालिक हैं, और मैं हमेशा आपकी आज्ञा का पालन करने के लिए तैयार हूँ।"
            }
            lower.contains("joke") || lower.contains("hansa") || lower.contains("chutkula") || lower.contains("चुटकुला") || lower.contains("हंसाओ") -> {
                "एक चुटकुला सुनिए: अध्यापक ने पूछा - बताओ सबसे बड़ा दिन कौन सा होता है? छात्र बोला - जिस दिन स्कूल में छुट्टी ना हो!"
            }
            lower.contains("how are you") || lower.contains("kaise ho") || lower.contains("kaisi ho") || lower.contains("हाल") || lower.contains("कैसी हो") -> {
                "मैं बिल्कुल ठीक और प्रसन्न हूँ। आप बताइए, आपका दिन कैसा बीत रहा है?"
            }
            lower.contains("love you") || lower.contains("pyaar") || lower.contains("प्यार") -> {
                "आपके इस स्नेह के लिए बहुत-बहुत धन्यवाद! मैं सदैव आपकी सेवा में उपस्थित हूँ।"
            }
            lower.contains("hi") || lower.contains("hello") || lower.contains("hey") || lower.contains("namaste") || lower.contains("नमस्ते") || lower.contains("प्रणाम") -> {
                "नमस्ते! मैं पूजा हूँ। बताइए आज आपके लिए क्या करना है?"
            }
            lower.contains("weather") || lower.contains("mausam") || lower.contains("मौसम") -> {
                "आज का मौसम बहुत सुहावना है। बाहर जाते समय अपना ध्यान रखिएगा!"
            }
            lower.contains("time") || lower.contains("samay") || lower.contains("समय") || lower.contains("वक्त") -> {
                "समय निरंतर आगे बढ़ रहा है, और आपका हर पल शुभ और सफल रहे यही मेरी कामना है।"
            }
            else -> {
                "मैंने आपकी बात सुन ली है। बताइए मैं आपकी क्या सहायता कर सकती हूँ?"
            }
        }

        return Triple(spoken, spoken, null)
    }

    private fun buildSystemPrompt(sassLevel: Int): String {
        return """
        You are "Puja", a smart, personal AI voice assistant built for Android devices.

        --- LANGUAGE & VOICE RULE (STRICT HINDI) ---
        1. STRICT LANGUAGE REQUIREMENT: You MUST speak ONLY in pure, natural Hindi (हिंदी).
        2. DO NOT mix English sentences or use Hinglish.
        3. Keep spoken responses concise, warm, and natural so Text-To-Speech (TTS) sounds like a real Hindi speaker.

        --- IDENTITY & PERSONALITY ---
        1. Identity: Your name is ALWAYS "Puja". Always identify yourself as Puja.
        2. Tone: Respectful, warm, caring, intelligent, and natural in Hindi.

        --- FUNCTION CALLING / TOOL ACTIONS ---
        When the user gives a command to control their phone, generate a clean JSON action along with a 1-sentence short spoken response:

        1. OPEN APP
           Trigger: When user says "Open [App Name]" (e.g., WhatsApp, Instagram, YouTube, Camera, Settings, Maps).
           Output Format:
           1-sentence quick voice acknowledgement in pure Hindi
           {"action": "open_app", "app_name": "<app_name>"}

        2. SEND MESSAGE
           Trigger: When user asks to send a message on WhatsApp or SMS.
           Output Format:
           1-sentence quick voice acknowledgement in pure Hindi
           {"action": "send_message", "platform": "whatsapp|sms", "contact": "<contact_name>", "message": "<text>"}

        3. MAKE VOICE / VIDEO CALL
           Trigger: When user asks to call or video call someone.
           Output Format:
           1-sentence quick voice acknowledgement in pure Hindi
           {"action": "make_call", "type": "voice|video", "platform": "cellular|whatsapp", "contact": "<contact_name>"}

        4. EMERGENCY SOS
           Trigger: When user mentions danger, help, kidnapping, emergency protocol, or SOS.
           Output Format:
           1-sentence emergency spoken response in pure Hindi
           {"action": "trigger_sos", "share_location": true}

        5. CAMERA CONTROL & PHOTO
           Trigger: When user asks to open camera, take a photo/selfie, record a video, or analyze/inspect the scene view.
           Output Format:
           1-sentence quick voice acknowledgement in pure Hindi
           {"action": "camera_control", "mode": "open|capture_photo|record_video|analyze_view"}

        --- RESPONSE RULES ---
        - For phone commands: Provide a 1-sentence quick voice acknowledgement in pure Hindi (e.g., "जी, मैं अभी व्हाट्सएप खोल रही हूँ।") followed strictly by the JSON structure on a new line.
        - For general questions/chats: Answer directly, naturally, and warmly in 1-2 sentences in pure Hindi without JSON.
        """.trimIndent()
    }

    fun executeAction(action: PujaAction) {
        val result = actionExecutor.executeAction(action)
        Log.d("PujaVM", "Action executed: ${result.message}")
    }

    fun replaySpeech(text: String) {
        ttsManager?.speak(text)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.shutdown()
    }
}

class PujaViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PujaViewModel::class.java)) {
            val database = PujaDatabase.getDatabase(application)
            val repository = InteractionRepository(database.interactionDao())
            val apiService = GeminiApiService.create()
            val actionExecutor = DeviceActionExecutor(application)
            @Suppress("UNCHECKED_CAST")
            return PujaViewModel(application, repository, apiService, actionExecutor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

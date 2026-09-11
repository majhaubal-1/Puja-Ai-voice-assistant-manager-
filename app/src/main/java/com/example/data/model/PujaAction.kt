package com.example.data.model

sealed class PujaAction(val type: String) {
    data class OpenApp(
        val appName: String,
        val packageName: String? = null
    ) : PujaAction("open_app")

    data class SendMessage(
        val platform: String, // "whatsapp" or "sms"
        val contact: String,
        val message: String
    ) : PujaAction("send_message")

    data class MakeCall(
        val callType: String, // "voice" or "video"
        val platform: String, // "cellular" or "whatsapp"
        val contact: String
    ) : PujaAction("make_call")

    data class TriggerSos(
        val shareLocation: Boolean = true
    ) : PujaAction("trigger_sos")

    data class CameraControl(
        val mode: String // "open" | "capture_photo" | "record_video" | "analyze_view"
    ) : PujaAction("camera_control")

    companion object {
        fun parseFromJson(json: String?): PujaAction? {
            if (json.isNullOrBlank()) return null
            try {
                val cleaned = json.trim()
                if (cleaned.contains("\"camera_control\"")) {
                    val modeRegex = """"mode"\s*:\s*"([^"]+)"""".toRegex()
                    val mode = modeRegex.find(cleaned)?.groupValues?.get(1) ?: "open"
                    return CameraControl(mode = mode)
                }
                if (cleaned.contains("\"open_app\"")) {
                    val appNameRegex = """"app_name"\s*:\s*"([^"]+)"""".toRegex()
                    val match = appNameRegex.find(cleaned)
                    val appName = match?.groupValues?.get(1) ?: "App"
                    return OpenApp(appName = appName)
                }
                if (cleaned.contains("\"send_message\"")) {
                    val platformRegex = """"platform"\s*:\s*"([^"]+)"""".toRegex()
                    val contactRegex = """"contact"\s*:\s*"([^"]+)"""".toRegex()
                    val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()

                    val platform = platformRegex.find(cleaned)?.groupValues?.get(1) ?: "whatsapp"
                    val contact = contactRegex.find(cleaned)?.groupValues?.get(1) ?: "Contact"
                    val message = messageRegex.find(cleaned)?.groupValues?.get(1) ?: ""
                    return SendMessage(platform = platform, contact = contact, message = message)
                }
                if (cleaned.contains("\"make_call\"")) {
                    val typeRegex = """"type"\s*:\s*"([^"]+)"""".toRegex()
                    val platformRegex = """"platform"\s*:\s*"([^"]+)"""".toRegex()
                    val contactRegex = """"contact"\s*:\s*"([^"]+)"""".toRegex()

                    val type = typeRegex.find(cleaned)?.groupValues?.get(1) ?: "voice"
                    val platform = platformRegex.find(cleaned)?.groupValues?.get(1) ?: "cellular"
                    val contact = contactRegex.find(cleaned)?.groupValues?.get(1) ?: "Contact"
                    return MakeCall(callType = type, platform = platform, contact = contact)
                }
                if (cleaned.contains("\"trigger_sos\"")) {
                    return TriggerSos(shareLocation = true)
                }
            } catch (_: Exception) {
                // Ignore parse errors
            }
            return null
        }
    }
}

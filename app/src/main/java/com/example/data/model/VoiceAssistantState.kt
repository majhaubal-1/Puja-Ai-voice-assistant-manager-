package com.example.data.model

sealed class AssistantState {
    object Idle : AssistantState()
    object ListeningWakeWord : AssistantState()
    object ListeningSpeech : AssistantState()
    object Thinking : AssistantState()
    data class Speaking(val text: String) : AssistantState()
    data class Error(val message: String) : AssistantState()
}

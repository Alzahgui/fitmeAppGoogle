package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ChatMessage
import com.example.data.db.FitPulseDatabase
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiCoachViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FitnessRepository

    init {
        val database = FitPulseDatabase.getDatabase(application, viewModelScope)
        repository = FitnessRepository(
            database.exerciseDao(),
            database.workoutPlanDao(),
            database.workoutLogDao(),
            database.userProfileDao()
        )
    }

    private val initialWelcome = ChatMessage(
        role = "model",
        text = "👋 Hey athlete! I'm **Coach Pulse**, your personal AI strength and conditioning specialist.\n\nAsk me anything about:\n• Exercise biomechanics and form cues\n• Workout programming and progressive overload\n• Exercise swaps for injuries or equipment limits\n• Science-backed nutrition and recovery strategies\n\nHow can I help elevate your training today?"
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(initialWelcome))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val quickChips = listOf(
        "🍗 Optimal Daily Protein",
        "🏋️ Barbell Squat Cues",
        "⏱️ Rest Time Between Sets",
        "🔄 Swap Pull-Ups (No Bar)",
        "🔥 5-Min Warm-Up Routine",
        "🛌 Fast Soreness Recovery"
    )

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(role = "user", text = userText.trim())
        val updatedList = _messages.value + userMessage
        _messages.value = updatedList
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val responseText = repository.chatWithCoach(updatedList, userText.trim())
                val modelMessage = ChatMessage(role = "model", text = responseText)
                _messages.value = _messages.value + modelMessage
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    role = "model",
                    text = "I encountered a minor glitch connecting to the fitness engine. Please ensure internet access is active and try asking again!"
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = listOf(initialWelcome)
    }
}

package com.localai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localai.domain.engine.AIModelEngine
import com.localai.domain.model.ChatMessage
import com.localai.domain.model.MessageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiModelEngine: AIModelEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentAssistantMessage = ""
    private var modelPath: String = ""

    fun loadModel(path: String) {
        if (path == modelPath && _uiState.value.isModelLoaded) return
        modelPath = path
        viewModelScope.launch {
            _uiState.update { it.copy(isModelLoaded = false, error = null) }
            val result = aiModelEngine.initialize(path)
            result.onSuccess {
                _uiState.update { it.copy(isModelLoaded = true) }
            }
            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isModelLoaded = false) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(currentInput = text) }
    }

    fun sendMessage() {
        val input = _uiState.value.currentInput.trim()
        if (input.isEmpty() || _uiState.value.isGenerating || !_uiState.value.isModelLoaded) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = input
        )

        val assistantMessageId = UUID.randomUUID().toString()
        currentAssistantMessage = ""

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + ChatMessage(
                    id = assistantMessageId,
                    role = MessageRole.ASSISTANT,
                    content = ""
                ),
                currentInput = "",
                isGenerating = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                aiModelEngine.generate(uiState.value.messages).collect { token ->
                    currentAssistantMessage += token
                    _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        val lastIdx = msgs.lastIndex
                        if (lastIdx >= 0 && msgs[lastIdx].role == MessageRole.ASSISTANT) {
                            msgs[lastIdx] = msgs[lastIdx].copy(content = currentAssistantMessage)
                        }
                        state.copy(messages = msgs)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    if (msgs.lastOrNull()?.let { it.role == MessageRole.ASSISTANT && it.content.isEmpty() } == true) {
                        msgs.removeAt(msgs.lastIndex)
                    }
                    state.copy(messages = msgs, error = e.message)
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { ChatUiState(isModelLoaded = true) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            aiModelEngine.release()
        }
    }
}

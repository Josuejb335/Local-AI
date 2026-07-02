package com.localai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localai.domain.engine.AIModelEngine
import com.localai.domain.engine.GenerationEvent
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

    private var currentAssistantContent = StringBuilder()
    private var currentThinkingContent = StringBuilder()
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
        val stateBeforeSend = _uiState.value
        val input = stateBeforeSend.currentInput.trim()
        if (input.isEmpty() || stateBeforeSend.isGenerating || !stateBeforeSend.isModelLoaded) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = input
        )
        val inferenceMessages = stateBeforeSend.messages + userMessage

        val assistantMessageId = UUID.randomUUID().toString()
        currentAssistantContent.clear()
        currentThinkingContent.clear()

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + ChatMessage(
                    id = assistantMessageId,
                    role = MessageRole.ASSISTANT,
                    content = ""
                ),
                currentInput = "",
                isGenerating = true,
                isThinking = false,
                thinkingContent = "",
                error = null
            )
        }

        viewModelScope.launch {
            try {
                aiModelEngine.generate(inferenceMessages).collect { event ->
                    when (event) {
                        is GenerationEvent.ThinkingStarted -> {
                            _uiState.update { state ->
                                state.copy(isThinking = true)
                            }
                        }

                        is GenerationEvent.ThinkingContent -> {
                            currentThinkingContent.append(event.text)
                            _uiState.update { state ->
                                state.copy(thinkingContent = currentThinkingContent.toString())
                            }
                            // Also update the message's thinking content
                            updateAssistantMessage()
                        }

                        is GenerationEvent.ThinkingEnded -> {
                            _uiState.update { state ->
                                state.copy(isThinking = false)
                            }
                        }

                        is GenerationEvent.Content -> {
                            currentAssistantContent.append(event.text)
                            updateAssistantMessage()
                        }
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
                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    val lastAssistant = msgs.lastOrNull()
                    val hasEmptyAssistant = lastAssistant?.let {
                        it.role == MessageRole.ASSISTANT && it.content.isEmpty()
                    } == true

                    if (hasEmptyAssistant) {
                        // If we have thinking content but no visible output, that's still valid
                        // (some models only think and give a brief answer)
                        val hasThinking = currentThinkingContent.isNotEmpty()
                        if (hasThinking) {
                            // Keep the message but note it had only thinking
                            msgs[msgs.lastIndex] = msgs[msgs.lastIndex].copy(
                                content = "(Model completed reasoning but produced no visible response)",
                                thinkingContent = currentThinkingContent.toString()
                            )
                            state.copy(
                                messages = msgs,
                                isGenerating = false,
                                isThinking = false,
                                thinkingContent = ""
                            )
                        } else {
                            msgs.removeAt(msgs.lastIndex)
                            state.copy(
                                messages = msgs,
                                isGenerating = false,
                                isThinking = false,
                                thinkingContent = "",
                                error = state.error ?: "Model returned no text. Try a different model or prompt."
                            )
                        }
                    } else {
                        state.copy(
                            isGenerating = false,
                            isThinking = false,
                            thinkingContent = ""
                        )
                    }
                }
            }
        }
    }

    private fun updateAssistantMessage() {
        _uiState.update { state ->
            val msgs = state.messages.toMutableList()
            val lastIdx = msgs.lastIndex
            if (lastIdx >= 0 && msgs[lastIdx].role == MessageRole.ASSISTANT) {
                msgs[lastIdx] = msgs[lastIdx].copy(
                    content = currentAssistantContent.toString(),
                    thinkingContent = currentThinkingContent.toString().ifEmpty { null }
                )
            }
            state.copy(messages = msgs)
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

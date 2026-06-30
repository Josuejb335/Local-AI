package com.localai.domain.util

import com.localai.domain.model.ChatMessage
import com.localai.domain.model.MessageRole

enum class PromptTemplate(val label: String) {
    CHATML("chatml"),
    LLAMA_3("llama3"),
    RAW("raw")
}

object PromptFormatter {

    private const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant running entirely on-device. Answer concisely and accurately."
    private const val TOKEN_ESTIMATE_PER_CHAR = 0.3f
    private const val SAFETY_MARGIN_TOKENS = 256

    fun format(
        messages: List<ChatMessage>,
        template: PromptTemplate = PromptTemplate.CHATML,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
        maxContextTokens: Int = 2048
    ): String {
        val pruned = trimToMaxTokens(messages, systemPrompt, maxContextTokens)
        return when (template) {
            PromptTemplate.CHATML -> formatChatML(pruned, systemPrompt)
            PromptTemplate.LLAMA_3 -> formatLlama3(pruned, systemPrompt)
            PromptTemplate.RAW -> formatRaw(pruned)
        }
    }

    fun estimateTokenCount(text: String): Int =
        (text.length * TOKEN_ESTIMATE_PER_CHAR).toInt().coerceAtLeast(1)

    fun trimToMaxTokens(
        messages: List<ChatMessage>,
        systemPrompt: String,
        maxTokens: Int
    ): List<ChatMessage> {
        val systemTokens = estimateTokenCount(systemPrompt)
        val available = maxTokens - systemTokens - SAFETY_MARGIN_TOKENS
        if (available <= 0) return emptyList()

        val formattedMessages = messages.toMutableList()
        var totalTokens = formattedMessages.sumOf { estimateTokenCount(it.content) }

        while (totalTokens > available && formattedMessages.size > 2) {
            val removed = formattedMessages.removeFirst()
            totalTokens -= estimateTokenCount(removed.content)
        }

        if (totalTokens > available && formattedMessages.isNotEmpty()) {
            val last = formattedMessages.last()
            val maxLastTokens = (last.content.length * available.toFloat() / totalTokens).toInt()
            val truncated = last.content.take(maxLastTokens)
            formattedMessages[formattedMessages.lastIndex] = last.copy(content = truncated)
        }

        return formattedMessages
    }

    private fun formatChatML(messages: List<ChatMessage>, systemPrompt: String): String {
        val sb = StringBuilder()
        sb.append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
        for (msg in messages) {
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            sb.append("<|im_start|>$role\n${msg.content}<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun formatLlama3(messages: List<ChatMessage>, systemPrompt: String): String {
        val sb = StringBuilder()
        sb.append("<|begin_of_text|>")
        if (systemPrompt.isNotBlank()) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>")
        }
        for (msg in messages) {
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            sb.append("<|start_header_id|>$role<|end_header_id|>\n\n${msg.content}<|eot_id|>")
        }
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }

    private fun formatRaw(messages: List<ChatMessage>): String =
        messages.joinToString("\n") { msg ->
            "${msg.role.name}: ${msg.content}"
        } + "\nassistant: "
}

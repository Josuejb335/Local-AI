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

    // Conservative token estimate: ~0.4 tokens per character accounts for
    // subword tokenization better than 0.3 (which undercounted and led to
    // context overflow with small models). Most English text averages ~0.25-0.35,
    // but special tokens, code, and non-English text can be much higher.
    private const val TOKEN_ESTIMATE_PER_CHAR = 0.4f

    // Safety margin to prevent ever hitting the context limit.
    // Accounts for template overhead (special tokens, role markers, etc.)
    private const val SAFETY_MARGIN_TOKENS = 384

    // Approximate token overhead per message from template formatting
    // (role markers, special tokens, newlines)
    private const val TEMPLATE_OVERHEAD_PER_MESSAGE = 8

    fun format(
        messages: List<ChatMessage>,
        template: PromptTemplate = PromptTemplate.CHATML,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
        maxContextTokens: Int = 2048
    ): String {
        val pruned = trimToMaxTokens(messages, systemPrompt, maxContextTokens, template)
        return when (template) {
            PromptTemplate.CHATML -> formatChatML(pruned, systemPrompt)
            PromptTemplate.LLAMA_3 -> formatLlama3(pruned, systemPrompt)
            PromptTemplate.RAW -> formatRaw(pruned)
        }
    }

    /**
     * Estimate token count for a given text string.
     * This is a conservative estimate - it intentionally overestimates
     * to prevent context overflow crashes on small models.
     */
    fun estimateTokenCount(text: String): Int =
        (text.length * TOKEN_ESTIMATE_PER_CHAR).toInt().coerceAtLeast(1)

    /**
     * Estimate total tokens for a message including template overhead.
     */
    private fun estimateMessageTokens(message: ChatMessage): Int =
        estimateTokenCount(message.content) + TEMPLATE_OVERHEAD_PER_MESSAGE

    /**
     * Estimate tokens used by the system prompt including its template wrapper.
     */
    private fun estimateSystemPromptTokens(systemPrompt: String, template: PromptTemplate): Int {
        val baseTokens = estimateTokenCount(systemPrompt)
        val overhead = when (template) {
            // <|im_start|>system\n...<|im_end|>\n  ~ 4 tokens
            PromptTemplate.CHATML -> 4
            // <|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n...<|eot_id|>  ~ 6 tokens
            PromptTemplate.LLAMA_3 -> 6
            PromptTemplate.RAW -> 0
        }
        return baseTokens + overhead
    }

    fun trimToMaxTokens(
        messages: List<ChatMessage>,
        systemPrompt: String,
        maxTokens: Int,
        template: PromptTemplate = PromptTemplate.CHATML
    ): List<ChatMessage> {
        val systemTokens = estimateSystemPromptTokens(systemPrompt, template)
        // Account for the final "assistant" prompt marker
        val assistantMarkerTokens = when (template) {
            PromptTemplate.CHATML -> 3   // <|im_start|>assistant\n
            PromptTemplate.LLAMA_3 -> 5  // <|start_header_id|>assistant<|end_header_id|>\n\n
            PromptTemplate.RAW -> 2      // \nassistant:
        }
        val available = maxTokens - systemTokens - assistantMarkerTokens - SAFETY_MARGIN_TOKENS
        if (available <= 0) return emptyList()

        val formattedMessages = messages.toMutableList()
        var totalTokens = formattedMessages.sumOf { estimateMessageTokens(it) }

        // Remove oldest messages (keep at least the last user message)
        while (totalTokens > available && formattedMessages.size > 1) {
            val removed = formattedMessages.removeFirst()
            totalTokens -= estimateMessageTokens(removed)
        }

        // If a single message is still too long, truncate its content
        if (totalTokens > available && formattedMessages.isNotEmpty()) {
            val last = formattedMessages.last()
            val contentTokenBudget = available - TEMPLATE_OVERHEAD_PER_MESSAGE
            if (contentTokenBudget > 0) {
                // Convert token budget back to approximate character count
                val maxChars = (contentTokenBudget / TOKEN_ESTIMATE_PER_CHAR).toInt()
                val truncated = last.content.take(maxChars)
                formattedMessages[formattedMessages.lastIndex] = last.copy(content = truncated)
            } else {
                // Can't fit even the overhead - return empty
                return emptyList()
            }
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

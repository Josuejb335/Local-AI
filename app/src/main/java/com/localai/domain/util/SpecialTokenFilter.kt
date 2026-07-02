package com.localai.domain.util

/**
 * Comprehensive special token filter for LLM output streams.
 *
 * Handles all known special tokens from major model families:
 * - ChatML: <|im_start|>, <|im_end|>, <|endoftext|>
 * - Llama 3: <|begin_of_text|>, <|end_of_text|>, <|start_header_id|>, <|end_header_id|>, <|eot_id|>
 * - Thinking/Reasoning: <think>, </think>, <|thinking|>, <|/thinking|>
 * - DeepSeek: <|startofthought|>, <|endofthought|>
 * - Gemma: <start_of_turn>, <end_of_turn>
 * - Mistral: [INST], [/INST], <<SYS>>, <</SYS>>
 * - General: <s>, </s>, <pad>, <unk>, [PAD], [UNK], [CLS], [SEP], [MASK]
 *
 * This filter processes streaming tokens and:
 * 1. Strips all special tokens from visible output
 * 2. Detects thinking blocks and emits them separately
 * 3. Handles partial token matches (buffering) for streaming scenarios
 * 4. Detects stop sequences that should terminate generation
 */
object SpecialTokenFilter {

    /**
     * Result of processing a token through the filter.
     */
    sealed class FilterResult {
        /** Visible text content to display to the user */
        data class Content(val text: String) : FilterResult()

        /** The model is currently in a thinking block */
        data class ThinkingContent(val text: String) : FilterResult()

        /** A thinking block has started */
        data object ThinkingStart : FilterResult()

        /** A thinking block has ended */
        data object ThinkingEnd : FilterResult()

        /** Token was a special token and was consumed (no output) */
        data object Consumed : FilterResult()

        /** A stop sequence was detected - generation should stop */
        data object Stop : FilterResult()

        /** Token is being buffered (partial match of a special token) */
        data object Buffered : FilterResult()
    }

    // -- Special tokens that should be completely stripped from output --

    private val STRIP_TOKENS = setOf(
        // ChatML
        "<|im_start|>",
        "<|im_end|>",
        "<|endoftext|>",
        "<|im_sep|>",

        // Llama 3 / Meta
        "<|begin_of_text|>",
        "<|end_of_text|>",
        "<|start_header_id|>",
        "<|end_header_id|>",
        "<|eot_id|>",
        "<|finetune_right_pad_id|>",
        "<|step_id|>",

        // Gemma
        "<start_of_turn>",
        "<end_of_turn>",
        "<bos>",
        "<eos>",

        // Mistral / Llama 2
        "[INST]",
        "[/INST]",
        "<<SYS>>",
        "<</SYS>>",

        // General BOS/EOS/padding
        "<s>",
        "</s>",
        "<pad>",
        "<unk>",
        "[PAD]",
        "[UNK]",
        "[CLS]",
        "[SEP]",
        "[MASK]",
        "[gMASK]",
        "[sMASK]",

        // ChatGLM
        "<|user|>",
        "<|assistant|>",
        "<|system|>",
        "<|observation|>",

        // Phi
        "<|end|>",
        "<|endofprompt|>",

        // Qwen
        "<|extra_0|>",
        "<|extra_1|>",

        // Command-R
        "<|START_OF_TURN_TOKEN|>",
        "<|END_OF_TURN_TOKEN|>",
        "<|SYSTEM_TOKEN|>",
        "<|USER_TOKEN|>",
        "<|CHATBOT_TOKEN|>",

        // Yi
        "<|im_start|>",
        "<|im_end|>",

        // InternLM
        "<|action_start|>",
        "<|action_end|>",
        "<|interpreter|>",
        "<|plugin|>"
    )

    // -- Thinking tokens --

    private val THINKING_START_TOKENS = setOf(
        "<think>",
        "<|thinking|>",
        "<|startofthought|>",
        "<|thought_start|>"
    )

    private val THINKING_END_TOKENS = setOf(
        "</think>",
        "<|/thinking|>",
        "<|endofthought|>",
        "<|thought_end|>"
    )

    // -- Stop sequences that should terminate generation --

    private val STOP_TOKENS = setOf(
        "<|im_end|>",
        "<|eot_id|>",
        "<|end_of_text|>",
        "<|endoftext|>",
        "</s>",
        "<end_of_turn>",
        "<|end|>",
        "<|END_OF_TURN_TOKEN|>"
    )

    // -- Role header patterns to strip (model echoing roles) --

    private val ROLE_PATTERNS = listOf(
        Regex("^(user|assistant|system|human|bot):\\s*", RegexOption.IGNORE_CASE),
        Regex("^###\\s*(User|Assistant|System|Human|Bot)\\s*", RegexOption.IGNORE_CASE)
    )

    /**
     * Stateful streaming filter that handles partial token matches and thinking blocks.
     *
     * Create one instance per generation session.
     */
    class StreamingFilter {
        private val buffer = StringBuilder()
        private var isInThinkingBlock = false
        private var isFirstOutput = true
        private var consecutiveNewlines = 0

        /** Whether the model is currently in a thinking block */
        val isThinking: Boolean get() = isInThinkingBlock

        /**
         * Process a single streamed token and return the appropriate result(s).
         *
         * @param token The raw token string from the model
         * @return List of FilterResults (may be multiple if buffer is flushed)
         */
        fun process(token: String): List<FilterResult> {
            if (token.isEmpty()) return emptyList()

            buffer.append(token)
            val content = buffer.toString()

            // Check if the buffer IS a complete special token
            val completeMatch = findCompleteSpecialToken(content)
            if (completeMatch != null) {
                buffer.clear()
                return handleSpecialToken(completeMatch)
            }

            // Check if the buffer CONTAINS special tokens (token came mid-text)
            val results = extractAndProcessBuffer()
            if (results.isNotEmpty()) return results

            // Check if the buffer could be the START of a special token (partial match)
            if (couldBePartialSpecialToken(content)) {
                return listOf(FilterResult.Buffered)
            }

            // No special token involvement - emit buffer as content
            buffer.clear()
            return emitContent(content)
        }

        /**
         * Flush any remaining buffered content. Call when generation ends.
         *
         * @return Any remaining content that was buffered
         */
        fun flush(): List<FilterResult> {
            if (buffer.isEmpty()) return emptyList()
            val content = buffer.toString()
            buffer.clear()

            // Final check: if it's a partial special token that never completed,
            // emit it as content (unless it's a known strip token)
            val stripped = stripAllSpecialTokens(content)
            if (stripped.isEmpty()) return emptyList()

            return emitContent(stripped)
        }

        /**
         * Reset the filter state for a new generation.
         */
        fun reset() {
            buffer.clear()
            isInThinkingBlock = false
            isFirstOutput = true
            consecutiveNewlines = 0
        }

        private fun handleSpecialToken(token: String): List<FilterResult> {
            // Check thinking start
            if (token.lowercase() in THINKING_START_TOKENS.map { it.lowercase() }) {
                isInThinkingBlock = true
                return listOf(FilterResult.ThinkingStart)
            }

            // Check thinking end
            if (token.lowercase() in THINKING_END_TOKENS.map { it.lowercase() }) {
                isInThinkingBlock = false
                return listOf(FilterResult.ThinkingEnd)
            }

            // Check stop tokens
            if (token.lowercase() in STOP_TOKENS.map { it.lowercase() }) {
                return listOf(FilterResult.Stop)
            }

            // It's a strip token - just consume it
            return listOf(FilterResult.Consumed)
        }

        private fun extractAndProcessBuffer(): List<FilterResult> {
            val content = buffer.toString()
            val results = mutableListOf<FilterResult>()

            // Find the earliest special token in the buffer
            var earliestPos = Int.MAX_VALUE
            var earliestToken = ""
            var earliestLen = 0

            val allTokens = STRIP_TOKENS + THINKING_START_TOKENS + THINKING_END_TOKENS + STOP_TOKENS
            for (specialToken in allTokens) {
                val pos = content.lowercase().indexOf(specialToken.lowercase())
                if (pos != -1 && pos < earliestPos) {
                    earliestPos = pos
                    earliestToken = specialToken
                    earliestLen = specialToken.length
                }
            }

            if (earliestPos == Int.MAX_VALUE) return emptyList()

            // Emit content before the special token
            if (earliestPos > 0) {
                val beforeText = content.substring(0, earliestPos)
                results.addAll(emitContent(beforeText))
            }

            // Handle the special token
            results.addAll(handleSpecialToken(earliestToken))

            // Process remaining content after the special token
            val remaining = content.substring(earliestPos + earliestLen)
            buffer.clear()
            if (remaining.isNotEmpty()) {
                buffer.append(remaining)
                val subResults = extractAndProcessBuffer()
                if (subResults.isNotEmpty()) {
                    results.addAll(subResults)
                } else if (!couldBePartialSpecialToken(remaining)) {
                    buffer.clear()
                    results.addAll(emitContent(remaining))
                } else {
                    results.add(FilterResult.Buffered)
                }
            }

            return results
        }

        private fun emitContent(text: String): List<FilterResult> {
            if (text.isEmpty()) return emptyList()

            var cleaned = text

            // Strip role headers at the beginning of output
            if (isFirstOutput) {
                for (pattern in ROLE_PATTERNS) {
                    cleaned = cleaned.replace(pattern, "")
                }
                // Strip leading whitespace/newlines only at start
                cleaned = cleaned.trimStart('\n', '\r')
                if (cleaned.isNotEmpty()) {
                    isFirstOutput = false
                }
            }

            if (cleaned.isEmpty()) return listOf(FilterResult.Consumed)

            // Track consecutive newlines to prevent excessive spacing
            val newlineCount = cleaned.count { it == '\n' }
            if (cleaned.all { it == '\n' || it == '\r' || it == ' ' }) {
                consecutiveNewlines += newlineCount
                if (consecutiveNewlines > 2) {
                    return listOf(FilterResult.Consumed)
                }
            } else {
                consecutiveNewlines = 0
            }

            return if (isInThinkingBlock) {
                listOf(FilterResult.ThinkingContent(cleaned))
            } else {
                listOf(FilterResult.Content(cleaned))
            }
        }

        private fun findCompleteSpecialToken(text: String): String? {
            val lower = text.lowercase().trim()
            val allTokens = STRIP_TOKENS + THINKING_START_TOKENS + THINKING_END_TOKENS + STOP_TOKENS
            return allTokens.find { it.lowercase() == lower }
        }

        private fun couldBePartialSpecialToken(text: String): Boolean {
            if (text.length > 50) return false // No special token is this long
            val lower = text.lowercase()
            val allTokens = STRIP_TOKENS + THINKING_START_TOKENS + THINKING_END_TOKENS + STOP_TOKENS
            return allTokens.any { token ->
                val tokenLower = token.lowercase()
                // Buffer is a prefix of a token
                tokenLower.startsWith(lower) ||
                // Buffer ends with the start of a token
                (lower.length < tokenLower.length && lower.isNotEmpty() &&
                    tokenLower.startsWith(lower.takeLast(minOf(lower.length, tokenLower.length))))
            }
        }
    }

    /**
     * Simple one-shot stripping of all known special tokens from a string.
     * Useful for cleaning up final output or stored messages.
     */
    fun stripAllSpecialTokens(text: String): String {
        var result = text
        val allTokens = STRIP_TOKENS + THINKING_START_TOKENS + THINKING_END_TOKENS + STOP_TOKENS
        for (token in allTokens.sortedByDescending { it.length }) {
            result = result.replace(token, "", ignoreCase = true)
        }
        // Clean up excessive whitespace left behind
        result = result.replace(Regex("\n{3,}"), "\n\n")
        return result.trim()
    }

    /**
     * Extract the thinking content from a raw model output.
     * Returns a Pair of (thinking content, visible content).
     */
    fun extractThinkingBlock(text: String): Pair<String?, String> {
        val thinkPatterns = listOf(
            Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL),
            Regex("<\\|thinking\\|>(.*?)<\\|/thinking\\|>", RegexOption.DOT_MATCHES_ALL),
            Regex("<\\|startofthought\\|>(.*?)<\\|endofthought\\|>", RegexOption.DOT_MATCHES_ALL),
            Regex("<\\|thought_start\\|>(.*?)<\\|thought_end\\|>", RegexOption.DOT_MATCHES_ALL)
        )

        for (pattern in thinkPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val thinking = match.groupValues[1].trim()
                val visible = text.removeRange(match.range).let { stripAllSpecialTokens(it) }
                return Pair(thinking.ifEmpty { null }, visible)
            }
        }

        return Pair(null, stripAllSpecialTokens(text))
    }

    /**
     * Check if a token string is a known stop sequence.
     */
    fun isStopToken(token: String): Boolean {
        return token.lowercase().trim() in STOP_TOKENS.map { it.lowercase() }
    }

    /**
     * Check if a token is any known special token.
     */
    fun isSpecialToken(token: String): Boolean {
        val lower = token.lowercase().trim()
        val allTokens = STRIP_TOKENS + THINKING_START_TOKENS + THINKING_END_TOKENS + STOP_TOKENS
        return lower in allTokens.map { it.lowercase() }
    }
}

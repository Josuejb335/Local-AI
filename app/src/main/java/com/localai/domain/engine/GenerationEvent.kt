package com.localai.domain.engine

/**
 * Events emitted during text generation.
 *
 * Allows the engine to communicate not just visible text but also
 * thinking/reasoning content and state changes to the UI layer.
 */
sealed class GenerationEvent {
    /** Visible content token to display to the user */
    data class Content(val text: String) : GenerationEvent()

    /** The model has started a thinking/reasoning block */
    data object ThinkingStarted : GenerationEvent()

    /** Content from within a thinking/reasoning block */
    data class ThinkingContent(val text: String) : GenerationEvent()

    /** The model has finished a thinking/reasoning block */
    data object ThinkingEnded : GenerationEvent()
}

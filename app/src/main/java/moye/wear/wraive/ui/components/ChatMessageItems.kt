package moye.wear.wraive.ui.components

import moye.wear.wraive.model.ChatMessage
import moye.wear.wraive.model.MessageRole

internal enum class MessagePart { FULL, HEADER, REASONING, FOOTER }

internal data class ChatMessageItem(
    val message: ChatMessage,
    val part: MessagePart,
    val reasoningText: String = "",
    val offset: Int = 0
) {
    val key: String
        get() = when (part) {
            MessagePart.FULL, MessagePart.HEADER -> message.id
            else -> "${message.id}:${part.name}:$offset"
        }
}

internal fun chatMessageItems(
    messages: List<ChatMessage>,
    expandedIds: List<String>,
    showReasoning: Boolean
): List<ChatMessageItem> = buildList {
    messages.forEach { message ->
        if (showReasoning && message.role == MessageRole.ASSISTANT &&
            message.id in expandedIds && message.reasoning.isNotBlank()
        ) {
            add(ChatMessageItem(message, MessagePart.HEADER))
            var offset = 0
            splitReasoning(message.reasoning).forEach { chunk ->
                add(ChatMessageItem(message, MessagePart.REASONING, chunk, offset))
                offset += chunk.length
            }
            add(ChatMessageItem(message, MessagePart.FOOTER))
        } else {
            add(ChatMessageItem(message, MessagePart.FULL))
        }
    }
}

internal fun splitReasoning(text: String): List<String> = buildList {
    var start = 0
    while (start < text.length) {
        var end = (start + 768).coerceAtMost(text.length)
        if (end < text.length) {
            var lineEnd = end
            while (lineEnd > start && text[lineEnd - 1] != '\n') lineEnd--
            if (lineEnd > start) {
                end = lineEnd
            } else if (text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) {
                end--
            }
        }
        add(text.substring(start, end))
        start = end
    }
}

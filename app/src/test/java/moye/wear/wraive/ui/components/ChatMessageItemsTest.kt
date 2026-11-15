package moye.wear.wraive.ui.components

import moye.wear.wraive.model.ChatMessage
import moye.wear.wraive.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageItemsTest {
    @Test
    fun chunksPreserveLongParagraphsNewlinesAndEmoji() {
        listOf("", "分析思考".repeat(5_000), "段落\n\n后续\r\n".repeat(500), "字".repeat(767) + "🙂".repeat(1_000))
            .forEach { text ->
                val chunks = splitReasoning(text)
                assertEquals(text, chunks.joinToString(""))
                assertTrue(chunks.all { it.length <= 768 && it.isNotEmpty() })
                assertTrue(chunks.none { it.last().isHighSurrogate() || it.first().isLowSurrogate() })
            }
    }

    @Test
    fun streamingKeepsCompletedChunkKeysAndTextStable() {
        val message = ChatMessage(id = "reply", conversationId = "chat", role = MessageRole.ASSISTANT,
            reasoning = "逐段核对信息。\n".repeat(500), content = "结论")
        val before = chatMessageItems(listOf(message), listOf(message.id), true)
        val after = chatMessageItems(listOf(message.copy(reasoning = message.reasoning + "继续生成。".repeat(200))),
            listOf(message.id), true)
        before.filter { it.part == MessagePart.REASONING }.dropLast(1).forEach { chunk ->
            assertEquals(chunk.reasoningText, after.single { it.key == chunk.key }.reasoningText)
        }
        assertEquals(message.id, after.first().key)
        assertEquals(after.size, after.map { it.key }.distinct().size)
        assertEquals(MessagePart.FOOTER, after.last().part)
        assertEquals("结论", after.last().message.content)
    }
}

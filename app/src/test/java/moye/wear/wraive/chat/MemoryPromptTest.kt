package moye.wear.wraive.chat

import com.google.gson.Gson
import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.ChatMessage
import moye.wear.wraive.model.MessageRole
import moye.wear.wraive.model.ModelConfig
import moye.wear.wraive.model.MemoryEntry
import java.text.DateFormat
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryPromptTest {
    @Test
    fun promptAcceptsDeserializedMemoriesWithMissingOrUnknownCategories() {
        val gson = Gson()
        val memories = listOf("null", "\"unknown-category\"").mapIndexed { index, category ->
            gson.fromJson(
                """{"id":"memory-$index","category":$category,"content":"记忆$index","enabled":true}""",
                MemoryEntry::class.java
            )
        }
        val prompt = PromptComposer().turnContext(
            assistant = AssistantProfile(name = "助手"),
            memories = memories,
            worldBookEntries = emptyList(),
            history = emptyList()
        )
        assertTrue(prompt.contains("identity: 记忆0"))
        assertTrue(prompt.contains("identity: 记忆1"))
    }

    @Test
    fun clockTemplateUsesTheUserTurnsTimestampEvenAfterAToolReply() {
        val composer = PromptComposer()
        val assistant = AssistantProfile(name = "Watch", systemPrompt = "Report the time {{datetime}}")
        val model = ModelConfig(id = "model", providerId = "provider")
        val system = composer.systemPrompt(assistant, model, "Previously agreed plan")
        assertTrue(system.contains("Report the time {{datetime}}"))
        assertTrue(system.contains("Previously agreed plan"))
        val user = ChatMessage(conversationId = "chat", role = MessageRole.USER, content = "Now?", createdAt = 0L)
        val tool = user.copy(role = MessageRole.TOOL, content = "Result", createdAt = 86400000L)
        val history = listOf(user, tool)
        val context = composer.turnContext(assistant, emptyList(), emptyList(), history)
        assertEquals("{{datetime}} = " + DateFormat.getDateTimeInstance().format(Date(0L)), context)
        val request = composer.withTurnContext(history, context)
        assertTrue(request.first().content.endsWith("Now?"))
        assertEquals(tool, request.last())
        assertEquals("Now?", history.first().content)
    }
}

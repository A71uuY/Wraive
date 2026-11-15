package moye.wear.wraive.chat

import com.google.gson.Gson
import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.MemoryEntry
import moye.wear.wraive.model.ModelConfig
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
        val prompt = PromptComposer().systemPrompt(
            assistant = AssistantProfile(name = "助手"),
            model = ModelConfig(id = "model", providerId = "provider"),
            memories = memories,
            worldBookEntries = emptyList(),
            history = emptyList(),
            conversationSummary = ""
        )
        assertTrue(prompt.contains("identity: 记忆0"))
        assertTrue(prompt.contains("identity: 记忆1"))
    }
}

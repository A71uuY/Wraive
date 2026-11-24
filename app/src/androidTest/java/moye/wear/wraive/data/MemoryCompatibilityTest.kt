package moye.wear.wraive.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import moye.wear.wraive.WraiveApplication
import moye.wear.wraive.chat.PromptComposer
import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.MemoryCategory
import moye.wear.wraive.model.MemoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MemoryCompatibilityTest {
    @Test
    fun memoriesWithNullOrUnknownCategoriesStillComposePrompts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = (context.applicationContext as WraiveApplication).graph.repository
        val brokenIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val valid = MemoryEntry(category = MemoryCategory.WORKFLOW, content = "正常提炼的记忆")
        try {
            WraiveDatabase(context).use { database ->
                brokenIds.zip(listOf("null", "\"stripped-category\"")).forEach { (id, category) ->
                    database.put(WraiveDatabase.KIND_MEMORY, id,
                        """{"id":"$id","category":$category,"content":"旧版记忆","enabled":true,"createdAt":1,"updatedAt":1}""")
                }
            }
            repository.upsertMemory(valid)
            val restored = repository.memories.value.filter { it.id in brokenIds || it.id == valid.id }
            assertEquals(3, restored.size)
            restored.filter { it.id in brokenIds }.forEach {
                assertEquals(MemoryCategory.IDENTITY, it.category)
                assertEquals("旧版记忆", it.content)
            }
            val prompt = PromptComposer().turnContext(
                assistant = AssistantProfile(name = "助手"),
                memories = restored,
                worldBookEntries = emptyList(),
                history = emptyList()
            )
            assertTrue(prompt.contains("identity: 旧版记忆"))
            assertTrue(prompt.contains("workflow: 正常提炼的记忆"))
        } finally {
            (brokenIds + valid.id).forEach { repository.deleteMemory(it) }
        }
    }
}

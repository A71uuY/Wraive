package moye.wear.wraive.chat

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import moye.wear.wraive.WraiveApplication
import moye.wear.wraive.data.PreferencesStore
import moye.wear.wraive.data.WraiveDatabase
import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.MemoryCategory
import moye.wear.wraive.model.MessageStatus
import moye.wear.wraive.model.ModelConfig
import moye.wear.wraive.model.ModelRef
import moye.wear.wraive.model.ProviderConfig
import moye.wear.wraive.model.ProviderProtocol
import moye.wear.wraive.network.AiGateway
import moye.wear.wraive.network.McpClient
import moye.wear.wraive.network.SearchClient
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MemoryExtractionTest {
    @Test
    fun conversationContinuesAfterExtractionAlongsideAnOlderNullCategory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = (context.applicationContext as WraiveApplication).graph.repository
        withTimeout(10_000) { repository.assistants.first { it.isNotEmpty() } }
        val gson = Gson()
        val preferences = PreferencesStore(context, gson)
        val originalPreferences = preferences.preferences.value
        preferences.update { it.copy(backgroundGeneration = false, autoTitle = false, contextCompressionEnabled = false) }
        val provider = ProviderConfig(name = "Memory test", protocol = ProviderProtocol.OPENAI_CHAT,
            baseUrl = "https://example.invalid/v1", apiKey = "test-key")
        val model = ModelConfig(id = "memory-test", providerId = provider.id)
        val assistant = AssistantProfile(name = "Memory test", model = ModelRef(provider.id, model.id),
            memoryEnabled = true, autoMemoryExtraction = true)
        val extracted = MemoryCategory.entries.map { mapOf("category" to it.name, "content" to "长期记忆 ${it.name}") }
        var lastConversationPrompt = ""
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = Buffer()
            chain.request().body!!.writeTo(buffer)
            val payload = JsonParser.parseString(buffer.readUtf8()).asJsonObject
            val system = payload.getAsJsonArray("messages").first().asJsonObject.get("content").asString
            val text = if (system == "Return valid JSON only.") gson.toJson(extracted) else {
                lastConversationPrompt = payload.getAsJsonArray("messages").last().asJsonObject.get("content").asString
                "正常回复"
            }
            val data = JsonObject().apply {
                add("choices", gson.toJsonTree(listOf(mapOf("delta" to mapOf("content" to text), "finish_reason" to "stop"))))
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("data: $data\n\ndata: [DONE]\n\n".toResponseBody()).build()
        }.build()
        val engine = ChatEngine(context, repository, preferences, AiGateway(http, gson, context),
            ToolRuntime(repository, SearchClient(http, gson), McpClient(http, gson), http, gson))
        val olderId = UUID.randomUUID().toString()
        var conversationId: String? = null
        try {
            repository.upsertProvider(provider)
            repository.upsertModel(model)
            repository.upsertAssistant(assistant)
            val conversation = engine.createConversation(assistant.id)
            conversationId = conversation.id
            repeat(2) { withTimeout(10_000) { engine.send(conversation, "第${it + 1}条消息").join() } }
            WraiveDatabase(context).use { database ->
                database.put(WraiveDatabase.KIND_MEMORY, olderId,
                    """{"id":"$olderId","assistantId":"${assistant.id}","category":null,"content":"旧记忆","enabled":true,"createdAt":1,"updatedAt":1}""",
                    parentId = assistant.id)
            }
            withTimeout(10_000) { engine.send(conversation, "第三条消息，触发自动提炼").join() }
            val memories = repository.memories.value.filter { it.assistantId == assistant.id }
            assertEquals(5, memories.size)
            assertEquals(MemoryCategory.entries.toSet(), memories.map { it.effectiveCategory }.toSet())
            withTimeout(10_000) { engine.send(conversation, "提炼后继续对话").join() }
            val reply = repository.messages(conversation.id).last()
            assertEquals(MessageStatus.COMPLETE, reply.status)
            assertEquals("正常回复", reply.content)
            assertTrue(lastConversationPrompt.contains("identity: 旧记忆"))
            MemoryCategory.entries.forEach {
                assertTrue(lastConversationPrompt.contains("${it.name.lowercase()}: 长期记忆 ${it.name}"))
            }
        } finally {
            (repository.memories.value.filter { it.assistantId == assistant.id }.map { it.id } + olderId)
                .distinct().forEach { repository.deleteMemory(it) }
            conversationId?.let { repository.deleteConversation(it) }
            repository.deleteAssistant(assistant.id)
            repository.deleteProvider(provider.id)
            preferences.replace(originalPreferences)
        }
    }
}

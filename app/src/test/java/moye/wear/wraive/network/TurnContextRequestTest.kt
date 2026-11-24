package moye.wear.wraive.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import moye.wear.wraive.chat.PromptComposer
import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.ChatMessage
import moye.wear.wraive.model.MemoryCategory
import moye.wear.wraive.model.MemoryEntry
import moye.wear.wraive.model.MessageRole
import moye.wear.wraive.model.ModelConfig
import moye.wear.wraive.model.ProviderConfig
import moye.wear.wraive.model.ProviderProtocol
import moye.wear.wraive.model.WorldBookEntry
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnContextRequestTest {
    @Test
    fun newMemoryAndWorldBookEntryOnlyAppearInLatestUserMessage() = runBlocking {
        val composer = PromptComposer()
        val assistant = AssistantProfile(name = "Watch", systemPrompt = "Rules for {{assistant}}",
            worldBookIds = setOf("travel"))
        val model = ModelConfig(id = "model", providerId = "provider")
        val memory = MemoryEntry(category = MemoryCategory.WORKFLOW, content = "Prefer metric units")
        val entry = WorldBookEntry(bookId = "travel", title = "Transit", keywords = listOf("rail"),
            content = "The line closes early")
        val question = ChatMessage(conversationId = "chat", role = MessageRole.USER, content = "Hello")
        val answer = question.copy(role = MessageRole.ASSISTANT, content = "Hi")
        val followUp = question.copy(content = "Which rail line?")
        val turns = listOf(listOf(question), listOf(question, answer, followUp))
        val gson = Gson()
        for (protocol in ProviderProtocol.entries) {
            val payloads = mutableListOf<JsonObject>()
            val http = OkHttpClient.Builder().addInterceptor { chain ->
                val buffer = Buffer()
                requireNotNull(chain.request().body).writeTo(buffer)
                payloads += JsonParser.parseString(buffer.readUtf8()).asJsonObject
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("".toResponseBody()).build()
            }.build()
            val provider = ProviderConfig(id = model.providerId, name = "Test", protocol = protocol,
                baseUrl = "https://example.invalid")
            val client = when (protocol) {
                ProviderProtocol.OPENAI_CHAT, ProviderProtocol.OPENAI_RESPONSES -> OpenAiClient(http, gson)
                ProviderProtocol.ANTHROPIC -> AnthropicClient(http, gson)
                ProviderProtocol.GOOGLE -> GoogleClient(http, gson)
            }
            for ((index, history) in turns.withIndex()) {
                val memories = if (index == 0) emptyList() else listOf(memory)
                val context = composer.turnContext(assistant, memories, listOf(entry), history)
                client.stream(CompletionRequest(provider, model, assistant,
                    composer.systemPrompt(assistant, model, ""), composer.withTurnContext(history, context))).toList()
            }
            val field = when (protocol) {
                ProviderProtocol.OPENAI_RESPONSES -> "input"
                ProviderProtocol.GOOGLE -> "contents"
                else -> "messages"
            }
            val first = payloads.first().remove(field).asJsonArray
            val second = payloads.last().remove(field).asJsonArray
            assertEquals(protocol.name, payloads.first(), payloads.last())
            assertTrue(protocol.name, first.last().toString().contains(question.content))
            assertEquals(protocol.name, first.toList(), second.take(first.size()))
            assertEquals(protocol.name, first.size() + 2, second.size())
            assertTrue(protocol.name, second[second.size() - 2].toString().contains(answer.content))
            val last = second.last().toString()
            for (detail in listOf(memory.content, entry.content)) {
                assertFalse(protocol.name, payloads.last().toString().contains(detail))
                assertTrue(protocol.name, second.toList().dropLast(1).none { it.toString().contains(detail) })
                assertTrue(protocol.name, last.contains(detail))
                assertTrue(protocol.name, last.indexOf(detail) < last.indexOf(followUp.content))
            }
        }
        assertEquals("Which rail line?", followUp.content)
    }
}

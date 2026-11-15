package moye.wear.wraive.chat

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import moye.wear.wraive.WraiveApplication
import moye.wear.wraive.data.PreferencesStore
import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.Conversation
import moye.wear.wraive.model.MessageRole
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
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ToolConversationTest {
    @Test
    fun continuesThroughSixToolRoundsAndOffersOfficialTools() = runBlocking {
        val requests = AtomicInteger()
        withEngine({ payload ->
            val tools = payload.getAsJsonArray("tools").map { it.asJsonObject.get("type").asString }
            assertTrue(tools.contains("deepseek_web_search"))
            assertTrue(tools.contains("deepseek_read_url"))
            val round = requests.incrementAndGet()
            if (round <= 6) toolReply(round) else
                """{"choices":[{"delta":{"content":"完成"},"finish_reason":"stop"}]}"""
        }) { engine, conversation ->
            withTimeout(30_000) { engine.send(conversation, "计算").join() }
            val messages = engine.observeMessages(conversation.id).first()
            assertEquals(7, requests.get())
            assertEquals(6, messages.count { it.role == MessageRole.TOOL })
            assertEquals("完成", messages.last().content)
            assertEquals(MessageStatus.COMPLETE, messages.last().status)
            assertFalse(conversation.id in engine.generatingConversationIds.value)
        }
    }

    @Test
    fun cancellationStopsFurtherToolRounds() = runBlocking {
        val requests = AtomicInteger()
        val reachedFifth = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        withEngine({ _ ->
            val round = requests.incrementAndGet()
            if (round == 5) {
                reachedFifth.complete(Unit)
                check(release.await(15, TimeUnit.SECONDS))
            }
            toolReply(round)
        }) { engine, conversation ->
            val job = engine.send(conversation, "计算")
            try {
                withTimeout(30_000) { reachedFifth.await() }
                engine.cancel(conversation.id)
            } finally {
                release.countDown()
            }
            withTimeout(10_000) { job.join() }
            assertTrue(job.isCancelled)
            assertEquals(5, requests.get())
            assertFalse(conversation.id in engine.generatingConversationIds.value)
        }
    }

    private suspend fun withEngine(
        reply: (JsonObject) -> String,
        verify: suspend (ChatEngine, Conversation) -> Unit
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = (context.applicationContext as WraiveApplication).graph.repository
        withTimeout(10_000) { repository.assistants.first { it.isNotEmpty() } }
        val gson = Gson()
        val preferences = PreferencesStore(context, gson)
        val originalPreferences = preferences.preferences.value
        preferences.update { it.copy(backgroundGeneration = false, autoTitle = false, contextCompressionEnabled = false) }
        val provider = ProviderConfig(name = "Test DeepSeek", protocol = ProviderProtocol.OPENAI_CHAT,
            baseUrl = "https://api.deepseek.com", apiKey = "test-key")
        val model = ModelConfig(id = "deepseek-flash", providerId = provider.id)
        val assistant = AssistantProfile(name = "Tool test", model = ModelRef(provider.id, model.id), memoryEnabled = false)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = Buffer()
            chain.request().body!!.writeTo(buffer)
            val data = reply(JsonParser.parseString(buffer.readUtf8()).asJsonObject)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("data: $data\n\ndata: [DONE]\n\n".toResponseBody()).build()
        }.build()
        val engine = ChatEngine(context, repository, preferences, AiGateway(http, gson, context),
            ToolRuntime(repository, SearchClient(http, gson), McpClient(http, gson), http, gson))
        var conversation: Conversation? = null
        try {
            repository.upsertProvider(provider)
            repository.upsertModel(model)
            repository.upsertAssistant(assistant)
            conversation = engine.createConversation(assistant.id, temporary = true)
            verify(engine, conversation)
        } finally {
            conversation?.let {
                engine.discardTemporary(it.id)
                repository.deleteConversation(it.id)
            }
            repository.deleteAssistant(assistant.id)
            repository.deleteProvider(provider.id)
            preferences.replace(originalPreferences)
        }
    }

    private fun toolReply(round: Int): String =
        """{"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call$round","type":"function","function":{"name":"calculator","arguments":"{\"expression\":\"1+1\"}"}}]},"finish_reason":"tool_calls"}]}"""
}

package moye.wear.wraive.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.ChatMessage
import moye.wear.wraive.model.MessageRole
import moye.wear.wraive.model.ModelConfig
import moye.wear.wraive.model.ProviderConfig
import moye.wear.wraive.model.ProviderMessageContent
import moye.wear.wraive.model.ProviderProtocol
import moye.wear.wraive.model.StreamEvent
import moye.wear.wraive.model.ToolCall
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class DeepSeekToolsTest {
    private val gson = Gson()
    private val openAi = OpenAiClient(OkHttpClient(), gson)

    private fun request(protocol: ProviderProtocol, model: String = "deepseek-flash") = CompletionRequest(
        provider = ProviderConfig(id = "deepseek", name = "DeepSeek", protocol = protocol,
            baseUrl = "https://api.deepseek.com/v1"),
        model = ModelConfig(id = model, providerId = "deepseek"),
        assistant = AssistantProfile(name = "助手"),
        systemPrompt = "",
        messages = listOf(ChatMessage(conversationId = "chat", role = MessageRole.USER, content = "你好"))
    )

    @Test
    fun defaultsUseDocumentedModelAndEndpointCapabilities() {
        val request = request(ProviderProtocol.OPENAI_CHAT)
        for (model in listOf("deepseek-flash", "deepseek-v4-flash", "deepseek-v4-flash-vision-exp")) {
            assertEquals(listOf("deepseek_web_search", "deepseek_read_url"),
                defaultDeepSeekTools(request.provider, model).map { it.string("type") })
        }
        assertTrue(defaultDeepSeekTools(request.provider, "unknown-model").isEmpty())
        for (url in listOf("https://proxy.example/api.deepseek.com", "https://api.deepseek.com.example/v1")) {
            assertTrue(defaultDeepSeekTools(request.provider.copy(baseUrl = url), "deepseek-flash").isEmpty())
        }
        assertTrue(defaultDeepSeekTools(request.provider, "deepseek-v4-pro").isEmpty())
        assertEquals("web_search", defaultDeepSeekTools(
            request.provider.copy(protocol = ProviderProtocol.OPENAI_RESPONSES), "deepseek-v4-pro"
        ).single().string("type"))
        val pro = defaultDeepSeekTools(request.provider.copy(protocol = ProviderProtocol.ANTHROPIC), "deepseek-v4-pro").single()
        assertEquals("web_search_20250305", pro.string("type"))
        assertEquals("web_search", pro.string("name"))
        assertFalse(pro.has("max_uses"))
    }

    @Test
    fun allProtocolsSendNativeToolsAlongsideCorrectlyFormattedClientTools() = runBlocking {
        val function = parseJson("""{"type":"function","function":{"name":"calculator","parameters":{"type":"object"}}}""").asJsonObject
        for (protocol in listOf(ProviderProtocol.OPENAI_CHAT, ProviderProtocol.OPENAI_RESPONSES, ProviderProtocol.ANTHROPIC)) {
            var payload = JsonObject()
            val http = OkHttpClient.Builder().addInterceptor { chain ->
                val buffer = Buffer()
                chain.request().body!!.writeTo(buffer)
                payload = parseJson(buffer.readUtf8()).asJsonObject
                val terminal = when (protocol) {
                    ProviderProtocol.OPENAI_CHAT -> """{"choices":[{"delta":{},"finish_reason":"stop"}]}"""
                    ProviderProtocol.OPENAI_RESPONSES -> """{"type":"response.completed","response":{"output":[],"usage":null}}"""
                    else -> """{"type":"message_stop"}"""
                }
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("OK").body("data: $terminal\n\n".toResponseBody()).build()
            }.build()
            val base = request(protocol)
            val client = if (protocol == ProviderProtocol.ANTHROPIC) AnthropicClient(http, gson) else OpenAiClient(http, gson)
            client.stream(base.copy(toolDefinitions = listOf(function) + defaultDeepSeekTools(base.provider, base.model.id))).toList()
            val tools = payload.getAsJsonArray("tools")
            assertEquals(3, tools.size())
            val local = tools[0].asJsonObject
            when (protocol) {
                ProviderProtocol.OPENAI_CHAT -> assertEquals("calculator", local.getAsJsonObject("function").string("name"))
                ProviderProtocol.OPENAI_RESPONSES -> {
                    assertEquals("calculator", local.string("name"))
                    assertFalse(local.has("function"))
                    assertTrue(local.has("parameters"))
                }
                else -> {
                    assertEquals("calculator", local.string("name"))
                    assertTrue(local.has("input_schema"))
                    assertFalse(local.has("type"))
                }
            }
            assertEquals("deepseek_web_search", tools[1].asJsonObject.string("type"))
            assertEquals("deepseek_read_url", tools[2].asJsonObject.string("type"))
            if (protocol == ProviderProtocol.ANTHROPIC) assertEquals("deepseek_read_url", tools[2].asJsonObject.string("name"))
        }
    }

    @Test
    fun chatRetainsServerSignatureAndOnlyFinalStepReasoning() {
        val state = DeepSeekStreamState(ProviderProtocol.OPENAI_CHAT)
        state.consume("""{"choices":[{"delta":{"reasoning_content":"searching"}}]}""")
        state.consume("""{"choices":[{"server_tool_event":{"type":"step_reasoning_done","step":0}}]}""")
        state.consume("""{"choices":[{"delta":{"reasoning_content":"answering"}}]}""")
        val final = """{"choices":[{"finish_reason":"stop","delta":{"server_tool_calls":[{"index":0,"reasoning_content":"searching","calls":[{"id":"srv1","type":"deepseek_read_url","kind":"read_link","signature":"signed-page"}]}],"annotations":[{"type":"url_citation","url_citation":{"url":"https://example.com/page","title":"来源"}}]}}]}"""
        assertTrue(openAi.parseChatChunk(final).none { it is StreamEvent.ToolDelta })
        val content = state.consume(final)!!.content
        assertTrue(state.finished)
        assertEquals("https://example.com/page", content.citations.single().url)
        val saved = gson.fromJson(gson.toJson(content), ProviderMessageContent::class.java)
        val message = ChatMessage(conversationId = "chat", role = MessageRole.ASSISTANT, content = "回答",
            reasoning = "searchinganswering", providerId = "deepseek", providerContent = saved)
        val replay = openAi.chatMessages(request(ProviderProtocol.OPENAI_CHAT).copy(messages = listOf(message))).single().asJsonObject
        assertEquals("answering", replay.string("reasoning_content"))
        assertEquals("signed-page", replay.getAsJsonArray("server_tool_calls")[0].asJsonObject
            .getAsJsonArray("calls")[0].asJsonObject.string("signature"))
        assertEquals("回答", replay.string("content"))
    }

    @Test
    fun responsesReplayCompleteOutputAndExposeReasoningAndCitations() {
        val state = DeepSeekStreamState(ProviderProtocol.OPENAI_RESPONSES)
        val output = """[{"type":"web_search_call","id":"srv1","kind":"read_link","signature":"page","status":"completed"},{"type":"reasoning","content":[{"type":"reasoning_text","text":"thinking"}]},{"type":"message","role":"assistant","content":[{"type":"output_text","text":"回答","annotations":[{"type":"url_citation","url":"https://example.com/page","title":"Page"}]}]}]"""
        val content = state.consume("""{"type":"response.completed","response":{"output":$output}}""")!!.content
        val message = ChatMessage(conversationId = "chat", role = MessageRole.ASSISTANT, content = "回答",
            providerId = "deepseek", providerContent = content)
        val replay = openAi.responsesInput(request(ProviderProtocol.OPENAI_RESPONSES).copy(messages = listOf(message)))
        assertEquals(parseJson(output), replay)
        assertEquals("Page", content.citations.single().title)
        assertEquals(StreamEvent.ReasoningDelta("thinking"), openAi.parseResponsesChunk(
            """{"type":"response.reasoning_text.delta","delta":"thinking"}"""))
        assertNull(openAi.parseResponsesChunk("""{"type":"response.output_item.added","item":{"type":"web_search_call","id":"srv1"}}"""))
    }

    @Test
    fun anthropicKeepsSignedBlocksAndClientToolIds() {
        val state = DeepSeekStreamState(ProviderProtocol.ANTHROPIC)
        val client = AnthropicClient(OkHttpClient(), gson)
        val chunks = listOf(
            """{"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":""}}""",
            """{"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"think"}}""",
            """{"type":"content_block_delta","index":0,"delta":{"type":"signature_delta","signature":"signed-thinking"}}""",
            """{"type":"content_block_start","index":1,"content_block":{"type":"server_tool_use","id":"srv1","name":"deepseek_read_url","input":{"action":"read_link","url":"https://example.com"},"kind":"read_link"}}""",
            """{"type":"content_block_start","index":2,"content_block":{"type":"web_fetch_tool_result","tool_use_id":"srv1","kind":"read_link","signature":"signed-page","content":{}}}""",
            """{"type":"content_block_start","index":3,"content_block":{"type":"tool_use","id":"client1","name":"calculator","input":{}}}""",
            """{"type":"content_block_delta","index":3,"delta":{"type":"input_json_delta","partial_json":"{\"expression\":\"1+1\"}"}}""",
            """{"type":"content_block_stop","index":3}"""
        )
        chunks.forEach { state.consume(it) }
        assertNull(client.parseChunk(chunks[3]))
        val content = state.consume("""{"type":"message_stop"}""")!!.content
        val blocks = parseJson(content.json).asJsonArray
        assertEquals("signed-thinking", blocks[0].asJsonObject.string("signature"))
        assertEquals("signed-page", blocks[2].asJsonObject.string("signature"))
        assertEquals("1+1", blocks[3].asJsonObject.getAsJsonObject("input").string("expression"))
        val message = ChatMessage(conversationId = "chat", role = MessageRole.ASSISTANT, content = "",
            providerId = "deepseek", providerContent = content)
        assertEquals(blocks, client.messageBlocks(request(ProviderProtocol.ANTHROPIC).copy(messages = listOf(message)))
            .single().asJsonObject.getAsJsonArray("content"))
    }

    @Test
    fun anthropicGroupsParallelToolResultsWithProviderIdsBeforeContinuing() {
        val calls = listOf(
            ToolCall("0", "calculator", "{}", providerCallId = "call-one", result = "2"),
            ToolCall("1", "current_time", "{}", providerCallId = "call-two", result = "time")
        )
        val messages = listOf(ChatMessage(conversationId = "chat", role = MessageRole.ASSISTANT,
            content = "", toolCalls = calls)) + calls.map { call ->
            ChatMessage(conversationId = "chat", role = MessageRole.TOOL,
                content = call.result!!, toolCalls = listOf(call))
        }
        val blocks = AnthropicClient(OkHttpClient(), gson).messageBlocks(
            request(ProviderProtocol.ANTHROPIC).copy(messages = messages)
        )
        assertEquals(2, blocks.size())
        val results = blocks[1].asJsonObject.getAsJsonArray("content")
        assertEquals(listOf("call-one", "call-two"), results.map { it.asJsonObject.string("tool_use_id") })
        assertTrue(results.all { it.asJsonObject.string("type") == "tool_result" })
    }

    @Test
    fun serverHistoryDoesNotLeakAcrossProvidersOrProtocols() {
        val content = ProviderMessageContent(ProviderProtocol.OPENAI_CHAT.name, """{"server_tool_calls":[]}""")
        val message = ChatMessage(conversationId = "chat", role = MessageRole.ASSISTANT, content = "reply",
            providerId = "another-provider", providerContent = content)
        assertNull(request(ProviderProtocol.OPENAI_CHAT).savedProviderContent(message))
        assertNull(request(ProviderProtocol.OPENAI_RESPONSES).savedProviderContent(message.copy(providerId = "deepseek")))
    }

    @Test
    fun streamingFailuresAreReportedInsteadOfCompletingSuccessfully() {
        assertThrows(IllegalStateException::class.java) {
            openAi.parseChatChunk("""{"choices":[],"error":{"message":"failed"}}""")
        }
        assertThrows(IllegalStateException::class.java) {
            openAi.parseResponsesChunk("""{"type":"response.failed","response":{"error":{"message":"failed"}}}""")
        }
        assertThrows(IllegalStateException::class.java) {
            AnthropicClient(OkHttpClient(), gson).parseChunk("""{"type":"error","error":{"message":"failed"}}""")
        }
    }
}

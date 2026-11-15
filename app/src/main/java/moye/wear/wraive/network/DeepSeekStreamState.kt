package moye.wear.wraive.network

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import moye.wear.wraive.model.MessageCitation
import moye.wear.wraive.model.ProviderMessageContent
import moye.wear.wraive.model.ProviderProtocol
import moye.wear.wraive.model.StreamEvent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Retains protocol-native history, including encrypted URL content and thinking signatures. */
internal class DeepSeekStreamState(private val protocol: ProviderProtocol) {
    private var stepReasoning = ""
    private val blocks = sortedMapOf<Int, JsonObject>()
    private val inputs = mutableMapOf<Int, StringBuilder>()
    var finished: Boolean = false
        private set

    fun consume(data: String): StreamEvent.ProviderContent? {
        val root = parseJson(data).objectOrNull() ?: return null
        val content = when (protocol) {
            ProviderProtocol.OPENAI_CHAT -> chatContent(root)
            ProviderProtocol.OPENAI_RESPONSES -> responseContent(root)
            ProviderProtocol.ANTHROPIC -> anthropicContent(root)
            else -> null
        } ?: return null
        return StreamEvent.ProviderContent(
            ProviderMessageContent(protocol.name, content.toString(), citations(content))
        )
    }

    private fun chatContent(root: JsonObject): JsonObject? {
        val choice = root.arrayOrNull("choices")?.firstOrNull()?.objectOrNull() ?: return null
        if (choice.string("finish_reason") != null) finished = true
        val delta = choice.objectOrNull("delta")
        stepReasoning += delta?.string("reasoning_content").orEmpty()
        if (choice.objectOrNull("server_tool_event")?.string("type") == "step_reasoning_done") {
            stepReasoning = ""
        }
        if (delta?.arrayOrNull("server_tool_calls") == null && delta?.arrayOrNull("annotations") == null) {
            return null
        }
        return JsonObject().apply {
            delta.arrayOrNull("server_tool_calls")?.let { add("server_tool_calls", it) }
            delta.arrayOrNull("annotations")?.let { add("annotations", it) }
            addProperty("reasoning_content", stepReasoning)
        }
    }

    private fun responseContent(root: JsonObject): JsonArray? {
        when (root.string("type")) {
            "response.output_item.done" -> root.objectOrNull("item")?.let {
                blocks[root.int("output_index")] = it
            }
            "response.completed", "response.incomplete" -> {
                finished = true
                return root.objectOrNull("response")?.arrayOrNull("output")
                    ?: JsonArray().apply { blocks.values.forEach(::add) }
            }
        }
        return null
    }

    private fun anthropicContent(root: JsonObject): JsonArray? {
        val index = root.int("index")
        when (root.string("type")) {
            "content_block_start" -> root.objectOrNull("content_block")?.let { blocks[index] = it }
            "content_block_delta" -> {
                val block = blocks[index] ?: return null
                val delta = root.objectOrNull("delta") ?: return null
                when (delta.string("type")) {
                    "text_delta" -> block.append("text", delta.string("text"))
                    "thinking_delta" -> block.append("thinking", delta.string("thinking"))
                    "signature_delta" -> block.append("signature", delta.string("signature"))
                    "input_json_delta" -> inputs.getOrPut(index, ::StringBuilder)
                        .append(delta.string("partial_json").orEmpty())
                    "citations_delta" -> delta.objectOrNull("citation")?.let { citation ->
                        val citations = block.arrayOrNull("citations") ?: JsonArray().also {
                            block.add("citations", it)
                        }
                        citations.add(citation)
                    }
                }
            }
            "content_block_stop" -> inputs.remove(index)?.let {
                blocks[index]?.add("input", parseJson(it.toString()))
            }
            "message_stop" -> {
                finished = true
                return JsonArray().apply { blocks.values.forEach(::add) }
            }
        }
        return null
    }

    private fun JsonObject.append(name: String, value: String?) {
        addProperty(name, string(name).orEmpty() + value.orEmpty())
    }

    private fun citations(content: JsonElement): List<MessageCitation> {
        val found = linkedMapOf<String, MessageCitation>()
        fun visit(element: JsonElement) {
            when {
                element.isJsonArray -> element.asJsonArray.forEach(::visit)
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { (name, value) ->
                    if ((name == "annotations" || name == "citations") && value.isJsonArray) {
                        value.asJsonArray.forEach { entry ->
                            val citation = entry.objectOrNull() ?: return@forEach
                            val location = citation.objectOrNull("url_citation") ?: citation
                            val url = location.string("url")?.toHttpUrlOrNull()?.toString()
                                ?: return@forEach
                            found[url] = MessageCitation(url, location.string("title").orEmpty().ifBlank { url })
                        }
                    } else visit(value)
                }
            }
        }
        visit(content)
        return found.values.toList()
    }
}

package moye.wear.wraive.chat

import moye.wear.wraive.model.AssistantProfile
import moye.wear.wraive.model.ChatMessage
import moye.wear.wraive.model.MemoryEntry
import moye.wear.wraive.model.MessageRole
import moye.wear.wraive.model.ModelConfig
import moye.wear.wraive.model.PromptTransform
import moye.wear.wraive.model.WorldBookEntry
import java.text.DateFormat
import java.util.Date

class PromptComposer {
    fun systemPrompt(
        assistant: AssistantProfile,
        model: ModelConfig,
        conversationSummary: String
    ): String = buildString {
        append(assistant.systemPrompt
            .replace("{{model}}", model.displayName)
            .replace("{{model_id}}", model.id)
            .replace("{{assistant}}", assistant.name))
        if (clockVariables.any { it in assistant.systemPrompt }) {
            append("\n\nUse the current turn’s clock values for the time placeholders above.")
        }
        if (conversationSummary.isNotBlank()) {
            append("\n\n# Earlier conversation summary\n$conversationSummary")
        }
    }.trim()

    fun turnContext(
        assistant: AssistantProfile,
        memories: List<MemoryEntry>,
        worldBookEntries: List<WorldBookEntry>,
        history: List<ChatMessage>
    ): String {
        val recentText = history.takeLast(12).joinToString("\n") { it.content }.lowercase()
        val triggeredWorldBook = worldBookEntries
            .filter { entry ->
                entry.enabled &&
                    entry.bookId in assistant.worldBookIds &&
                    entry.keywords.any { keyword -> keyword.lowercase() in recentText }
            }
            .sortedByDescending(WorldBookEntry::priority)
        return buildString {
            val turn = history.lastOrNull { it.role == MessageRole.USER }
            if (turn != null) {
                val date = Date(turn.createdAt)
                for (variable in clockVariables.filter { it in assistant.systemPrompt }) {
                    val format = when (variable) {
                        "{{time}}" -> DateFormat.getTimeInstance()
                        "{{date}}" -> DateFormat.getDateInstance()
                        else -> DateFormat.getDateTimeInstance()
                    }
                    appendLine("$variable = ${format.format(date)}")
                }
            }
            if (assistant.memoryEnabled) {
                val selectedMemories = memories.filter {
                    it.enabled && (it.assistantId == null || it.assistantId == assistant.id)
                }
                if (selectedMemories.isNotEmpty()) {
                    append("\n\n# Long-term memory\n")
                    selectedMemories.forEach { append("- ${it.effectiveCategory.name.lowercase()}: ${it.content}\n") }
                }
            }
            if (triggeredWorldBook.isNotEmpty()) {
                append("\n\n# Relevant world book entries\n")
                triggeredWorldBook.forEach { append("## ${it.title}\n${it.content}\n") }
            }
        }.trim()
    }

    fun transformHistory(
        history: List<ChatMessage>,
        transforms: List<PromptTransform>
    ): List<ChatMessage> = history.map { message ->
        val applicable = transforms.filter { transform ->
            transform.enabled && when (message.role) {
                MessageRole.USER -> transform.applyToUser
                MessageRole.ASSISTANT -> transform.applyToAssistant
                else -> false
            }
        }
        val transformed = applicable.fold(message.content) { content, transform ->
            runCatching {
                Regex(transform.pattern).replace(content, transform.replacement)
            }.getOrElse { content }
        }
        message.copy(
            content = transformed,
            providerContent = message.providerContent.takeIf { transformed == message.content }
        )
    }

    fun withTurnContext(history: List<ChatMessage>, context: String): List<ChatMessage> {
        if (context.isBlank()) return history
        val index = history.indexOfLast { it.role == MessageRole.USER }
        if (index < 0) return history
        val message = history[index]
        return history.toMutableList().apply {
            this[index] = message.copy(
                content = "$context\n\n# User message\n${message.content}",
                providerContent = null
            )
        }
    }

    private val clockVariables = listOf("{{date}}", "{{time}}", "{{datetime}}")
}

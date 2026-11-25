package moye.wear.wraive.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class SpeechEngine { SYSTEM, OPENAI, ELEVENLABS, GOOGLE }

enum class TranscriptionEngine { SYSTEM, OPENAI, GOOGLE }

data class SpeechConfig(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = DEFAULT_ID,
    @SerializedName(value = "engine", alternate = ["b"])
    val engine: SpeechEngine = SpeechEngine.SYSTEM,
    @SerializedName(value = "name", alternate = ["c"])
    val name: String = "系统语音",
    @SerializedName(value = "endpoint", alternate = ["d"])
    val endpoint: String = "",
    @SerializedName(value = "apiKey", alternate = ["e"])
    val apiKey: String = "",
    @SerializedName(value = "model", alternate = ["f"])
    val model: String = "gpt-4o-mini-tts",
    @SerializedName(value = "voice", alternate = ["g"])
    val voice: String = "alloy",
    @SerializedName(value = "languageCode", alternate = ["h"])
    val languageCode: String = "zh-CN",
    @SerializedName(value = "speed", alternate = ["i"])
    val speed: Float = 1f
) {
    companion object {
        const val DEFAULT_ID = "default"
    }
}

data class TranscriptionConfig(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = DEFAULT_ID,
    @SerializedName(value = "engine", alternate = ["b"])
    val engine: TranscriptionEngine = TranscriptionEngine.SYSTEM,
    @SerializedName(value = "endpoint", alternate = ["c"])
    val endpoint: String = "",
    @SerializedName(value = "apiKey", alternate = ["d"])
    val apiKey: String = "",
    @SerializedName(value = "model", alternate = ["e"])
    val model: String = "gpt-4o-mini-transcribe",
    @SerializedName(value = "languageCode", alternate = ["f"])
    val languageCode: String = "zh-CN"
) {
    companion object {
        const val DEFAULT_ID = "default"
    }
}

data class CloudBackupConfig(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = DEFAULT_ID,
    @SerializedName(value = "endpoint", alternate = ["b"])
    val endpoint: String = "",
    @SerializedName(value = "region", alternate = ["c"])
    val region: String = "us-east-1",
    @SerializedName(value = "bucket", alternate = ["d"])
    val bucket: String = "",
    @SerializedName(value = "objectKey", alternate = ["e"])
    val objectKey: String = "wraive/backup.wraive",
    @SerializedName(value = "accessKeyId", alternate = ["f"])
    val accessKeyId: String = "",
    @SerializedName(value = "secretAccessKey", alternate = ["g"])
    val secretAccessKey: String = "",
    @SerializedName(value = "encryptionPassword", alternate = ["h"])
    val encryptionPassword: String = "",
    @SerializedName(value = "pathStyle", alternate = ["i"])
    val pathStyle: Boolean = true,
    @SerializedName(value = "enabled", alternate = ["j"])
    val enabled: Boolean = false,
    @SerializedName(value = "lastSyncedAt", alternate = ["k"])
    val lastSyncedAt: Long? = null
) {
    companion object {
        const val DEFAULT_ID = "default"
    }
}

enum class ProxyType { HTTP, SOCKS }

data class NetworkProxyConfig(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = DEFAULT_ID,
    @SerializedName(value = "enabled", alternate = ["b"])
    val enabled: Boolean = false,
    @SerializedName(value = "type", alternate = ["c"])
    val type: ProxyType = ProxyType.HTTP,
    @SerializedName(value = "host", alternate = ["d"])
    val host: String = "",
    @SerializedName(value = "port", alternate = ["e"])
    val port: Int = 8080,
    @SerializedName(value = "username", alternate = ["f"])
    val username: String = "",
    @SerializedName(value = "password", alternate = ["g"])
    val password: String = "",
    @SerializedName(value = "bypassHosts", alternate = ["h"])
    val bypassHosts: List<String> = listOf("localhost", "127.0.0.1")
) {
    companion object {
        const val DEFAULT_ID = "default"
    }
}

enum class RequestStatus { RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class RequestLog(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "conversationId", alternate = ["b"])
    val conversationId: String,
    @SerializedName(value = "providerId", alternate = ["c"])
    val providerId: String,
    @SerializedName(value = "modelId", alternate = ["d"])
    val modelId: String,
    @SerializedName(value = "startedAt", alternate = ["e"])
    val startedAt: Long = System.currentTimeMillis(),
    @SerializedName(value = "finishedAt", alternate = ["f"])
    val finishedAt: Long? = null,
    @SerializedName(value = "status", alternate = ["g"])
    val status: RequestStatus = RequestStatus.RUNNING,
    @SerializedName(value = "usage", alternate = ["h"])
    val usage: TokenUsage = TokenUsage(),
    @SerializedName(value = "error", alternate = ["i"])
    val error: String? = null,
    @SerializedName(value = "responsePreview", alternate = ["j"])
    val responsePreview: String = ""
)

data class MessageSearchHit(
    val conversation: Conversation,
    val message: ChatMessage
)

data class UsageSummary(
    val conversations: Int,
    val messages: Int,
    val requests: Int,
    val inputTokens: Long,
    val outputTokens: Long,
    val cachedTokens: Long,
    val failures: Int,
    val activeDays: Int,
    val daily: List<DailyUsage>
) {
    val totalTokens: Long get() = inputTokens + outputTokens
}

data class DailyUsage(
    val dayStart: Long,
    val requests: Int,
    val tokens: Long
)

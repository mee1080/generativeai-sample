@file:Suppress("PropertyName", "LocalVariableName", "unused")

package io.github.mee1080.generativeai.api.gemini

import io.github.mee1080.generativeai.api.ApiBase
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val LOGGING = false

private const val URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

enum class GeminiModel(val modelName: String) {
    GeminiPro15("gemini-1.5-pro-latest"),
    GeminiPro("gemini-pro"),
    GeminiProVision("gemini-pro-vision");
}

private fun getUrl(model: GeminiModel, method: String, apiKey: String) =
    "$URL_BASE/${model.modelName}:${method}?key=$apiKey"

sealed class GeminiConversationBase(
    private val model: GeminiModel,
    private val apiKey: String,
) : ApiBase(LOGGING) {

    open val functions: List<Pair<FunctionDeclaration, (Map<String, String>) -> (Map<String, JsonElement>)>> =
        emptyList()

    val safetySettings = mutableMapOf<HarmCategory, HarmBlockThreshold>()

    var config = GenerationConfig()

    protected suspend fun generateContent(contents: List<Content>): Content? {
        val functions = if (functions.isEmpty()) emptyList() else listOf(Tool(functions.map { it.first }))
        val safetySettings = safetySettings.toList().map { SafetySetting(it.first, it.second) }
        val config = config
        return request<_, GenerateContentResponse>(
            getUrl(model, "generateContent", apiKey),
            GenerateContentRequest(contents, functions, safetySettings, config),
        ).candidates.first().content
    }

    protected suspend fun countTokens(contents: List<Content>): Int {
        return request<_, CountTokensResponse>(
            getUrl(model, "countTokens", apiKey),
            CountTokensRequest(contents),
        ).totalTokens
    }
}

class GeminiTextConversation(
    apiKey: String,
    model: GeminiModel = GeminiModel.GeminiPro,
) : GeminiConversationBase(model, apiKey) {

    private val _history: MutableList<Content> = mutableListOf()
    val history get() = _history as List<Content>

    override val functions =
        mutableListOf<Pair<FunctionDeclaration, (Map<String, String>) -> (Map<String, JsonElement>)>>()

    suspend fun send(text: String): String {
        val content = Content(listOf(Part(text)), "user")
        _history.add(content)
        for (i in 0..10) {
            val responseContent = generateContent(history) ?: throw RuntimeException("Failed to generate content")
            _history.add(responseContent)
            val functionCall = responseContent.parts.first().functionCall ?: break
            val functionAction = functions.firstOrNull { it.first.name == functionCall.name }?.second
                ?: throw RuntimeException("No function ${functionCall.name}")
            val functionResponse = FunctionResponse(functionCall.name, functionAction(functionCall.args))
            val functionContent = Content(listOf(Part(functionResponse = functionResponse)), "function")
            _history.add(functionContent)
        }
        return _history.last().parts.first().text ?: ""
    }

    suspend fun countTokens(text: String): Int {
        return countTokens(history + listOf(Content(listOf(Part(text)), "user")))
    }
}

class GeminiImageConversation(
    apiKey: String,
) : GeminiConversationBase(GeminiModel.GeminiProVision, apiKey) {

    @OptIn(ExperimentalEncodingApi::class)
    private fun toContent(text: String, image: ByteArray): Content {
        val parts = buildList {
            add(Part(text))
            val imageBase64 = Base64.encode(image)
            add(Part(inline_data = Blob("image/png", imageBase64)))
        }
        return Content(parts, "user")
    }

    suspend fun send(text: String, image: ByteArray): String {
        val content = toContent(text, image)
        val response = generateContent(listOf(content)) ?: throw RuntimeException("Failed to generate content")
        return response.parts.first().text ?: ""
    }

    suspend fun countTokens(text: String, image: ByteArray): Int {
        return countTokens(listOf(toContent(text, image)))
    }
}

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val tools: List<Tool> = emptyList(),
    val safetySettings: List<SafetySetting> = emptyList(),
    val generationConfig: GenerationConfig = GenerationConfig(),
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String,
)

@Serializable
data class Part(
    val text: String? = null,
    val inline_data: Blob? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null,
)

@Serializable
data class FunctionCall(
    val name: String,
    val args: Map<String, String> = emptyMap(),
)

@Serializable
data class FunctionResponse(
    val name: String,
    val response: Map<String, JsonElement>,
)

@Serializable
data class Blob(
    val mime_type: String,
    val data: String,
)

@Serializable
data class Tool(
    val functionDeclarations: List<FunctionDeclaration> = emptyList(),
)

@Serializable
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Schema? = null,
)

@Serializable
data class Schema(
    val type: Type,
    val format: String = "",
    val description: String = "",
    val nullable: Boolean = false,
    val enum: List<String> = emptyList(),
    val properties: Map<String, Schema> = emptyMap(),
    val required: List<String> = emptyList(),
    val items: Schema? = null,
)

enum class Type {
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    ARRAY,
    OBJECT,
}

@Serializable
data class SafetySetting(
    val category: HarmCategory,
    val threshold: HarmBlockThreshold,
)

enum class HarmCategory {
    HARM_CATEGORY_HARASSMENT,
    HARM_CATEGORY_HATE_SPEECH,
    HARM_CATEGORY_SEXUALLY_EXPLICIT,
    HARM_CATEGORY_DANGEROUS_CONTENT,
}

enum class HarmBlockThreshold {
    HARM_BLOCK_THRESHOLD_UNSPECIFIED,
    BLOCK_LOW_AND_ABOVE,
    BLOCK_MEDIUM_AND_ABOVE,
    BLOCK_ONLY_HIGH,
    BLOCK_NONE,
}

@Serializable
data class GenerationConfig(
    val stopSequences: List<String> = emptyList(),
    val temperature: Double = -1.0,
    val maxOutputTokens: Int = -1,
    val topP: Double = -1.0,
    val topK: Int = -1,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>,
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class Candidate(
    val content: Content?,
    val finishReason: String,
    val index: Int,
    val safetyRatings: List<SafetyRating>,
)

@Serializable
data class SafetyRating(
    val category: HarmCategory,
    val probability: String,
)

@Serializable
data class PromptFeedback(
    val safetyRatings: List<SafetyRating>,
)

@Serializable
data class CountTokensRequest(
    val contents: List<Content>,
)

@Serializable
data class CountTokensResponse(
    val totalTokens: Int,
)

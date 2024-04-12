@file:Suppress("PropertyName")

package io.github.mee1080.generativeai.api.openrouter

import io.github.mee1080.generativeai.api.ApiBase
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private const val LOGGING = true

private const val URL = "https://openrouter.ai/api/v1/chat/completions"

class OpenRouterConversation(
    private val apiKey: String,
    private val model: String,
) : ApiBase(LOGGING) {

    override fun HttpClientConfig<CIOEngineConfig>.configureClient() {
        defaultRequest {
            header("Authorization", "Bearer $apiKey")
        }
    }

    suspend fun send(message: String): String {
        return request<_, Response>(
            URL,
            Request(model, prompt = message)
        ).choices.joinToString("\n=====\n") { it.textContent ?: "" }
    }
}

@Serializable
data class Request(
    val model: String,
    val messages: List<RequestMessage>? = null,
    val prompt: String? = null,
    val response_format: ResponseFormat? = null,
    val stop: List<String>? = null,
    val stream: Boolean? = null,
    val max_tokens: Int? = null,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val top_k: Double? = null,
    val frequency_penalty: Double? = null,
    val presence_penalty: Double? = null,
    val repetition_penalty: Double? = null,
    val seed: Int? = null,
    val tools: List<Tool>? = null,
    val tool_choice: JsonElement? = null,
    val logit_bias: Map<Int, Int>? = null,
)

sealed interface ContentPart

@Serializable
data class TextContent(
    val text: String,
    val type: String = "text",
) : ContentPart

@Serializable
data class ImageContentPart(
    val image_url: ImageUrl,
    val type: String = "image_url",
) : ContentPart

@Serializable
data class ImageUrl(
    val url: String,
    val detail: String? = null,
)

@Serializable
data class RequestMessage(
    val role: String,
    val content: List<ContentPart>? = null,
    val name: String?,
)

@Serializable
data class ResponseFormat(
    val type: String,
)

@Serializable
data class FunctionDescription(
    val description: String,
    val name: String,
    val parameters: JsonElement,
)

@Serializable
data class Tool(
    val function: FunctionDescription,
    val type: String = "function",
)

@Serializable
data class Response(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val `object`: String,
    val usage: Usage?,
)

@Serializable
data class Choice(
    val finish_reason: String?,
    val text: String? = null,
    private val message: JsonElement? = null,
    val delta: ResponseMessage? = null,
    val code: Int? = null,
) {
    val responseMessage: ResponseMessage? = (message as? JsonObject)?.let {
        Json.decodeFromString(it.toString())
    }

    val errorMessage: String? = (message as? JsonPrimitive)?.content

    val textContent = text ?: responseMessage?.content ?: delta?.content ?: errorMessage
}

@Serializable
data class ResponseMessage(
    val content: String?,
    val role: String?,
    val tool_calls: List<ToolCall>?,
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
data class ToolCall(
    val id: String,
    val function: FunctionCall,
    val type: String = "function",
)

@Serializable
data class Usage(
    val completion_tokens: Int,
    val prompt_tokens: Int,
    val total_tokens: Int,
    val total_cost: Int,
)

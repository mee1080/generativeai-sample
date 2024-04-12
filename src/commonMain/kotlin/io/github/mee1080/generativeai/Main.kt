package io.github.mee1080.generativeai

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.PlatformImage
import dev.shreyaspatil.ai.client.generativeai.type.asTextOrNull
import dev.shreyaspatil.ai.client.generativeai.type.content
import io.github.mee1080.generativeai.api.gemini.*
import io.github.mee1080.generativeai.api.openrouter.OpenRouterConversation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.system.measureTimeMillis

private val GEMINI_KEY = BuildKonfig.GEMINI_API_KEY

private val OPEN_ROUTER_KEY = BuildKonfig.OPEN_ROUTER_KEY

fun main() {
    runBlocking {
        val time = measureTimeMillis {
//        openRouterTextConversation()
            textConversation1()
//            textConversationLib()
//        imageConversation1()
//            imageConversationLib()
//        functionConversation1()
//        conversationEachOther()
        }
        println()
        println("time: $time ms")
    }
}

suspend fun openRouterTextConversation() {
    OpenRouterConversation(OPEN_ROUTER_KEY, "google/gemma-7b-it:free").use { conversation ->
        val text1 = "compare ChatGPT and Gemini"
        println(conversation.send(text1))
    }
}

suspend fun textConversation1() {
    GeminiTextConversation(GEMINI_KEY, GeminiModel.GeminiPro15).use { conversation ->
        HarmCategory.entries.forEach { conversation.safetySettings[it] = HarmBlockThreshold.BLOCK_NONE }
        val text1 = "適当な論理パズルを作ってください。"
        println(conversation.countTokens(text1))
        conversation.send(text1)
        val text2 = "その論理パズルの解答を、ステップバイステップで教えてください。"
        println(conversation.countTokens(text2))
        conversation.send(text2)
        conversation.history.forEach { println(it.parts.first().text) }
    }
}

suspend fun textConversationLib() {
    val chat = GenerativeModel(GeminiModel.GeminiPro.modelName, GEMINI_KEY).startChat()
    val text1 = "適当な論理パズルを作ってください。"
    chat.sendMessage(text1)
    val text2 = "その論理パズルの解答を、ステップバイステップで教えてください。"
    chat.sendMessage(text2)
    chat.history.forEach { content ->
        println(content.role)
        println(content.parts.joinToString { it.asTextOrNull() ?: "" })
    }
}

suspend fun imageConversation1() {
    GeminiImageConversation(GEMINI_KEY).use { conversation ->
        val text = "このメッセージが出たら、一般的にはどうすればいいですか？"
        val image = Path.of("test.png").readBytes()
        println(conversation.countTokens(text, image))
        val response = conversation.send(text, image)
        println(response)
    }
}

suspend fun imageConversationLib() {
    val api = GenerativeModel(GeminiModel.GeminiProVision.modelName, GEMINI_KEY)
    val text = "このメッセージが出たら、一般的にはどうすればいいですか？"
    val image = Path.of("test.png").readBytes()
    val response = api.generateContent(content {
        image(PlatformImage(image))
        text(text)
    })
    println(response.text)
}

suspend fun functionConversation1() {
    GeminiTextConversation(GEMINI_KEY).use { conversation ->
        conversation.config = conversation.config.copy(
            temperature = 0.3,
        )
        conversation.functions += FunctionDeclaration(
            name = "getEvents",
            description = "イベントを取得する",
            parameters = Schema(
                type = Type.OBJECT,
                properties = mapOf(
                    "year" to Schema(
                        type = Type.INTEGER,
                        format = "int32",
                        description = "年（西暦）",
                    ),
                    "month" to Schema(
                        type = Type.INTEGER,
                        format = "int32",
                        description = "月",
                    ),
                    "day" to Schema(
                        type = Type.INTEGER,
                        format = "int32",
                        description = "日",
                    ),
                ),
                required = listOf("year", "month", "day"),
            ),
        ) to { _ ->
            mapOf(
                "events" to JsonArray(
                    listOf(
                        buildJsonObject {
                            put("name", "〇〇の会")
                            put("place", "東京")
                        },
                        buildJsonObject {
                            put("name", "合宿")
                            put("place", "札幌")
                        },
                        buildJsonObject {
                            put("name", "勉強会")
                            put("place", "京都")
                        },
                        buildJsonObject {
                            put("name", "会合")
                            put("place", "福岡")
                        },
                        buildJsonObject {
                            put("name", "セミナー")
                            put("place", "大阪")
                        },
                    )
                )
            )
        }
        val text = """
            提供しているfunctionを使用して、以下の質問に答えてください。

            質問###
            2024年3月25日に行われるイベントを教えてください
            ###
        """.trimIndent()
        val response = conversation.send(text)
        println(response)
    }
}

suspend fun conversationEachOther() {
    val context = "大規模システム開発において"
    val target1 = "Java"
    val target2 = "Python"
    val stepCount = 3
    GeminiTextConversation(GEMINI_KEY).use { conversation1 ->
        HarmCategory.entries.forEach { conversation1.safetySettings[it] = HarmBlockThreshold.BLOCK_NONE }
        GeminiTextConversation(GEMINI_KEY).use { conversation2 ->
            HarmCategory.entries.forEach { conversation2.safetySettings[it] = HarmBlockThreshold.BLOCK_NONE }
            val theme = "${context}「${target1}」と「${target2}」のどちらが優れているか、議論しましょう。\n\n"
            var message1 =
                conversation1.send(theme + "**重要:**\nあなたは「${target1}」が優れているという立場で発言してください。\n\nまずは「${target1}」が優れている理由を説明してください。")
            var message2 =
                conversation2.send(theme + "**重要:**\nあなたは「${target2}」が優れているという立場で発言してください。\n\nまずは「${target1}」が優れているという以下の主張に反論してください。\n\n相手の主張:###${message1}###")
            repeat(stepCount) {
                println("step ${it + 1}")
                message1 = conversation1.send(message2)
                message2 = conversation2.send(message1)
            }
            conversation2.history.forEach {
                println(it.parts.first().text)
                println("================")
            }
        }
    }
}
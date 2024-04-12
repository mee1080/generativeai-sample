package io.github.mee1080.generativeai.api

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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

abstract class ApiBase(logging: Boolean = false) : AutoCloseable {

    @OptIn(ExperimentalSerializationApi::class)
    protected val client = HttpClient(CIO) {
        if (logging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = Long.MAX_VALUE
        }
        install(ContentNegotiation) {
            json(Json {
                explicitNulls = false
                isLenient = true
            })
        }
        configureClient()
    }

    protected open fun HttpClientConfig<CIOEngineConfig>.configureClient() {
    }

    override fun close() {
        client.close()
    }

    protected suspend inline fun <reified Request, reified Response> request(url: String, request: Request): Response {
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody<Request>(request)
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Request Failed: ${response.status}\nrequest: ${Json.encodeToString(request)}\nresponse: ${response.bodyAsText()}")
        }
        return response.body()
    }
}
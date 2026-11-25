// PATTERN: KOI-R-0001
// TITLE: tddworks/openai-kotlin / gemini-client:gemini-client-core / DefaultTextGenerationApi
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true
// ORIGIN: real:tddworks/openai-kotlin@d02d678103d2/gemini-client/gemini-client-core/src/commonMain/kotlin/com/tddworks/gemini/api/textGeneration/api/internal/DefaultTextGenerationApi.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: gemini-client-core/commonMain/com/tddworks/gemini/api/textGeneration/api/internal/DefaultTextGenerationApi.kt
package com.tddworks.gemini.api.textGeneration.api.internal

import com.tddworks.common.network.api.ktor.api.HttpRequester
import com.tddworks.common.network.api.ktor.api.performRequest
import com.tddworks.common.network.api.ktor.api.streamRequest
import com.tddworks.gemini.api.textGeneration.api.GenerateContentRequest
import com.tddworks.gemini.api.textGeneration.api.GenerateContentResponse
import com.tddworks.gemini.api.textGeneration.api.TextGeneration
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class DefaultTextGenerationApi(private val requester: HttpRequester) : TextGeneration {
    override suspend fun generateContent(request: GenerateContentRequest): GenerateContentResponse {
        return requester.performRequest<GenerateContentResponse> { configureRequest(request) }
    }

    override fun streamGenerateContent(
        request: GenerateContentRequest
    ): Flow<GenerateContentResponse> {
        return requester.streamRequest<GenerateContentResponse> { configureRequest(request) }
    }

    private fun HttpRequestBuilder.configureRequest(request: GenerateContentRequest) {
        method = HttpMethod.Post
        url(path = request.toRequestUrl())
        if (request.stream) {
            parameter("alt", "sse")
        }
        setBody(request)
        contentType(ContentType.Application.Json)
    }

    companion object {
        const val GEMINI_API_PATH = "/v1beta/models"
    }
}

// PATTERN: KOI-R-0042
// TITLE: darriousliu/PiPixiv / common:ai / AiHttpClientHolder
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:darriousliu/PiPixiv@1a2e76353cc3/common/ai/src/commonMain/kotlin/com/mrl/pixiv/common/ai/internal/AiHttpClientHolder.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: ai/commonMain/com/mrl/pixiv/common/ai/internal/AiHttpClientHolder.kt
package com.mrl.pixiv.common.ai.internal

import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.network.httpEngineFactory
import com.mrl.pixiv.common.network.configureNetworkProxy
import com.mrl.pixiv.common.network.NetworkFeature
import com.mrl.pixiv.common.network.RoutedHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import org.koin.core.annotation.Single

internal const val AI_GENERATION_TIMEOUT_MILLIS =
    AiTranslationConfig.GENERATION_TIMEOUT_DEFAULT_SECONDS * 1_000L
internal const val AI_CONNECT_TIMEOUT_MILLIS = 60_000L

@Single
class AiHttpClientHolder(networkFeature: NetworkFeature) {
    private val clients = RoutedHttpClient(
        readSetting = { networkFeature.provideUserPreference().bypassSetting },
        create = { setting ->
            HttpClient(httpEngineFactory) {
                configureNetworkProxy(setting)
                configureAiHttpClient()
            }
        },
        close = HttpClient::close,
    )

    internal suspend fun <T> withClient(block: suspend (HttpClient) -> T): T =
        clients.withClient(block)
}

internal fun HttpClientConfig<*>.configureAiHttpClient() {
    followRedirects = false
    install(HttpTimeout) {
        requestTimeoutMillis = AI_GENERATION_TIMEOUT_MILLIS
        connectTimeoutMillis = AI_CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = AI_GENERATION_TIMEOUT_MILLIS
    }
    install(SSE)
}

internal fun HttpRequestBuilder.configureAiGenerationTimeout(
    generationTimeoutMillis: Long,
    streaming: Boolean = false,
) {
    timeout {
        requestTimeoutMillis = if (streaming) {
            HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        } else {
            generationTimeoutMillis
        }
        connectTimeoutMillis = AI_CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = generationTimeoutMillis
    }
}

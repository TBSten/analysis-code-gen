// PATTERN: KOI-R-0002
// TITLE: tddworks/openai-kotlin / gemini-client:gemini-client-core / GeminiModule
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true
// ORIGIN: real:tddworks/openai-kotlin@d02d678103d2/gemini-client/gemini-client-core/src/commonMain/kotlin/com/tddworks/gemini/di/GeminiModule.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: gemini-client-core/commonMain/com/tddworks/gemini/di/GeminiModule.kt
package com.tddworks.gemini.di

import com.tddworks.common.network.api.ktor.api.HttpRequester
import com.tddworks.common.network.api.ktor.internal.ClientFeatures
import com.tddworks.common.network.api.ktor.internal.UrlBasedConnectionConfig
import com.tddworks.common.network.api.ktor.internal.createHttpClient
import com.tddworks.common.network.api.ktor.internal.default
import com.tddworks.di.createJson
import com.tddworks.gemini.api.textGeneration.api.Gemini
import com.tddworks.gemini.api.textGeneration.api.GeminiConfig
import com.tddworks.gemini.api.textGeneration.api.TextGeneration
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import org.koin.dsl.module
import org.koin.ksp.generated.module

@Module
@ComponentScan("com.tddworks.gemini")
class GeminiModule {

    @Single
    fun httpRequester(@Provided config: GeminiConfig): HttpRequester {
        return HttpRequester.default(
            createHttpClient(
                connectionConfig = UrlBasedConnectionConfig(config.baseUrl),
                features =
                    ClientFeatures(
                        json = createJson(),
                        queryParams = { mapOf("key" to config.apiKey()) },
                    ),
            )
        )
    }

    @Single
    fun gemini(textGeneration: TextGeneration): Gemini {
        return object : Gemini, TextGeneration by textGeneration {}
    }

    companion object {
        fun geminiModules(config: GeminiConfig) = module {
            single { config }
            includes(GeminiModule().module)
        }
    }
}

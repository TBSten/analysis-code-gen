// PATTERN: MSH-R-0034
// TITLE: skydoves/chatgpt-android / core-model / GPTChatResponse
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:skydoves/chatgpt-android@0155011570e6/core-model/src/main/kotlin/com/skydoves/chatgpt/core/model/network/GPTChatResponse.kt
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので K（KSP2 2.3.14）: K でビルドが失敗した: GPTChatResponse.kt:31: Unresolved reference 'GPTChoice'.
// FILE: core-model/main/com/skydoves/chatgpt/core/model/network/GPTChatResponse.kt
/*
 * Designed and developed by 2024 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.chatgpt.core.model.network

import com.skydoves.chatgpt.core.model.GPTChoice
import com.skydoves.chatgpt.core.model.GPTUsage
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GPTChatResponse(
  @field:Json(name = "id") val id: String,
  @field:Json(name = "object") val `object`: String,
  @field:Json(name = "created") val created: Long,
  @field:Json(name = "model") val model: String,
  @field:Json(name = "system_fingerprint") val systemFingerprint: String?,
  @field:Json(name = "choices") val choices: List<GPTChoice>,
  @field:Json(name = "usage") val usage: GPTUsage
)

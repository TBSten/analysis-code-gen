// PATTERN: MSH-R-0021
// TITLE: cashapp/cash-app-pay-android-sdk / core / CustomerProfile
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:cashapp/cash-app-pay-android-sdk@a4979b3d3094/core/src/main/java/app/cash/paykit/core/models/response/CustomerProfile.kt
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので K（KSP2 2.3.14）: K でビルドが失敗した: CustomerProfile.kt:27: Unresolved reference 'PiiString'.
// FILE: core/main/app/cash/paykit/core/models/response/CustomerProfile.kt
/*
 * Copyright (C) 2023 Cash App
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paykit.core.models.response

import app.cash.paykit.core.models.pii.PiiString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomerProfile(
  @Json(name = "id")
  val id: String,
  @Json(name = "cashtag")
  val cashTag: PiiString,
)

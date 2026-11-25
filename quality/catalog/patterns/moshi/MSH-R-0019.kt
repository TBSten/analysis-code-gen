// PATTERN: MSH-R-0019
// TITLE: cashapp/cash-app-pay-android-sdk / core / CustomerTopLevelResponse
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:cashapp/cash-app-pay-android-sdk@a4979b3d3094/core/src/main/java/app/cash/paykit/core/models/response/CustomerTopLevelResponse.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: core/main/app/cash/paykit/core/models/response/CustomerTopLevelResponse.kt
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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CustomerTopLevelResponse(
  @Json(name = "request")
  val customerResponseData: CustomerResponseData,
)

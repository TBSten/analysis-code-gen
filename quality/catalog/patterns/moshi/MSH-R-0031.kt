// PATTERN: MSH-R-0031
// TITLE: ZacSweers/CatchUp / services:reddit / RedditResponse
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:ZacSweers/CatchUp@b0873380ad4a/services/reddit/src/main/kotlin/catchup/service/reddit/model/RedditResponse.kt
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので K（KSP2 2.3.14）: K でビルドが失敗した: RedditResponse.kt:20: Unresolved reference 'RedditListing'.
// FILE: reddit/main/catchup/service/reddit/model/RedditResponse.kt
/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.service.reddit.model

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep @JsonClass(generateAdapter = true) data class RedditResponse(val data: RedditListing)

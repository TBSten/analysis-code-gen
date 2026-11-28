// PATTERN: MSH-R-0033
// TITLE: ZacSweers/CatchUp / services:uplabs / UplabsModels
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:ZacSweers/CatchUp@b0873380ad4a/services/uplabs/src/main/kotlin/catchup/service/uplabs/model/UplabsModels.kt
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので
// FILE: uplabs/main/catchup/service/uplabs/model/UplabsModels.kt
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
package catchup.service.uplabs.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.time.Instant

@JsonClass(generateAdapter = true)
data class UplabsImage(
  val id: Long,
  @Json(name = "showcased_at") val showcasedAt: Instant,
  @Json(name = "comments_count") val commentsCount: Int,
  val url: String,
  val animated: Boolean,
  @Json(name = "animated_teaser_url") val animatedTeaserUrl: String,
  val name: String,
  @Json(name = "maker_name") val makerName: String,
  val points: Int,
  @Json(name = "description") val htmlDescription: String?,
  @Json(name = "description_without_html") val description: String? = htmlDescription,
  val label: String,
  @Json(name = "category_friendly_name") val category: String,
  @Json(name = "teaser_url") val teaserUrl: String,
  @Json(name = "preview_url") val previewUrl: String,
  val images: List<Image>,
)

@JsonClass(generateAdapter = true)
data class Image(
  @Json(name = "content_type") val contentType: String,
  val height: Int,
  val width: Int,
  val size: Long,
  val urls: Urls,
)

@JsonClass(generateAdapter = true) data class Urls(val full: String, val thumbnail: String)

@JsonClass(generateAdapter = true) data class UplabsComments(val comments: List<UplabsComment>)

@JsonClass(generateAdapter = true)
data class UplabsComment(
  @Json(name = "attachment_url") val attachmentUrl: String? = null,
  val body: String = "Great job ",
  @Json(name = "comment_likes_count") val commentLikesCount: Int = 0,
  @Json(name = "created_at") val createdAt: Instant,
  val deleted: Boolean = false,
  val id: Long = 354347,
  @Json(name = "likes_user_ids") val likesUserIds: List<Long> = emptyList(),
  //  val in_reply_to_id: String =	null,
  val persisted: Boolean = true,
  @Json(name = "commentable_type") val commentableType: String = "Post",
  @Json(name = "commentable_id") val commentableId: Long = 596426,
  val replies: List<UplabsComment> = emptyList(),
  val annotation: Boolean = false,
  @Json(name = "annotation_x") val annotationX: Int? = null,
  @Json(name = "annotation_y") val annotationY: Int? = null,
  @Json(name = "annotation_label") val annotationLabel: String? = null,
  @Json(name = "preview_url") val previewUrl: String? = null,
  val user: UplabsUser? = null,
)

@JsonClass(generateAdapter = true)
data class UplabsUser(
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "full_name") val fullName: String? = null,
  val nickname: String? = null,
)

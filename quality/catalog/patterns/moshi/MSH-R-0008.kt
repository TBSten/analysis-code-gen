// PATTERN: MSH-R-0008
// TITLE: simpledotorg/simple-android / app / UnknownComponent
// PROCESSORS: moshi
// EXPECT: ok
// ORIGIN: real:simpledotorg/simple-android@482bb8c9bcdd/app/src/main/java/org/simple/clinic/questionnaire/component/UnknownComponent.kt
// LICENSE: MIT
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: app/main/org/simple/clinic/questionnaire/component/UnknownComponent.kt
package org.simple.clinic.questionnaire.component

import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
class UnknownComponent : BaseComponentData()

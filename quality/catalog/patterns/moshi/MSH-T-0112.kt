// PATTERN: MSH-T-0112
// TITLE: 実行時テスト / DualKotlinTest / TextAssetMetaData
// PROCESSORS: moshi
// EXPECT: ok
// TAGS: moshi.shape=default-values, moshi.shape=inheritance, moshi.shape=nested-adapter
// ORIGIN: test:square/moshi@7f957b18ea12/kotlin/tests/src/test/kotlin/com/squareup/moshi/kotlin/DualKotlinTest.kt#TextAssetMetaData
// LICENSE: Apache-2.0
// STATUS: verified
// NOTE: テストクラスの入れ子から取り出したものは最上位に移している
// FILE: TextAssetMetaData.kt
package com.squareup.moshi.kotlin

import com.squareup.moshi.JsonClass

class TextAsset : Asset<TextAsset>()

abstract class Asset<A : Asset<A>>

typealias A = Int

abstract class AssetMetaData<A : Asset<A>>

@JsonClass(generateAdapter = true)
class TextAssetMetaData(val text: String) : AssetMetaData<TextAsset>()

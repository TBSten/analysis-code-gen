// PATTERN: CRM-R-0044
// TITLE: cream.kt の test モジュール / CrossPackageMapping
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyMapping/CrossPackageMapping.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyMapping/CrossPackageMapping.kt
package me.tbsten.cream.test.copyMapping

import me.tbsten.cream.CopyMapping
import me.tbsten.cream.test.copyMapping.lib.CrossSource
import me.tbsten.cream.test.copyMapping.lib.CrossTarget

/**
 * The `@CopyMapping` holder is declared here, in a different package than `CrossSource` /
 * `CrossTarget` (which live in `...copyMapping.lib`). issue #145: the generated
 * `CrossSource.copyToCrossTarget(...)` must be emitted into THIS (the holder's) package, so the
 * test can call it without importing it from the library package.
 */
@CopyMapping(CrossSource::class, CrossTarget::class)
private object CrossPackageMapping

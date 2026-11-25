// PATTERN: CRM-T-0131
// TITLE: CombineTo / sourceKind / valueClassSource
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sourceKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/00--sourceKind/valueClassSource.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CombineTo

@JvmInline
@CombineTo(Target::class)
public value class SourceA(
  public val `value`: String,
)

@CombineTo(Target::class)
public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val `value`: String,
  public val extra: Int,
)

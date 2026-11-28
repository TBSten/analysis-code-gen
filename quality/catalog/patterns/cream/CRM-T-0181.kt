// PATTERN: CRM-T-0181
// TITLE: CopyFrom / sourceKind / valueClassSource
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sourceKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/00--sourceKind/valueClassSource.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyFrom

@CopyFrom(Source::class)
public data class Target(
  public val `value`: String,
)

@JvmInline
public value class Source(
  public val `value`: String,
)

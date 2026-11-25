// PATTERN: CRM-T-0066
// TITLE: CombineFrom / kdoc / description
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/08--kdoc/description.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineFrom
import me.tbsten.cream.KDoc

@CombineFrom(
  SourceA::class,
  SourceB::class,
  kdoc = KDoc(description = "Custom description for the combine function."),
)
public data class Target(
  public val name: String,
  public val extra: Int,
)

public data class SourceA(
  public val name: String,
)

public data class SourceB(
  public val extra: Int,
)

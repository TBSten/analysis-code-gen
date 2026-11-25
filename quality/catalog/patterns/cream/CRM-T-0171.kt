// PATTERN: CRM-T-0171
// TITLE: CombineTo / funName / tokenFunName
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=funName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/10--funName/tokenFunName.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineTo
import me.tbsten.cream.CopyTargetSimpleName

@CombineTo(
  Target::class,
  funName = "to" + CopyTargetSimpleName,
)
public data class SourceA(
  public val name: String,
)

@CombineTo(
  Target::class,
  funName = "to" + CopyTargetSimpleName,
)
public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val name: String,
  public val extra: Int,
)

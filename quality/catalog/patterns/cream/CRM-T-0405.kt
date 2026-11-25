// PATTERN: CRM-T-0405
// TITLE: CopyTo / funName / tokenFunName
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=funName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--funName/tokenFunName.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyTargetSimpleName
import me.tbsten.cream.CopyTo

@CopyTo(
  Target::class,
  funName = "to" + CopyTargetSimpleName,
)
public data class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
)

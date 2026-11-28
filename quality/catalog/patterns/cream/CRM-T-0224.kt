// PATTERN: CRM-T-0224
// TITLE: CopyFrom / funName / tokenFunName
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=funName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--funName/tokenFunName.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyFrom
import me.tbsten.cream.CopyTargetSimpleName

@CopyFrom(
  Source::class,
  funName = "to" + CopyTargetSimpleName,
)
public data class Target(
  public val name: String,
)

public data class Source(
  public val name: String,
)

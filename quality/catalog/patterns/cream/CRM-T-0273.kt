// PATTERN: CRM-T-0273
// TITLE: CopyMapping / funName / tokenFunNameReversible
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=funName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--funName/tokenFunNameReversible.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyMapping
import me.tbsten.cream.CopyTargetSimpleName

@CopyMapping(
  Source::class,
  Target::class,
  canReverse = true,
  funName = "to" + CopyTargetSimpleName,
)
public object Mapping

public data class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
)

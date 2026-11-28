// PATTERN: CRM-T-0085
// TITLE: CombineMapping / targetKind / abstractTarget
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=targetKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/01--targetKind/abstractTarget.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineMapping

@CombineMapping(
  sources = [SourceA::class, SourceB::class],
  target = Target::class,
)
public object Mapping

public data class SourceA(
  public val name: String,
)

public data class SourceB(
  public val extra: Int,
)

public abstract class Target(
  public val name: String,
  public val extra: Int,
)

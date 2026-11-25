// PATTERN: CRM-T-0231
// TITLE: CopyMapping / sourceKind / sealedInterfaceSource
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sourceKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/00--sourceKind/sealedInterfaceSource.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Target::class,
)
public object Mapping

public sealed interface Source {
  public val name: String
}

public data class Target(
  public val name: String,
  public val extra: Int,
)

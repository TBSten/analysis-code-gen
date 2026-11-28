// PATTERN: CRM-T-0230
// TITLE: CopyMapping / sourceKind / plainClassSource
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sourceKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/00--sourceKind/plainClassSource.md
// LICENSE: Apache-2.0
// STATUS: verified
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

public class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
  public val extra: Int,
)

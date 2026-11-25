// PATTERN: CRM-T-0257
// TITLE: CopyMapping / map / mapOverridesNameMatch
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/07--map/mapOverridesNameMatch.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Target::class,
  properties = [CopyMapping.Map(source = "beta", target = "alpha")],
)
public object Mapping

public data class Source(
  public val alpha: String,
  public val beta: String,
)

public data class Target(
  public val alpha: String,
)

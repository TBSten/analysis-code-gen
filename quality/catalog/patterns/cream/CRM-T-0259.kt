// PATTERN: CRM-T-0259
// TITLE: CopyMapping / map / multipleMappings
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/07--map/multipleMappings.md
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
  properties = [CopyMapping.Map(source = "firstSource", target = "firstTarget"), CopyMapping.Map(source = "secondSource", target = "secondTarget")],
)
public object Mapping

public data class Source(
  public val firstSource: String,
  public val secondSource: Int,
)

public data class Target(
  public val firstTarget: String,
  public val secondTarget: Int,
)

// PATTERN: CRM-T-0275
// TITLE: CopyMapping / exclude / excludeMappedProperty
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/13--excludes/excludeMappedProperty.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Target::class,
  properties = [CopyMapping.Map(source = "sourceName", target = "targetName")],
  excludes = ["targetName"],
)
public object Mapping

public data class Source(
  public val sourceName: String,
  public val shared: String,
)

public data class Target(
  public val targetName: String,
  public val shared: String,
)

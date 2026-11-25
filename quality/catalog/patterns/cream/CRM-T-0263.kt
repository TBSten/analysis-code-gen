// PATTERN: CRM-T-0263
// TITLE: CopyMapping / kdoc / description
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/09--kdoc/description.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyMapping
import me.tbsten.cream.KDoc

@CopyMapping(
  Source::class,
  Target::class,
  kdoc = KDoc(description = "Custom description for the copy function."),
)
public object Mapping

public data class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
)

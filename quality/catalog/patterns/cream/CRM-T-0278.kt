// PATTERN: CRM-T-0278
// TITLE: CopyMapping / exclude / excludeSharedPropWithCanReverse
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/13--excludes/excludeSharedPropWithCanReverse.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Target::class,
  canReverse = true,
  excludes = ["shared"],
)
public object Mapping

public data class Source(
  public val shared: String,
  public val sourceOnly: Int,
)

public data class Target(
  public val shared: String,
  public val targetOnly: Boolean,
)

// PATTERN: CRM-T-0242
// TITLE: CopyMapping / nesting / targetNestedInOuter
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=nesting
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--nesting/targetNestedInOuter.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Outer.Target::class,
)
public object Mapping

public data class Source(
  public val name: String,
)

public class Outer {
  public data class Target(
    public val name: String,
    public val extra: Int,
  )
}

// PATTERN: CRM-T-0367
// TITLE: CopyTo / nesting / targetNestedTwoLevelsInSource
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=nesting
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--nesting/targetNestedTwoLevelsInSource.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyTo

@CopyTo(Source.Middle.Target::class)
public data class Source(
  public val name: String,
) {
  public class Middle {
    public data class Target(
      public val name: String,
      public val extra: Int,
    )
  }
}

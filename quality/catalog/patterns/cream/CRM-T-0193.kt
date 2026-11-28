// PATTERN: CRM-T-0193
// TITLE: CopyFrom / nesting / sourceNestedTwoLevelsInTarget
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=nesting
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--nesting/sourceNestedTwoLevelsInTarget.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyFrom

@CopyFrom(Target.Middle.Source::class)
public data class Target(
  public val name: String,
  public val extra: Int,
) {
  public class Middle {
    public data class Source(
      public val name: String,
    )
  }
}

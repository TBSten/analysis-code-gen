// PATTERN: CRM-T-0192
// TITLE: CopyFrom / nesting / sourceNestedTwoLevelsInNestedTarget
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=nesting
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--nesting/sourceNestedTwoLevelsInNestedTarget.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Parent.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyFrom

public class Parent {
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
}

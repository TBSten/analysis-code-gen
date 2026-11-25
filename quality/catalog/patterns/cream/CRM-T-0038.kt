// PATTERN: CRM-T-0038
// TITLE: CombineFrom / targetKind / innerTarget
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=targetKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/01--targetKind/innerTarget.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Outer.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CombineFrom

public class Outer {
  @CombineFrom(Source::class)
  public inner class Target(
    public val name: String,
  )
}

public data class Source(
  public val name: String,
)

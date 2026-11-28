// PATTERN: CRM-T-0173
// TITLE: CombineTo / deprecated / perSourcePrecedence
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=deprecated
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--deprecated/perSourcePrecedence.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.Deprecated
import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class SourceA(
  public val shared: String,
  @Deprecated("a-prop")
  public val legacy: Int,
)

@Deprecated("b-class")
@CombineTo(Target::class)
public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val shared: String,
  public val legacy: Int,
  public val extra: Int,
)

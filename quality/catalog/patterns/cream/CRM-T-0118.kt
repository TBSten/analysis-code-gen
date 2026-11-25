// PATTERN: CRM-T-0118
// TITLE: CombineMapping / repeatable / multipleAnnotations
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=repeatable
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/10--repeatable/multipleAnnotations.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineMapping

@CombineMapping(
  sources = [LibA::class, LibB::class],
  target = Target::class,
)
@CombineMapping(
  sources = [LibB::class, LibC::class],
  target = Target::class,
)
public object Mapping

public data class LibA(
  public val shared: String,
  public val aProp: Int,
)

public data class LibB(
  public val shared: String,
  public val bProp: Int,
)

public data class LibC(
  public val shared: String,
  public val cProp: Int,
)

public data class Target(
  public val shared: String,
  public val aProp: Int,
  public val bProp: Int,
)

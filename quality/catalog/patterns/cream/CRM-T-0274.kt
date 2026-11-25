// PATTERN: CRM-T-0274
// TITLE: CopyMapping / repeatable / multipleAnnotations
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=repeatable
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/12--repeatable/multipleAnnotations.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyMapping

@CopyMapping(
  LibA::class,
  LibB::class,
)
@CopyMapping(
  LibB::class,
  LibC::class,
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

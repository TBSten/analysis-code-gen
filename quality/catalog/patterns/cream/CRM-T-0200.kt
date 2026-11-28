// PATTERN: CRM-T-0200
// TITLE: CopyFrom / constructor / varargMatchedPrimitiveArray
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=constructor
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/04--constructor/varargMatchedPrimitiveArray.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.IntArray
import kotlin.String
import me.tbsten.cream.CopyFrom

@CopyFrom(Source::class)
public class Target(
  public val name: String,
  vararg values: Int,
)

public data class Source(
  public val name: String,
  public val values: IntArray,
)

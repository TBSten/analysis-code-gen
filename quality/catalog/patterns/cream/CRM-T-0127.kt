// PATTERN: CRM-T-0127
// TITLE: CombineTo / valueClassMapping / unwrapFromSecondarySource
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(autoValueClassMapping=true)/12--valueClassMapping/unwrapFromSecondarySource.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class SourceA(
  public val name: String,
)

@CombineTo(Target::class)
public data class SourceB(
  public val id: DomainId,
)

public data class Target(
  public val id: String,
  public val name: String,
)

@JvmInline
public value class DomainId(
  public val `value`: String,
)

// PATTERN: CRM-T-0125
// TITLE: CombineTo / valueClassMapping / mapRenamedWrap
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(autoValueClassMapping=true)/12--valueClassMapping/mapRenamedWrap.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class SourceA(
  @CombineTo.Map("id")
  public val rawId: String,
)

@CombineTo(Target::class)
public data class SourceB(
  public val name: String,
)

public data class Target(
  public val id: DomainId,
  public val name: String,
)

@JvmInline
public value class DomainId(
  public val `value`: String,
)

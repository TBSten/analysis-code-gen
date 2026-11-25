// PATTERN: CRM-T-0031
// TITLE: CombineFrom / valueClassMapping / mapRenamedWrap
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(autoValueClassMapping=true)/12--valueClassMapping/mapRenamedWrap.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CombineFrom

@CombineFrom(
  SourceA::class,
  SourceB::class,
)
public data class Target(
  @CombineFrom.Map("rawId")
  public val id: DomainId,
  public val name: String,
)

public data class SourceA(
  public val rawId: String,
)

public data class SourceB(
  public val name: String,
)

@JvmInline
public value class DomainId(
  public val `value`: String,
)

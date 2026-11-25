// PATTERN: CRM-T-0081
// TITLE: CombineMapping / valueClassMapping / wrapIntoValueClass
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(autoValueClassMapping=true)/13--valueClassMapping/wrapIntoValueClass.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CombineMapping

@CombineMapping(
  sources = [SourceA::class, SourceB::class],
  target = Target::class,
)
public object Mapping

public data class SourceA(
  public val id: String,
)

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

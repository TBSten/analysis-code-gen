// PATTERN: CRM-T-0080
// TITLE: CombineMapping / valueClassMapping / unwrapFromSecondarySource
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(autoValueClassMapping=true)/13--valueClassMapping/unwrapFromSecondarySource.md
// LICENSE: Apache-2.0
// STATUS: verified
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
  public val name: String,
)

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

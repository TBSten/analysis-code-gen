// PATTERN: CRM-T-0227
// TITLE: CopyMapping / valueClassMapping / propertiesRenamedWrap
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(autoValueClassMapping=true)/14--valueClassMapping/propertiesRenamedWrap.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Target::class,
  properties = [CopyMapping.Map(source = "rawId", target = "id")],
)
public object Mapping

public data class Source(
  public val rawId: String,
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

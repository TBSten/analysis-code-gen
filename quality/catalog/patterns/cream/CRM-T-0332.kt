// PATTERN: CRM-T-0332
// TITLE: CopyTo / valueClassMapping / excludeSuppressesConversion
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(autoValueClassMapping=true)/14--valueClassMapping/excludeSuppressesConversion.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source(
  @CopyTo.Exclude
  public val id: String,
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

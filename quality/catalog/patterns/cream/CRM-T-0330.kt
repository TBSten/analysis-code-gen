// PATTERN: CRM-T-0330
// TITLE: CopyTo / valueClassMapping / chainedWrapFromNullableSourceIntoNullableTarget
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(autoValueClassMapping=true)/14--valueClassMapping/chainedWrapFromNullableSourceIntoNullableTarget.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source(
  public val id: String?,
  public val name: String,
)

public data class Target(
  public val id: UserId?,
  public val name: String,
)

@JvmInline
public value class RawId(
  public val `value`: String,
)

@JvmInline
public value class UserId(
  public val rawId: RawId,
)

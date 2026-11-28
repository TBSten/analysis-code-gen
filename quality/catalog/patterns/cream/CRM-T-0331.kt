// PATTERN: CRM-T-0331
// TITLE: CopyTo / valueClassMapping / differentValueClassesMismatch
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(autoValueClassMapping=true)/14--valueClassMapping/differentValueClassesMismatch.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source(
  public val id: SourceId,
  public val name: String,
)

public data class Target(
  public val id: TargetId,
  public val name: String,
)

@JvmInline
public value class SourceId(
  public val `value`: String,
)

@JvmInline
public value class TargetId(
  public val `value`: String,
)

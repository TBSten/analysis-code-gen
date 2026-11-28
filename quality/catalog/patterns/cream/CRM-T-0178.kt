// PATTERN: CRM-T-0178
// TITLE: CopyFrom / valueClassMapping / wrapIntoValueClass
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(autoValueClassMapping=true)/12--valueClassMapping/wrapIntoValueClass.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyFrom

@CopyFrom(Source::class)
public data class Target(
  public val id: DomainId,
  public val name: String,
)

public data class Source(
  public val id: String,
  public val name: String,
)

@JvmInline
public value class DomainId(
  public val `value`: String,
)

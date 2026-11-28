// PATTERN: CRM-T-0345
// TITLE: CopyTo / valueClassMapping / unwrapWinsOverWrap
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(autoValueClassMapping=true)/14--valueClassMapping/unwrapWinsOverWrap.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Any
import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source(
  public val item: Holder,
  public val name: String,
)

public data class Target(
  public val item: Wrapper,
  public val name: String,
)

@JvmInline
public value class Wrapper(
  public val raw: Any,
)

@JvmInline
public value class Holder(
  public val wrapper: Wrapper,
)

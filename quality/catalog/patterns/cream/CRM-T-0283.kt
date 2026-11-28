// PATTERN: CRM-T-0283
// TITLE: CopyToChildren / valueClassMapping / underlyingTypeMismatch
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(autoValueClassMapping=true)/10--valueClassMapping/underlyingTypeMismatch.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: State.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface State {
  public val id: Int

  public class Loaded(
    id: DomainId,
  ) : State {
    override val id: Int = id.value.length
  }
}

@JvmInline
public value class DomainId(
  public val `value`: String,
)

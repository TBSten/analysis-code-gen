// PATTERN: CRM-T-0282
// TITLE: CopyToChildren / valueClassMapping / excludeSuppressesConversion
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(autoValueClassMapping=true)/10--valueClassMapping/excludeSuppressesConversion.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: State.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface State {
  @CopyToChildren.Exclude
  public val id: String

  public class Loaded(
    id: DomainId,
  ) : State {
    override val id: String = id.value
  }
}

@JvmInline
public value class DomainId(
  public val `value`: String,
)

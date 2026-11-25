// PATTERN: CRM-T-0284
// TITLE: CopyToChildren / valueClassMapping / unwrapFromValueClass
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.autoValueClassMapping=true
// TAGS: cream.family=valueClassMapping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(autoValueClassMapping=true)/10--valueClassMapping/unwrapFromValueClass.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: State.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface State {
  public val id: DomainId

  public class Loaded(
    id: String,
  ) : State {
    override val id: DomainId = DomainId(id)
  }
}

@JvmInline
public value class DomainId(
  public val `value`: String,
)

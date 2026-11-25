// PATTERN: CRM-T-0501
// TITLE: SealedCopy / kdoc / description
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/07--kdoc/description.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.KDoc
import me.tbsten.cream.SealedCopy

@SealedCopy(kdoc = KDoc(description = "Custom description for the type-preserving copy."))
public sealed interface Source {
  public val name: String

  public data class Child(
    override val name: String,
  ) : Source
}

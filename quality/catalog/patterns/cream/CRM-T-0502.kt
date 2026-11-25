// PATTERN: CRM-T-0502
// TITLE: SealedCopy / kdoc / descriptionAndExamples
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/07--kdoc/descriptionAndExamples.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.KDoc
import me.tbsten.cream.SealedCopy

@SealedCopy(kdoc = KDoc(description = "Use this only when migrating.", examples = ["# Recommended\n\nval updated = state.copy(name = name)"]))
public sealed interface Source {
  public val name: String

  public data class Child(
    override val name: String,
  ) : Source
}

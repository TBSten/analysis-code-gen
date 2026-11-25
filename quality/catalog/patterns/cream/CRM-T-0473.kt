// PATTERN: CRM-T-0473
// TITLE: SealedCopy / sealedParentKind / nonSealedParentRejected
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sealedParentKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/00--sealedParentKind/nonSealedParentRejected.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.SealedCopy

@SealedCopy
public data class Source(
  public val name: String,
)

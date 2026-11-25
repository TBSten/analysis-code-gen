// PATTERN: CRM-T-0398
// TITLE: CopyTo / kdoc / multipleExamples
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/09--kdoc/multipleExamples.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyTo
import me.tbsten.cream.KDoc

@CopyTo(
  Target::class,
  kdoc = KDoc(description = "Two ways to call it.", examples = ["# Direct\n\nval a = source.copyToTarget()", "# Reuse\n\nval b = source.copyToTarget()"]),
)
public data class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
)

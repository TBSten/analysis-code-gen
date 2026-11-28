// PATTERN: CRM-T-0396
// TITLE: CopyTo / kdoc / examplesOnly
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/09--kdoc/examplesOnly.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyTo
import me.tbsten.cream.KDoc

@CopyTo(
  Target::class,
  kdoc = KDoc(description = "", examples = ["val target = source.copyToTarget()"]),
)
public data class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
)

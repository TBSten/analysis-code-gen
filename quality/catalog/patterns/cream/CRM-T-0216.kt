// PATTERN: CRM-T-0216
// TITLE: CopyFrom / kdoc / description
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/09--kdoc/description.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyFrom
import me.tbsten.cream.KDoc

@CopyFrom(
  Source::class,
  kdoc = KDoc(description = "Custom description for the copy function."),
)
public data class Target(
  public val name: String,
)

public data class Source(
  public val name: String,
)

// PATTERN: CRM-T-0300
// TITLE: CopyToChildren / propertyShape / customTypeProp
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/03--propertyShape/customTypeProp.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface Source {
  public val `data`: Data

  public data class Child(
    override val `data`: Data,
  ) : Source
}

public data class Data(
  public val `value`: String,
)

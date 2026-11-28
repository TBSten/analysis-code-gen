// PATTERN: CRM-T-0255
// TITLE: CopyMapping / propertyShape / zeroProps
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/05--propertyShape/zeroProps.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Mapping.kt
package me.tbsten.cream.generated

import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Target::class,
)
public object Mapping

public class Source

public class Target

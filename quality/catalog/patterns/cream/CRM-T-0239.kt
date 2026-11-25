// PATTERN: CRM-T-0239
// TITLE: CopyMapping / targetKind / sealedInterfaceTarget
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=targetKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/01--targetKind/sealedInterfaceTarget.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyMapping

@CopyMapping(
  Source::class,
  Target::class,
)
public object Mapping

public data class Source(
  public val name: String,
)

public sealed interface Target {
  public val name: String

  public sealed interface First : Target {
    public sealed interface Second : First {
      public data class Done(
        override val name: String,
      ) : Second

      public data class Refreshing(
        override val name: String,
        public val extra: Int,
      ) : Second
    }
  }
}

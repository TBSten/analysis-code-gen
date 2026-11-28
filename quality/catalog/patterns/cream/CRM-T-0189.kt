// PATTERN: CRM-T-0189
// TITLE: CopyFrom / targetKind / sealedInterfaceTarget
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=targetKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/01--targetKind/sealedInterfaceTarget.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyFrom

@CopyFrom(Source::class)
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

public data class Source(
  public val name: String,
)

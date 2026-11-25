// PATTERN: CRM-T-0326
// TITLE: CopyToChildren / useCase / CounterUiState MVI reducer transitions
// PROCESSORS: cream
// EXPECT: ok
// TAGS: cream.family=useCase
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/UseCase/CounterUiState MVI reducer transitions.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: CounterUiState.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.Long
import kotlin.String
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface CounterUiState {
  public val userId: String

  public val sessionStartedAt: Long

  public data class Idle(
    override val userId: String,
    override val sessionStartedAt: Long,
  ) : CounterUiState

  public data class Counting(
    override val userId: String,
    override val sessionStartedAt: Long,
    public val count: Int,
  ) : CounterUiState

  public data class Finished(
    override val userId: String,
    override val sessionStartedAt: Long,
    public val total: Int,
  ) : CounterUiState
}

// PATTERN: CRM-T-0327
// TITLE: CopyToChildren / useCase / SearchState Koma store
// PROCESSORS: cream
// EXPECT: ok
// TAGS: cream.family=useCase
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/UseCase/SearchState Koma store.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SearchState.kt
package me.tbsten.cream.generated

import java.time.Instant
import kotlin.String
import kotlin.collections.List
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface SearchState : State {
  public val query: String

  public val sortOrder: SortOrder

  public val searchedAt: Instant?

  public data class Loading(
    override val query: String,
    override val sortOrder: SortOrder,
    override val searchedAt: Instant?,
  ) : SearchState

  public data class Content(
    override val query: String,
    override val sortOrder: SortOrder,
    override val searchedAt: Instant?,
    public val results: List<Item>,
  ) : SearchState

  public data class Error(
    override val query: String,
    override val sortOrder: SortOrder,
    override val searchedAt: Instant?,
    public val message: String,
  ) : SearchState
}

public interface State

public data class Item(
  public val id: String,
)

public class SortOrder(
  public val key: String,
)

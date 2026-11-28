// PATTERN: CRM-T-0511
// TITLE: SealedCopy / useCase / FeedUiState refresh and banner keep subtype
// PROCESSORS: cream
// EXPECT: ok
// TAGS: cream.family=useCase
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/UseCase/FeedUiState refresh and banner keep subtype.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: FeedUiState.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.String
import kotlin.Throwable
import kotlin.collections.List
import me.tbsten.cream.NonCopyableStrategy
import me.tbsten.cream.SealedCopy

@SealedCopy(nonCopyableStrategy = NonCopyableStrategy.RETURN_AS_IS)
public sealed interface FeedUiState {
  public val isRefreshing: Boolean

  public val bannerMessage: String?

  public data object Loading : FeedUiState {
    override val isRefreshing: Boolean
      get() = false

    override val bannerMessage: String?
      get() = null
  }

  public data class Content(
    override val isRefreshing: Boolean,
    override val bannerMessage: String?,
    public val posts: List<Post>,
  ) : FeedUiState

  public data class Error(
    override val isRefreshing: Boolean,
    override val bannerMessage: String?,
    public val throwable: Throwable,
  ) : FeedUiState
}

public data class Post(
  public val id: String,
)

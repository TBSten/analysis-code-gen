// PATTERN: CRM-T-0225
// TITLE: CopyFrom / useCase / ItemDetailUiState transitions declared on targets
// PROCESSORS: cream
// EXPECT: ok
// TAGS: cream.family=useCase
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/UseCase/ItemDetailUiState transitions declared on targets.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: ItemDetailUiState.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.String
import me.tbsten.cream.CopyFrom

public sealed interface ItemDetailUiState {
  public val itemId: String

  public val isBookmarked: Boolean

  public val snackbarMessage: String?

  public data class Loading(
    override val itemId: String,
    override val isBookmarked: Boolean,
    override val snackbarMessage: String?,
  ) : ItemDetailUiState

  @CopyFrom(Loading::class)
  public data class Success(
    override val itemId: String,
    public val item: Item,
    override val isBookmarked: Boolean,
    override val snackbarMessage: String?,
  ) : ItemDetailUiState

  @CopyFrom(Loading::class)
  public data class Error(
    override val itemId: String,
    public val message: String,
    override val isBookmarked: Boolean,
    override val snackbarMessage: String?,
  ) : ItemDetailUiState
}

public data class Item(
  public val id: String,
)

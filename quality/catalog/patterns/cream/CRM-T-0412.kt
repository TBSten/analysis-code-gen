// PATTERN: CRM-T-0412
// TITLE: CopyTo / useCase / ItemDetailUiState transitions from Loading
// PROCESSORS: cream
// EXPECT: ok
// TAGS: cream.family=useCase
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/UseCase/ItemDetailUiState transitions from Loading.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: ItemDetailUiState.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.String
import me.tbsten.cream.CopyTo

public sealed interface ItemDetailUiState {
  public val itemId: String

  public val isBookmarked: Boolean

  public val snackbarMessage: String?

  @CopyTo(
    Success::class,
    Error::class,
  )
  public data class Loading(
    override val itemId: String,
    override val isBookmarked: Boolean,
    override val snackbarMessage: String?,
  ) : ItemDetailUiState

  public data class Success(
    override val itemId: String,
    public val item: Item,
    override val isBookmarked: Boolean,
    override val snackbarMessage: String?,
  ) : ItemDetailUiState

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

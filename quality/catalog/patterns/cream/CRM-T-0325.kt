// PATTERN: CRM-T-0325
// TITLE: CopyToChildren / useCase / CheckoutUiState nested state machine (notCopyToObject)
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true
// TAGS: cream.family=useCase
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/UseCase/CheckoutUiState nested state machine (notCopyToObject).md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: CheckoutUiState.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.collections.List
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface CheckoutUiState {
  public val sessionId: String

  public data object Idle : CheckoutUiState {
    override val sessionId: String
      get() = ""
  }

  public sealed interface Cart : CheckoutUiState {
    public val items: List<CartItem>

    public data class Editing(
      override val sessionId: String,
      override val items: List<CartItem>,
    ) : Cart

    public data class Validating(
      override val sessionId: String,
      override val items: List<CartItem>,
    ) : Cart
  }

  public sealed interface Payment : CheckoutUiState {
    public val items: List<CartItem>

    public val address: Address

    public data class SelectingMethod(
      override val sessionId: String,
      override val items: List<CartItem>,
      override val address: Address,
    ) : Payment

    public data class Processing(
      override val sessionId: String,
      override val items: List<CartItem>,
      override val address: Address,
      public val method: PaymentMethod,
    ) : Payment

    public data class Completed(
      override val sessionId: String,
      override val items: List<CartItem>,
      override val address: Address,
      public val orderId: String,
    ) : Payment
  }
}

public data class CartItem(
  public val name: String,
)

public data class Address(
  public val line: String,
)

public data class PaymentMethod(
  public val name: String,
)

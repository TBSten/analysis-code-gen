package sample.cream

/** 入力フォームの値。[User] の @CopyFrom の写し元。 */
data class UserForm(
    val name: String,
    val age: Int,
    val draft: Boolean,
)

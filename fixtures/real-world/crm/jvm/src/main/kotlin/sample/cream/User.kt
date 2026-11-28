package sample.cream

import me.tbsten.cream.CopyFrom

/** 写す先の側に注釈を置く例。[UserForm] から User への関数が作られる。 */
@CopyFrom(UserForm::class)
data class User(
    val name: String,
    val age: Int,
    val email: String?,
)

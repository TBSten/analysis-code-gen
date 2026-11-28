package sample.combo

/** 画面に出すモデル。[ProfileResponse] から cream.kt の写し関数で作る。 */
data class Profile(
    val id: String,
    val displayName: String,
)

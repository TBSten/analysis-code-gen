package sample.combo

import me.tbsten.cream.CopyTo

/** API の応答。cream.kt が `copyToProfile` を作る。 */
@CopyTo(Profile::class)
data class ProfileResponse(
    val id: String,
    val displayName: String,
    val updatedAt: Long,
)

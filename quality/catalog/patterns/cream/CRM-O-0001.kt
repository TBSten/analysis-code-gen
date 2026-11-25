// PATTERN: CRM-O-0001
// TITLE: Gradle モジュールをまたぐ @CopyTo（domain の型へ data 層から写す）
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: domain/commonMain/com/example/cream/domain/User.kt
package com.example.cream.domain

data class User(val id: Long, val name: String, val premium: Boolean)
// FILE: data/commonMain/com/example/cream/data/UserEntity.kt
package com.example.cream.data

import com.example.cream.domain.User
import me.tbsten.cream.CopyTo

@CopyTo(User::class)
data class UserEntity(val id: Long, val name: String)

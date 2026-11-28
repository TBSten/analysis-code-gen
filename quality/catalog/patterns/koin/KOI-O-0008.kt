// PATTERN: KOI-O-0008
// TITLE: Koin + cream.kt: ViewModel の状態遷移を cream で書く
// PROCESSORS: koin, cream
// EXPECT: ok
// ORIGIN: own
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが失敗した: Unresolved reference 'androidx'.
// FILE: main/com/example/combo/LoginState.kt
package com.example.combo

import me.tbsten.cream.CopyToChildren

@CopyToChildren
sealed interface LoginState {
    val email: String

    data class Input(override val email: String) : LoginState
    data class Loading(override val email: String) : LoginState
    data class Failed(override val email: String, val reason: String) : LoginState
}
// FILE: main/com/example/combo/LoginViewModel.kt
package com.example.combo

import androidx.lifecycle.ViewModel
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Single

@Single
class AuthApi

@KoinViewModel
class LoginViewModel(private val api: AuthApi) : ViewModel() {
    var state: LoginState = LoginState.Input("")
        private set

    fun submit() {
        state = state.copyToLoginStateLoading()
    }
}

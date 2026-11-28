// PATTERN: CMN-O-0007
// TITLE: 背景コード: 利用者のコードにコンパイルエラーがある（Processor とは無関係の箇所）
// PROCESSORS: *
// EXPECT: compile-error
// ORIGIN: own
// STATUS: verified
// NOTE: Processor が走ったあとにコンパイルが失敗する。K と B で診断の位置と種類が一致することを確かめる
// FILE: main/com/example/bg/broken/Broken.kt
package com.example.bg.broken

class Broken {
    val count: Int = "not a number"

    fun call() = undefinedFunction()
}

// PATTERN: CMN-O-0010
// TITLE: 背景コード: テストソースセットにだけある宣言
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// NOTE: Processor をテストのコンパイルにも適用したとき（kspTest など）に効く
// FILE: main/com/example/bg/testonly/Calculator.kt
package com.example.bg.testonly

class Calculator {
    fun add(a: Int, b: Int): Int = a + b
}
// FILE: test/com/example/bg/testonly/CalculatorFake.kt
package com.example.bg.testonly

class CalculatorFake {
    val calls: MutableList<Pair<Int, Int>> = mutableListOf()
}

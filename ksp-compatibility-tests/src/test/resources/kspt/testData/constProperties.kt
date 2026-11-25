// WITH_RUNTIME
// TEST PROCESSOR: ConstPropertiesProcessor
// EXPECTED:
// insideCompanionConstCompiled
// insideCompanionConstSource
// insideObjectConstCompiled
// insideObjectConstSource
// topLevelConstCompiled
// topLevelConstSource
// END
// MODULE: lib
// FILE: compiledProperties.kt
package foo.compiled

const val topLevelConstCompiled: String = "hello"
val topLevelCompiled: String = "hello"
val topLevelDelegatedCompiled by lazy { "hello" }
var topLevelVarCompiled: String = "hello"
val topLevelCustomGetterCompiled: String get() = "hello"
object TestObject {
    const val insideObjectConstCompiled: Boolean = true
    val insideObjectCompiled: String = "hello"
    val insideObjectDelegatedCompiled by lazy { "hello" }
    var insideVarObjectCompiled: String = "hello"
    val insideObjectCustomGetterCompiled: String get() = "hello"
}
interface Foo {
    val abstractCompiled: Long
    val abstractWithDefaultCompiled: Long get() = 100L
    companion object {
        const val insideCompanionConstCompiled: Int = 34
    }
}

// MODULE: main(lib)
// FILE: sourceProperties.kt
package foo.source

const val topLevelConstSource: String = "hello"
val topLevelSource: String = "hello"
val topLevelDelegatedSource by lazy { "hello" }
var topLevelVarSource: String = "hello"
val topLevelCustomGetterSource: String get() = "hello"
object TestObject {
    const val insideObjectConstSource: Boolean = true
    val insideObjectSource: String = "hello"
    val insideObjectDelegatedSource by lazy { "hello" }
    var insideVarObjectSource: String = "hello"
    val insideObjectCustomGetterSource: String get() = "hello"
}
interface Foo {
    val abstractSource: Long
    val abstractWithDefaultSource: Long get() = 100L
    companion object {
        const val insideCompanionConstSource: Int = 34
    }
}
/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/testData/constProperties.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（上流にヘッダが無いため、行番号を変えないよう定型のヘッダと出所の行を末尾に追記しただけ）

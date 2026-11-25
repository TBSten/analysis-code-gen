// TEST PROCESSOR: JvmNameProcessor
// EXPECTED:
// (getX, setX), (getY, null), (isOpen, setOpen), (getIsoSomething, setIsoSomething)
// (getX, setX), (getY, null), (isOpen, setOpen), (getIsoSomething, setIsoSomething)
// stringParameter
// stringParameter
// stringParameter
// JvmName: stringParameter
// JvmName: stringParameter
// END
// MODULE: lib
// FILE: Lib.kt
data class TestLibDataClass(var x: Int, val y: String, var isOpen: String, var isoSomething: String)
// FILE: MyAnnotationLib.kt
annotation class MyAnnotationLib(
    @get:JvmName("stringParameter")
    val stringParam: String
)
// FILE: MyAnnotationUserLib.java
@MyAnnotationLib(stringParameter = "foo")
class MyAnnotationUserLib {}

// MODULE: main(lib)
// FILE: K.kt
// FILE: MyAnnotation.kt
annotation class MyAnnotation(
    @get:JvmName("stringParameter")
    val stringParam: String
)
data class TestDataClass(var x: Int, val y: String, var isOpen: String, var isoSomething: String)
// FILE: MyAnnotationUser.java
@MyAnnotationLib(stringParameter = "foo")
@MyAnnotation(stringParameter = "foo")
class MyAnnotationUser {}

/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/testData/jvmName.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（上流にヘッダが無いため、行番号を変えないよう定型のヘッダと出所の行を末尾に追記しただけ）

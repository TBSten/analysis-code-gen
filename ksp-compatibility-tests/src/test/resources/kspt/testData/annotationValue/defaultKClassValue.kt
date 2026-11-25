// TEST PROCESSOR: DefaultKClassValueProcessor
// EXPECTED:
// kotlin.String
// kotlin.String
// kotlin.String
// kotlin.Int
// END
// MODULE: lib1
// FILE: lib1.kt
annotation class ExampleAnnotation(val value: kotlin.reflect.KClass<*> = java.lang.String::class)

// MODULE: main(lib1)
// FILE: a.kt

@ExampleAnnotation(String::class)
class Example

@ExampleAnnotation(Int::class)
class Example2

/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/testData/annotationValue/defaultKClassValue.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（上流にヘッダが無いため、行番号を変えないよう定型のヘッダと出所の行を末尾に追記しただけ）

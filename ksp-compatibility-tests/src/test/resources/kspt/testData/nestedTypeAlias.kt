// TEST PROCESSOR: TypeAliasProcessor
// EXPECTED:
// nestedAlias : NestedAlias = String = (expanded) String
// nestedClass : Nested = (expanded) Nested
// useOfAliasTop : Top = NestedAlias = String = (expanded) String
// param w.o. asMemberOf: NestedAlias = String = (expanded) String
// param with asMemberOf: NestedAlias = String = (expanded) String
// param: C.NestedAlias: NestedAlias = String = (expanded) String
// END

class C {
    typealias NestedAlias = String
    class Nested
}

typealias Top = C.NestedAlias

val nestedAlias: C.NestedAlias = ""
val nestedClass: C.Nested = C.Nested()
val useOfAliasTop: Top = ""

class Subject(val param: C.NestedAlias)
/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/testData/nestedTypeAlias.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（上流にヘッダが無いため、行番号を変えないよう定型のヘッダと出所の行を末尾に追記しただけ）

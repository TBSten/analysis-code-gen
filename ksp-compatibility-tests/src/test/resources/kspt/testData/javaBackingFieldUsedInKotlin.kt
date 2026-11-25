/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TEST PROCESSOR: ReachableAnnotationsFromGivenClasses
// PROCESSOR INPUT: MyClass
// EXPECTED:
// MyClass
// com.example.MyAnnotation
// EXPECT ENABLE_BACKING_FIELDS = TRUE: com.example.MyAnnotation.myValue.field
// com.example.MyAnnotation.myValue
// <init>
// END

// FILE: MyAnnotation.java
package com.example;

public @interface MyAnnotation {
    int myValue() default 42;
}

// FILE: MyClass.kt
import com.example.MyAnnotation

@MyAnnotation
class MyClass
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/testData/javaBackingFieldUsedInKotlin.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（行番号を変えないよう、出所の行を末尾に追記しただけ）

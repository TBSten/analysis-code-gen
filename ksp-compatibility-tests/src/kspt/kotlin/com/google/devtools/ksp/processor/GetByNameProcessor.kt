/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/GetByNameProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.getPropertyDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetByNameProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classNames =
            listOf(
                "lib1.Foo",
                "lib1.Foo.FooNested",
                "source.FooInSource",
                "source.FooInSource.FooInSourceNested",
            )
        val funNames =
            listOf(
                "lib1.Foo.lib1MemberFun",
                "lib1.lib1TopFun",
                "lib1.Bar.lib1JavaMemberFun",
                "lib2.Foo.lib2MemberFun",
                "source.FooInSource.sourceMemberFun",
            )
        val overloadFunctionNames = listOf("lib1.Foo.overload")
        val propNames =
            listOf(
                "lib1.Foo.lib1MemberProp",
                "lib1.lib1TopProp",
                "lib2.Foo.lib2MemberProp",
                "source.FooInSource.sourceMemberProp",
                "source.propInSource",
            )
        for (className in classNames) {
            if (resolver.getClassDeclarationByName(className) == null) {
                results.add("failed to get $className")
            }
        }
        for (funName in funNames) {
            if (resolver.getFunctionDeclarationsByName(funName, true).none()) {
                results.add("failed to get $funName")
            }
        }
        for (funName in overloadFunctionNames) {
            if (resolver.getFunctionDeclarationsByName(funName, true).toList().size != 2) {
                results.add("failed to get all $funName")
            }
        }
        for (propName in propNames) {
            if (resolver.getPropertyDeclarationByName(propName, true) == null) {
                results.add("failed to get $propName")
            }
        }
        if (results.isEmpty()) {
            results.add("all success")
        }
        return emptyList()
    }
}

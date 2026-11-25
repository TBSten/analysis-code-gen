/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/MultipleroundProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate

class MultipleroundProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    var round = 0

    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun gen(
            cls: String,
            ext: String,
        ) {
            env.codeGenerator.createNewFile(Dependencies(false), "com.example", cls, ext).use {
                it.write("package com.example;".toByteArray())
                it.write("interface $cls {}".toByteArray())
            }
        }

        when (round) {
            0 -> gen("I0", "kt")
            1 -> gen("I1", "java")
            2 -> gen("I2", "kt")
            3 -> gen("I3", "java")
            4 -> gen("I4", "kt")
            5 -> gen("I5", "java")
        }

        fun check(cls: String) {
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(cls))?.let { c ->
                result.add(
                    "${c.simpleName.asString()} : " +
                        c.superTypes
                            .map {
                                it
                                    .resolve()
                                    .declaration.simpleName
                                    .asString()
                            }.joinToString(),
                )
            }
        }

        result.add("Round $round:")
        check("K")
        check("J")
        val newFiles = resolver.getNewFiles().map { it.fileName }.toSet()
        val allFiles = resolver.getAllFiles().map { it.fileName }
        result.add(allFiles.map { if (it in newFiles) "+$it" else it }.sorted().joinToString())

        filesFromLastRound = resolver.getAllFiles()
        round++
        return emptyList()
    }

    lateinit var filesFromLastRound: Sequence<KSFile>

    override fun finish() {
        val allFiles = filesFromLastRound.map { it.fileName }.joinToString(", ")
        result.add("Finish: $allFiles")
        assert(filesFromLastRound.all { it.validate(apiFeatures = apiFeatures) })
        super.finish()
    }

    lateinit var env: SymbolProcessorEnvironment

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return super.create(environment)
    }
}

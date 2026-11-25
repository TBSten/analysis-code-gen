/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:test-utils/src/main/kotlin/com/google/devtools/ksp/processor/JavaSubtypeProcessor.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: なし（そのまま）
package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getJavaClassByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Variance

class JavaSubtypeProcessor(override val apiFeatures: ApiFeatures) : AbstractTestProcessor() {
    var isOk = true

    override fun toResult(): List<String> = listOf(isOk.toString())

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val javaCollection = resolver.getJavaClassByName("kotlin.collections.Collection")!!.asStarProjectedType()
        val javaSet = resolver.getJavaClassByName("kotlin.collections.Set")!!.asStarProjectedType()
        val ktFunctionJava = resolver.getJavaClassByName("kotlin.jvm.functions.Function0")!!.asStarProjectedType()
        val ktCollection = resolver.getClassDeclarationByName("kotlin.collections.Collection")!!.asStarProjectedType()
        val ktSet = resolver.getClassDeclarationByName("kotlin.collections.Set")!!.asStarProjectedType()
        val ktFunction = resolver.getClassDeclarationByName("kotlin.jvm.functions.Function0")!!.asStarProjectedType()
        val javaCollectionRef = resolver.createKSTypeReferenceFromKSType(javaCollection)
        val ktCollectionRef = resolver.createKSTypeReferenceFromKSType(ktCollection)
        val javaSetRef = resolver.createKSTypeReferenceFromKSType(javaSet)
        val ktSetRef = resolver.createKSTypeReferenceFromKSType(ktSet)
        val javaSetOfJavaSet = javaSet.replace(listOf(resolver.getTypeArgument(javaSetRef, Variance.INVARIANT)))
        val javaSetOfKtSet = javaSet.replace(listOf(resolver.getTypeArgument(ktSetRef, Variance.INVARIANT)))
        val ktSetOfJavaSet = ktSet.replace(listOf(resolver.getTypeArgument(javaSetRef, Variance.INVARIANT)))
        val ktSetOfKtSet = ktSet.replace(listOf(resolver.getTypeArgument(ktSetRef, Variance.INVARIANT)))
        val javaCollectionOfJavaSet =
            javaCollection
                .replace(listOf(resolver.getTypeArgument(javaSetRef, Variance.INVARIANT)))
        val javaCollectionOfKtSet =
            javaCollection
                .replace(listOf(resolver.getTypeArgument(ktSetRef, Variance.INVARIANT)))
        val ktCollectionOfJavaSet =
            ktCollection
                .replace(listOf(resolver.getTypeArgument(javaSetRef, Variance.INVARIANT)))
        val ktCollectionOfKtSet =
            ktCollection
                .replace(listOf(resolver.getTypeArgument(ktSetRef, Variance.INVARIANT)))
        val ktCollectionOfJavaCollection =
            ktCollection
                .replace(listOf(resolver.getTypeArgument(javaCollectionRef, Variance.INVARIANT)))
        val javaSetOfKtCollection =
            javaSet
                .replace(listOf(resolver.getTypeArgument(ktCollectionRef, Variance.INVARIANT)))
        val container = resolver.getClassDeclarationByName("Container")!!
        val strRef =
            resolver.getTypeArgument(
                (container.declarations.single { it.simpleName.asString() == "str" } as KSPropertyDeclaration).type,
                Variance.INVARIANT,
            )
        val javaStrCollection = javaCollection.replace(listOf(strRef))
        val javaStrSet = javaSet.replace(listOf(strRef))
        val kotlinFunctionImplementorJava = resolver.getClassDeclarationByName("IntSupplier")!!.asStarProjectedType()
        isOk = isOk && javaCollection.isAssignableFrom(javaSet)
        isOk = isOk && javaCollection.isAssignableFrom(javaStrCollection)
        isOk = isOk && !javaStrCollection.isAssignableFrom(javaCollection)
        isOk = isOk && !javaStrSet.isAssignableFrom(javaStrCollection)
        isOk = isOk && javaStrCollection.isAssignableFrom(javaStrSet)
        isOk = isOk && javaSet.isAssignableFrom(javaStrSet)
        isOk = isOk && javaCollection.isAssignableFrom(ktCollection)
        isOk = isOk && ktCollection.isAssignableFrom(javaCollection)
        isOk = isOk && javaSetOfJavaSet.isAssignableFrom(ktSetOfJavaSet)
        isOk = isOk && ktSetOfJavaSet.isAssignableFrom(javaSetOfJavaSet)
        isOk = isOk && javaSetOfJavaSet.isAssignableFrom(javaSetOfKtSet)
        isOk = isOk && javaSetOfKtSet.isAssignableFrom(javaSetOfJavaSet)
        isOk = isOk && ktSetOfKtSet.isAssignableFrom(ktSetOfJavaSet)
        isOk = isOk && ktSetOfJavaSet.isAssignableFrom(ktSetOfKtSet)
        isOk = isOk && javaCollectionOfJavaSet.isAssignableFrom(ktCollectionOfJavaSet)
        isOk = isOk && ktCollectionOfJavaSet.isAssignableFrom(javaCollectionOfJavaSet)
        isOk = isOk && javaCollectionOfKtSet.isAssignableFrom(ktCollectionOfKtSet)
        isOk = isOk && javaCollectionOfKtSet.isAssignableFrom(ktCollectionOfJavaSet)
        isOk = isOk && ktCollectionOfJavaCollection.isAssignableFrom(javaSetOfKtCollection)
        isOk = isOk && !javaSetOfKtCollection.isAssignableFrom(ktCollectionOfJavaCollection)
        isOk = isOk && ktFunction.isAssignableFrom(kotlinFunctionImplementorJava)
        isOk = isOk && ktFunctionJava.isAssignableFrom(ktFunction) && ktFunction.isAssignableFrom(ktFunctionJava)
        isOk = isOk && ktFunctionJava.isAssignableFrom(kotlinFunctionImplementorJava)
        return emptyList()
    }
}

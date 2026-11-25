@file:OptIn(KspExperimental::class)

package me.tbsten.analysiscodegen.e2e.brg

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.testfoundation.DiffSpec
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenTest

// 5.14 KSP Bridge の API 互換（BRG）のうち、kct（runCodegenTest）で回せる I / D。
// I は「KSP2 で同じテストが通る」ことを ID の無い補助テストで毎回確かめ、期待そのものが KSP の挙動どおりであることを保証する

// ---- 型・宣言を比べられる文字列にする ----

internal fun KSType.render(): String {
    val decl = declaration
    val name = decl.qualifiedName?.asString() ?: decl.simpleName.asString()
    val args = if (arguments.isEmpty()) "" else arguments.joinToString(", ", "<", ">") { it.render() }
    val flags =
        buildList {
            if (isFunctionType) add("fun")
            if (isSuspendFunctionType) add("suspend")
            if (isError) add("error")
        }
    return name + args + (if (isMarkedNullable) "?" else "") + (if (flags.isEmpty()) "" else flags.joinToString(",", "[", "]"))
}

internal fun KSTypeArgument.render(): String = if (type == null) "*" else "${variance.label} ${type!!.resolve().render()}".trim()

internal fun KSDeclaration.renderDecl(): String =
    buildString {
        append(qualifiedName?.asString() ?: simpleName.asString())
        append(" origin=").append(origin)
        append(" mods=").append(modifiers.map { it.name }.sorted())
        when (val d = this@renderDecl) {
            is KSClassDeclaration -> {
                append(" kind=").append(d.classKind)
                append(" typeParams=").append(
                    d.typeParameters.map { tp ->
                        tp.name.asString() + ":" +
                            tp.bounds.map { it.resolve().render() }.toList()
                    },
                )
                append(" supers=").append(
                    d.superTypes
                        .map { it.resolve().render() }
                        .toList()
                        .sorted(),
                )
                append(" decls=").append(
                    d.declarations
                        .map { it.simpleName.asString() }
                        .toList()
                        .sorted(),
                )
            }
            is KSFunctionDeclaration -> {
                append(" params=").append(d.parameters.map { it.name?.asString() + ":" + it.type.resolve().render() })
                append(" returns=").append(d.returnType?.resolve()?.render())
            }
            is KSPropertyDeclaration -> append(" type=").append(d.type.resolve().render())
            is KSTypeAlias -> append(" aliased=").append(d.type.resolve().render())
            else -> {}
        }
    }

private fun Resolver.cls(fqn: String): KSClassDeclaration = getClassDeclarationByName(getKSNameFromString(fqn)) ?: error("$fqn が無い")

/** I ケースの本体を、エンジンを差し替えて回せる形にしたもの。 */
private fun brg01(engine: TestEngine) {
    val text =
        inspect(engine, {
            kotlin("A.kt", "class A")
            options["brg.key"] = "value"
        }) { _, env ->
            listOf(
                "option=" + env.options["brg.key"],
                "kotlinVersion=" + env.kotlinVersion,
                "apiVersion=" + env.apiVersion,
                "compilerVersion=" + env.compilerVersion,
                "platforms=" + env.platforms.map { it.platformName },
                "logger=" + (env.logger != null),
                "codeGenerator=" + (env.codeGenerator != null),
            ).joinToString("\n")
        }
    text shouldContain "option=value"
    text shouldContain "kotlinVersion=${KotlinVersion.CURRENT.major}."
    text shouldContain "platforms=[JVM]"
    text shouldContain "logger=true"
    text shouldContain "codeGenerator=true"
}

private fun brg04(engine: TestEngine) {
    val seen = mutableListOf<String>()
    val provider =
        SymbolProcessorProvider { env ->
            object : SymbolProcessor {
                private var round = 0

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    round++
                    val annotated = resolver.getSymbolsWithAnnotation("com.example.Later").toList()
                    annotated.filterIsInstance<KSClassDeclaration>().forEach { seen += "round$round:${it.simpleName.asString()}" }
                    if (round == 1) {
                        // 次のラウンドを起こすため 1 ファイル作り、注釈付きの宣言を defer する
                        env.codeGenerator.createNewFile(Dependencies(false), "com.example", "Trigger").use {
                            it.write("package com.example\n\nobject Trigger".toByteArray())
                        }
                        return annotated
                    }
                    return emptyList()
                }
            }
        }
    val result =
        runCodegenTest(engine) {
            kotlin("com/example/Later.kt", "package com.example\n\nannotation class Later\n\n@Later class Waits")
            ksp(provider)
        }
    result.isSuccess shouldBe true
    seen shouldContain "round1:Waits"
    seen shouldContain "round2:Waits"
}

private fun brg05(engine: TestEngine) {
    val provider =
        SymbolProcessorProvider { env ->
            object : SymbolProcessor {
                private var done = false

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (done) return emptyList()
                    done = true
                    val files = resolver.getAllFiles().filter { it.fileName != "Ignored.kt" }.toList()
                    env.codeGenerator.createNewFile(Dependencies(aggregating = true, *files.toTypedArray()), "com.example", "All").use {
                        it.write("package com.example\n\nval allFiles = ${files.size}".toByteArray())
                    }
                    env.codeGenerator.createNewFile(Dependencies(false, files.first()), "com.example", "Isolated").use {
                        it.write("package com.example\n\nval isolated = true".toByteArray())
                    }
                    env.codeGenerator.associate(files, "com.example", "All", "kt")
                    env.codeGenerator.createNewFileByPath(Dependencies(false), "META-INF/brg05", "txt").use { it.write("ok".toByteArray()) }
                    return emptyList()
                }
            }
        }
    val result =
        runCodegenTest(engine) {
            kotlin("com/example/A.kt", "package com.example\n\nclass A")
            kotlin("com/example/B.kt", "package com.example\n\nclass B")
            ksp(provider)
        }
    result.isSuccess shouldBe true
    result.generatedFile("All.kt") shouldContain "allFiles = 2"
    result.generatedFile("Isolated.kt") shouldContain "isolated"
    result.generatedFile("META-INF/brg05.txt") shouldBe "ok"
    result.generatedFiles.keys shouldContain "resources/META-INF/brg05.txt"
}

private fun brg06(engine: TestEngine) {
    val provider =
        SymbolProcessorProvider { env ->
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    resolver.getSymbolsWithAnnotation("com.example.Reject").forEach { env.logger.error("BRG-06: rejected", it) }
                    return emptyList()
                }
            }
        }
    val result =
        runCodegenTest(engine) {
            kotlin("com/example/Rejected.kt", "package com.example\n\nannotation class Reject\n\n@Reject\nclass Rejected")
            ksp(provider)
        }
    result.isSuccess shouldBe false
    val error = result.errors.single { "BRG-06: rejected" in it.message }
    // KSNode の位置（ファイル名と行）が出る
    error.message shouldContain "Rejected.kt"
    error.message shouldContain ":6"
}

private fun brg08(engine: TestEngine) {
    val text =
        inspect(engine, brgSources) { resolver, _ ->
            resolver
                .getSymbolsWithAnnotation("com.example.Marker")
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.simpleName.asString() == "JavaModel" }
                .single()
                .let { java ->
                    listOf(
                        java.renderDecl(),
                        "fields=" + java.getAllProperties().map { it.simpleName.asString() + ":" + it.type.resolve().render() }.toList(),
                        "file=" + java.containingFile?.fileName,
                    ).joinToString("\n")
                }
        }
    text shouldContain "com.example.JavaModel origin=JAVA"
    text shouldContain "items:kotlin.collections.MutableList<out com.example.JavaModel.T>"
    text shouldContain "file=JavaModel.java"
}

class BridgeApiIntegrationTest : IntegrationSpec({
    case(
        "BRG-01",
        "SymbolProcessorProvider が見つかり、environment の options / kotlinVersion / apiVersion / platforms / logger / codeGenerator が正しい",
        pending = PendingCase(Milestone.M1),
    ) {
        brg01(TestEngine.AnalysisCodegen)
        // 値そのものも KSP2 と同じ
        val sources: me.tbsten.analysiscodegen.testing.CodegenTestSpec.() -> Unit = {
            kotlin("A.kt", "class A")
            options["brg.key"] =
                "value"
        }
        val pair =
            inspectBoth(
                sources,
            ) { _, env -> "${env.options}|${env.kotlinVersion}|${env.apiVersion}|${env.platforms.map { it.platformName }}" }
        pair.bridge shouldBe pair.ksp2
    }
    "[KSP2 で期待を確認] BRG-01" { brg01(TestEngine.Ksp2) }

    case("BRG-04", "process() が返した deferred シンボルが次のラウンドで再び渡される", pending = PendingCase(Milestone.M1)) {
        brg04(TestEngine.AnalysisCodegen)
    }
    "[KSP2 で期待を確認] BRG-04" { brg04(TestEngine.Ksp2) }

    case("BRG-05", "CodeGenerator.createNewFile / associate / Dependencies(aggregating) が使える", pending = PendingCase(Milestone.M1)) {
        brg05(TestEngine.AnalysisCodegen)
    }
    "[KSP2 で期待を確認] BRG-05" { brg05(TestEngine.Ksp2) }

    case("BRG-06", "KSPLogger の error でビルドが失敗し、KSNode の位置が出る", pending = PendingCase(Milestone.M1)) {
        brg06(TestEngine.AnalysisCodegen)
    }
    "[KSP2 で期待を確認] BRG-06" { brg06(TestEngine.Ksp2) }

    case("BRG-08", "Java ソース上の宣言も KS* として扱える", pending = PendingCase(Milestone.M2)) {
        brg08(TestEngine.AnalysisCodegen)
    }
    "[KSP2 で期待を確認] BRG-08" { brg08(TestEngine.Ksp2) }
})

// ---- D: KSP2 と同じ結果 ----

private fun resolverDump(
    resolver: Resolver,
    @Suppress("UNUSED_PARAMETER") env: SymbolProcessorEnvironment,
): String =
    buildString {
        appendLine(
            "annotated=" +
                resolver
                    .getSymbolsWithAnnotation(
                        "com.example.Marker",
                    ).map { (it as? KSDeclaration)?.renderDecl() ?: it.toString() }
                    .toList()
                    .sorted(),
        )
        appendLine(
            "newFiles=" +
                resolver
                    .getNewFiles()
                    .map { it.packageName.asString() + "/" + it.fileName }
                    .toList()
                    .sorted(),
        )
        appendLine(
            "allFiles=" +
                resolver
                    .getAllFiles()
                    .map { it.packageName.asString() + "/" + it.fileName }
                    .toList()
                    .sorted(),
        )
        appendLine("byName=" + resolver.cls("com.example.Box").renderDecl())
        appendLine("nested=" + resolver.cls("com.example.Result.Ok").renderDecl())
        appendLine("missing=" + resolver.getClassDeclarationByName(resolver.getKSNameFromString("com.example.Nope")))
        appendLine(
            "pkg=" +
                resolver
                    .getDeclarationsFromPackage("com.example.other")
                    .map { it.renderDecl() }
                    .toList()
                    .sorted(),
        )
        appendLine(
            "functions=" +
                resolver
                    .getFunctionDeclarationsByName(resolver.getKSNameFromString("com.example.transform"), includeTopLevel = true)
                    .map {
                        it.renderDecl()
                    }.toList(),
        )
        appendLine(
            "property=" +
                resolver
                    .getPropertyDeclarationByName(
                        resolver.getKSNameFromString("com.example.topLevel"),
                        includeTopLevel = true,
                    )?.renderDecl(),
        )
        appendLine(
            "builtIns=" + listOf(resolver.builtIns.anyType, resolver.builtIns.stringType, resolver.builtIns.unitType).map { it.render() },
        )
    }

private fun typeDump(
    resolver: Resolver,
    @Suppress("UNUSED_PARAMETER") env: SymbolProcessorEnvironment,
): String =
    buildString {
        val box = resolver.cls("com.example.Box")
        val shape = resolver.cls("com.example.Shape")
        val transform = resolver.getFunctionDeclarationsByName(resolver.getKSNameFromString("com.example.transform"), true).single()
        val boxType = box.asStarProjectedType()
        appendLine("box*=" + boxType.render())
        appendLine(
            "boxString=" +
                box
                    .asType(
                        listOf(
                            resolver.getTypeArgument(
                                resolver.createKSTypeReferenceFromKSType(resolver.builtIns.stringType),
                                com.google.devtools.ksp.symbol.Variance.INVARIANT,
                            ),
                        ),
                    ).render(),
        )
        appendLine("shape<-box=" + shape.asStarProjectedType().isAssignableFrom(boxType))
        appendLine("box<-shape=" + boxType.isAssignableFrom(shape.asStarProjectedType()))
        appendLine("any<-box?=" + resolver.builtIns.anyType.isAssignableFrom(boxType.makeNullable()))
        appendLine(
            "anyNullable<-box?=" +
                resolver.builtIns.anyType
                    .makeNullable()
                    .isAssignableFrom(boxType.makeNullable()),
        )
        appendLine("params=" + transform.parameters.map { it.name!!.asString() + ":" + it.type.resolve().render() })
        appendLine("return=" + transform.returnType!!.resolve().render())
        resolver.getDeclarationsFromPackage("com.example").filterIsInstance<KSTypeAlias>().sortedBy { it.simpleName.asString() }.forEach {
            appendLine("alias ${it.simpleName.asString()}=" + it.type.resolve().render())
        }
        appendLine(
            "boxProps=" +
                box
                    .getAllProperties()
                    .map { it.simpleName.asString() + ":" + it.type.resolve().render() }
                    .toList()
                    .sorted(),
        )
        appendLine(
            "starProjection=" +
                resolver
                    .cls("kotlin.collections.List")
                    .asStarProjectedType()
                    .starProjection()
                    .render(),
        )
    }

private fun libraryDump(
    resolver: Resolver,
    @Suppress("UNUSED_PARAMETER") env: SymbolProcessorEnvironment,
): String =
    buildString {
        listOf(
            "kotlin.collections.List",
            "java.util.ArrayList",
            "kotlin.Result",
            "java.util.concurrent.ConcurrentHashMap",
            "kotlin.coroutines.Continuation",
        ).forEach { fqn ->
            val c = resolver.cls(fqn)
            appendLine(c.renderDecl())
            appendLine(
                "  functions=" +
                    c
                        .getAllFunctions()
                        .map { it.simpleName.asString() }
                        .toList()
                        .sorted()
                        .distinct()
                        .take(40),
            )
            appendLine("  annotations=" + c.annotations.map { it.shortName.asString() }.toList())
            appendLine("  file=" + c.containingFile)
        }
    }

private fun jvmDump(
    resolver: Resolver,
    @Suppress("UNUSED_PARAMETER") env: SymbolProcessorEnvironment,
): String =
    buildString {
        val box = resolver.cls("com.example.Box")
        val transform = resolver.getFunctionDeclarationsByName(resolver.getKSNameFromString("com.example.transform"), true).single()
        val topLevel = resolver.getPropertyDeclarationByName(resolver.getKSNameFromString("com.example.topLevel"), true)!!
        appendLine("jvmName(transform)=" + resolver.getJvmName(transform))
        appendLine("sig(transform)=" + resolver.mapToJvmSignature(transform))
        appendLine("owner(transform)=" + resolver.getOwnerJvmClassName(transform))
        appendLine("owner(topLevel)=" + resolver.getOwnerJvmClassName(topLevel))
        appendLine("sig(topLevel)=" + resolver.mapToJvmSignature(topLevel))
        appendLine("jvmName(getter)=" + topLevel.getter?.let { resolver.getJvmName(it) })
        appendLine("sig(Box)=" + resolver.mapToJvmSignature(box))
        box.getAllFunctions().sortedBy { it.simpleName.asString() }.forEach {
            appendLine(
                "fun ${it.simpleName.asString()}=" + resolver.mapToJvmSignature(it),
            )
        }
        appendLine(
            "checked(JavaModel.count)=" +
                resolver
                    .cls("com.example.JavaModel")
                    .getAllFunctions()
                    .filter { it.simpleName.asString() == "count" }
                    .map {
                        resolver
                            .getJvmCheckedException(it)
                            .map { t ->
                                t.render()
                            }.toList()
                    }.toList(),
        )
    }

class BridgeApiDiffTest : DiffSpec({
    case(
        "BRG-02",
        "Resolver の主要 API（getSymbolsWithAnnotation、getNewFiles、getAllFiles、getClassDeclarationByName、getDeclarationsFromPackage など）が KSP2 と同じ結果を返す",
        pending = PendingCase(Milestone.M1),
    ) {
        val pair = inspectBoth(brgSources, ::resolverDump)
        pair.bridge shouldBe pair.ksp2
    }
    "[KSP2 で入力が働くことを確認] BRG-02" {
        val text = inspect(TestEngine.Ksp2, brgSources, ::resolverDump)
        text shouldContain "com.example.Box origin=KOTLIN"
        text shouldContain "com.example.JavaModel origin=JAVA"
        text shouldContain "missing=null"
    }

    case("BRG-03", "KS* の型解決（resolve、isAssignableFrom、star projection、typealias、関数型）が KSP2 と同じ結果", pending = PendingCase(Milestone.M1)) {
        val pair = inspectBoth(brgSources, ::typeDump)
        pair.bridge shouldBe pair.ksp2
    }
    "[KSP2 で入力が働くことを確認] BRG-03" {
        val text = inspect(TestEngine.Ksp2, brgSources, ::typeDump)
        text shouldContain "shape<-box=true"
        text shouldContain "box<-shape=false"
        text shouldContain "[fun]"
    }

    case("BRG-09", "ライブラリ（バイナリ）の宣言が KSP2 と同じ Origin・内容で表現される", pending = PendingCase(Milestone.M2)) {
        val pair = inspectBoth(brgSources, ::libraryDump)
        pair.bridge shouldBe pair.ksp2
    }
    "[KSP2 で入力が働くことを確認] BRG-09" {
        inspect(TestEngine.Ksp2, brgSources, ::libraryDump) shouldContain "origin=KOTLIN_LIB"
    }

    case("BRG-10", "JVM 固有 API（getJvmName、mapToJvmSignature、getOwnerJvmClassName など）が KSP2 と同じ結果", pending = PendingCase(Milestone.M2)) {
        val pair = inspectBoth(brgSources, ::jvmDump)
        pair.bridge shouldBe pair.ksp2
    }
    "[KSP2 で入力が働くことを確認] BRG-10" {
        inspect(TestEngine.Ksp2, brgSources, ::jvmDump) shouldContain "owner(transform)=com.example.ModelKt"
    }

    case("BRG-12", "KSP 互換モードで、生成ファイルの相対パスとファイル名が KSP2 と一致する", pending = PendingCase(Milestone.M2)) {
        val provider =
            SymbolProcessorProvider { env ->
                object : SymbolProcessor {
                    private var done = false

                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        if (done) return emptyList()
                        done = true
                        val gen = env.codeGenerator
                        gen.createNewFile(Dependencies(false), "com.example.gen", "KtFile").use {
                            it.write("package com.example.gen\n\nclass KtFile".toByteArray())
                        }
                        gen.createNewFile(Dependencies(false), "", "RootFile").use { it.write("class RootFile".toByteArray()) }
                        gen.createNewFile(Dependencies(false), "com.example.gen", "JavaFile", "java").use {
                            it.write("package com.example.gen;\n\npublic class JavaFile {}".toByteArray())
                        }
                        gen
                            .createNewFileByPath(
                                Dependencies(false),
                                "META-INF/services/com.example.Service",
                                "",
                            ).use { it.write("x".toByteArray()) }
                        gen.createNewFileByPath(Dependencies(false), "nested/dir/data", "json").use { it.write("{}".toByteArray()) }
                        return emptyList()
                    }
                }
            }

        fun run(engine: TestEngine) =
            runCodegenTest(engine) {
                kotlin("A.kt", "class A")
                ksp(provider)
            }
        val ksp2 = run(TestEngine.Ksp2)
        val bridge = run(TestEngine.AnalysisCodegen)
        bridge.generatedFiles.keys shouldBe ksp2.generatedFiles.keys
        bridge.comparable() shouldBe ksp2.comparable()
    }
})

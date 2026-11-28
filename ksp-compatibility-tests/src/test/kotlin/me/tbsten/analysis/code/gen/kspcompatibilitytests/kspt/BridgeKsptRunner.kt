@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.kspcompatibilitytests.kspt

import me.tbsten.analysis.code.gen.engine.AnalysisCodegenEngine
import me.tbsten.analysis.code.gen.engine.EngineRequest
import me.tbsten.analysis.code.gen.engine.InstanceProcessorComponent
import me.tbsten.analysis.code.gen.model.AnalysisConfig
import me.tbsten.analysis.code.gen.model.CompilationModel
import me.tbsten.analysis.code.gen.model.CompilerOptionsModel
import me.tbsten.analysis.code.gen.model.DeferredSeverityModel
import me.tbsten.analysis.code.gen.model.ExecutionUnitModel
import me.tbsten.analysis.code.gen.model.KspBridgeConfig
import me.tbsten.analysis.code.gen.model.KspProcessingModeModel
import me.tbsten.analysis.code.gen.model.OutputDirsModel
import me.tbsten.analysis.code.gen.model.PlatformKind
import me.tbsten.analysis.code.gen.model.ProcessingScopeModel
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.model.SourceSetModel
import me.tbsten.analysis.code.gen.model.UnitKind
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import java.io.File

/**
 * KSPT の testData を analysisCodeGen のエンジンと KSP Bridge（B）で流す（ADR-024・062・064）。
 *
 * [Ksp2KsptRunner] と同じ手順でライブラリのモジュールをコンパイルし、main だけを 1 つの実行単位にした ProjectModel を
 * AnalysisCodegenEngine で処理する。テスト用 Processor（[com.google.devtools.ksp.processor.AbstractTestProcessor] は
 * Provider も兼ねる）はインスタンスのまま渡し、Bridge の Frontend が KSP の Processor として回す。
 */
object BridgeKsptRunner {
    fun run(
        data: KsptTestData,
        features: KsptFeatures,
        workDir: File,
    ): String {
        require(data.nativeTarget == null) { "Kotlin/Native の KSPT は Bridge ではまだ流さない（M1 の範囲外、ADR-056）" }
        workDir.deleteRecursively()
        val outDirs = mutableMapOf<String, File>()
        for (lib in data.libraryModules) {
            val deps = (lib.dependencies + lib.friends).map { outDirs.getValue(it) }
            outDirs[lib.name] = Ksp2KsptRunner.compileLibrary(data, lib, deps, File(workDir, lib.name))
        }
        val main = data.mainModule
        val root = File(workDir, main.name)
        val (kotlinSrc, javaSrc) = Ksp2KsptRunner.writeSources(main, root)
        val processor = Ksp2KsptRunner.newProcessor(data, features)
        val libraries = main.dependencies.map { outDirs.getValue(it) } + Ksp2KsptRunner.runtimeLibraries()
        val friends = main.friends.map { outDirs.getValue(it) }
        // 実行単位・SourceSet の名前を KSP2 に渡すモジュール名（`// COMPILER MODULE NAME:`）にそろえる。internal の名前の mangle に出るため
        val unitName = main.compilerModuleName
        val model = model(unitName, root, kotlinSrc, javaSrc, libraries, friends, data.jvmTarget ?: "1.8")
        val component = InstanceProcessorComponent("kspt:${data.processorName}", listOf(processor))
        val result = AnalysisCodegenEngine().run(EngineRequest(model, unitName, forceFull = true, extraComponents = listOf(component)))
        val lines =
            if (result.success) {
                processor.toResult()
            } else {
                // 原因の分類のため、エンジンの診断を標準出力に残す（期待値との比較には使わない）
                println("[B] ${data.path} [$features] の診断:\n" + result.diagnostics.joinToString("\n") { it.render() })
                // KSP2 の KotlinSymbolProcessing は Processor のエラーで PROCESSING_ERROR を返す（Ksp2KsptRunner と同じ書式）
                listOf("KSP FAILED WITH EXIT CODE: PROCESSING_ERROR") + processor.toResult()
            }
        return lines.joinToString("\n")
    }

    private fun model(
        unitName: String,
        root: File,
        kotlinSrc: File,
        javaSrc: File,
        libraries: List<File>,
        friends: List<File>,
        jvmTarget: String,
    ): ProjectModel {
        val compilation =
            CompilationModel(
                name = unitName,
                targetName = "",
                compilationName = "main",
                platform = PlatformKind.Jvm,
                defaultSourceSet = unitName,
                sourceSets = listOf(unitName),
                classpath = (libraries + friends).map { it.absolutePath },
                friendPaths = friends.map { it.absolutePath },
                jdkHome = Ksp2KsptRunner.jdkHome.absolutePath,
                compilerOptions =
                    CompilerOptionsModel(
                        languageVersion = Ksp2KsptRunner.LANGUAGE_VERSION,
                        apiVersion = Ksp2KsptRunner.LANGUAGE_VERSION,
                        optIns = emptyList(),
                        freeArgs = emptyList(),
                        jvmTarget = jvmTarget,
                    ),
            )
        val out = "kspTest"
        val unit =
            ExecutionUnitModel(
                name = unitName,
                kind = UnitKind.SourceSet,
                target = unitName,
                analysisCompilation = unitName,
                processingScope = ProcessingScopeModel.Own,
                // Processor は EngineRequest.extraComponents でインスタンスのまま渡す
                processors = emptyList(),
                options = emptyMap(),
                outputDirs = OutputDirsModel("$out/src/main/kotlin", "$out/src/main/java", "$out/src/main/resources"),
                stagingDir = "$out/staging",
                cacheDir = "$out/caches",
            )
        return ProjectModel(
            schemaVersion = 1,
            rootDir = root.absolutePath,
            sourceSets =
                listOf(
                    SourceSetModel(
                        name = unitName,
                        platform = PlatformKind.Jvm,
                        kotlinSourceRoots = listOf(kotlinSrc.relativeTo(root).invariantSeparatorsPath),
                        javaSourceRoots = listOf(javaSrc.relativeTo(root).invariantSeparatorsPath),
                        dependsOn = emptyList(),
                        upstreamGeneratedRoots = emptyList(),
                    ),
                ),
            compilations = listOf(compilation),
            units = listOf(unit),
            analysis =
                AnalysisConfig(
                    analysisApiVersion = KotlinVersion.CURRENT.toString(),
                    maxRounds = 100,
                    unresolvedDeferred = DeferredSeverityModel.Warning,
                    kspBridge = KspBridgeConfig(KspProcessingModeModel.Compatible, kspVersionCheck = false),
                ),
        )
    }
}

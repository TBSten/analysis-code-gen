package me.tbsten.analysis.code.gen.testprocessors.native

import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysis.code.gen.processor.CodegenLogger
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.processor.ExecutionUnit
import me.tbsten.analysis.code.gen.testprocessors.ActualFunction
import me.tbsten.analysis.code.gen.testprocessors.ActualModel
import me.tbsten.analysis.code.gen.testprocessors.ActualRender
import me.tbsten.analysis.code.gen.testprocessors.BadMode
import me.tbsten.analysis.code.gen.testprocessors.BadRender
import me.tbsten.analysis.code.gen.testprocessors.BodyModel
import me.tbsten.analysis.code.gen.testprocessors.BodyRender
import me.tbsten.analysis.code.gen.testprocessors.ChainModel
import me.tbsten.analysis.code.gen.testprocessors.ChainRender
import me.tbsten.analysis.code.gen.testprocessors.DeferModel
import me.tbsten.analysis.code.gen.testprocessors.DeferRender
import me.tbsten.analysis.code.gen.testprocessors.MapperModel
import me.tbsten.analysis.code.gen.testprocessors.MapperProperty
import me.tbsten.analysis.code.gen.testprocessors.MapperRender
import me.tbsten.analysis.code.gen.testprocessors.PlatformNames
import me.tbsten.analysis.code.gen.testprocessors.ProbeEvent
import me.tbsten.analysis.code.gen.testprocessors.RegistryModel
import me.tbsten.analysis.code.gen.testprocessors.RegistryRender
import me.tbsten.analysis.code.gen.testprocessors.ResourceModel
import me.tbsten.analysis.code.gen.testprocessors.ResourceRender
import me.tbsten.analysis.code.gen.testprocessors.TpAnnotations
import me.tbsten.analysis.code.gen.testprocessors.TpId
import me.tbsten.analysis.code.gen.testprocessors.TpOptions
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

// 各 TP の振る舞いは ADR-027 と test-processors/README.md。KSP 版（test-processors/ksp）と生成物を一致させる。
// 処理対象: ラウンド 1 は newFiles（全件ビルドなら allFiles）、ラウンド 2 以降は newFiles（前のラウンドの生成物）。KSP の
// getSymbolsWithAnnotation と同じ範囲になる

// ---- TP-Mapper（isolating） ----

public class MapperNativeProvider : TpNativeProvider(TpId.Mapper) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val models =
                    analyze(context.sourceSet.module) {
                        classesWith(context.newFiles, TpAnnotations.MAPPED).map { (psi, symbol) ->
                            val properties =
                                psi.primaryConstructorParameters.filter { it.hasValOrVar() }.map { param ->
                                    MapperProperty(param.name!!, notation((param.symbol as KaValueParameterSymbol).returnType))
                                }
                            psi.containingKtFile to MapperModel(symbol.packageName, symbol.nameInPackage, properties)
                        }
                    }
                models.forEach { (file, model) -> context.codeGenerator.write(Dependencies.isolating(file), MapperRender.render(model)) }
            }
        }
}

// ---- TP-Registry（aggregating。ラウンド 1 だけ。Processing Scope の全ファイルを見る） ----

public class RegistryNativeProvider : TpNativeProvider(TpId.Registry) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                if (context.round != 1) return
                val found =
                    analyze(context.sourceSet.module) {
                        classesWith(context.allFiles, TpAnnotations.REGISTERED).map { (psi, symbol) ->
                            psi.containingKtFile to
                                symbol.classId!!.asFqNameString()
                        }
                    }
                if (found.isEmpty()) return
                val model =
                    RegistryModel(options.registryPackage, options.registryName, found.map { it.second }, platformName(context.platform))
                context.codeGenerator.write(Dependencies.aggregating(found.map { it.first }.distinct()), RegistryRender.render(model))
            }
        }
}

// ---- TP-Chain（ラウンドをまたいで N 段） ----

public class ChainNativeProvider : TpNativeProvider(TpId.Chain) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val models =
                    analyze(context.sourceSet.module) {
                        classesWith(context.newFiles, TpAnnotations.CHAIN).map { (psi, symbol) ->
                            psi.containingKtFile to
                                ChainModel(
                                    packageName = symbol.packageName,
                                    classNameInPackage = symbol.nameInPackage,
                                    depth = annotationArgument(symbol, TpAnnotations.CHAIN, "depth") as Int,
                                    step = annotationArgument(symbol, TpAnnotations.CHAIN, "step") as? Int ?: 0,
                                    origin = annotationArgument(symbol, TpAnnotations.CHAIN, "origin") as? String ?: "",
                                )
                        }
                    }
                models.forEach { (file, model) ->
                    ChainRender.render(model)?.let { context.codeGenerator.write(Dependencies.isolating(file), it) }
                }
            }
        }
}

// ---- TP-Actual（プラットフォームの実行単位だけ。ラウンド 1 だけ） ----

public class ActualNativeProvider : TpNativeProvider(TpId.Actual) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val platform = platformName(context.platform)
                if (context.round != 1 || platform == PlatformNames.COMMON) return
                // expect は上流（commonMain など）にある。Compilation 単位なら参加する全 SourceSet、SourceSet 単位なら dependsOn の上流まで見る
                val files =
                    context.compilation
                        ?.sourceSets
                        ?.flatMap { it.ownFiles }
                        ?.distinct() ?: context.sourceSet.visibleFiles
                val generateActual = classIdOf(TpAnnotations.GENERATE_ACTUAL)
                val models =
                    analyze(context.sourceSet.module) {
                        files.sortedBy { it.virtualFilePath }.mapNotNull { file ->
                            val functions =
                                file.declarations
                                    .filterIsInstance<KtNamedFunction>()
                                    .filter { it.hasModifier(KtTokens.EXPECT_KEYWORD) && generateActual in it.symbol.annotations }
                                    .filter { fn ->
                                        val ok = fn.valueParameters.isEmpty() && notation(fn.symbol.returnType) == "kotlin.String"
                                        if (!ok) context.logger.error("TP-Actual は `expect fun name(): String` だけを扱う", fn)
                                        ok
                                    }.map {
                                        ActualFunction(
                                            it.name!!,
                                            if (it.hasModifier(KtTokens.INTERNAL_KEYWORD)) "internal" else "public",
                                        )
                                    }
                            if (functions.isEmpty()) {
                                null
                            } else {
                                file to
                                    ActualModel(file.packageFqName.asString(), file.name.removeSuffix(".kt"), functions, platform)
                            }
                        }
                    }
                models.forEach { (file, model) -> context.codeGenerator.write(Dependencies.isolating(file), ActualRender.render(model)) }
            }
        }
}

// ---- TP-Body（関数本体の呼び出し先。AA でしかできない解析なので Native 版だけ） ----

public class BodyNativeProvider : TpNativeProvider(TpId.Body) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val callGraph = classIdOf(TpAnnotations.CALL_GRAPH)
                val models =
                    analyze(context.sourceSet.module) {
                        context.newFiles.flatMap { file ->
                            file
                                .collectDescendantsOfType<KtNamedFunction>()
                                .filter { callGraph in it.symbol.annotations }
                                .map { fn ->
                                    val callees =
                                        fn.bodyExpression?.collectDescendantsOfType<KtCallExpression>().orEmpty().mapNotNull { call ->
                                            when (
                                                val callee =
                                                    call
                                                        .resolveToCall()
                                                        ?.successfulFunctionCallOrNull()
                                                        ?.partiallyAppliedSymbol
                                                        ?.signature
                                                        ?.symbol
                                            ) {
                                                is KaConstructorSymbol -> callee.containingClassId?.asFqNameString()?.let { "$it.<init>" }
                                                is KaNamedFunctionSymbol -> callee.callableId?.asSingleFqName()?.asString()
                                                else -> null
                                            }
                                        }
                                    val owner = fn.containingClassOrObject?.let { (it.symbol as? KaClassLikeSymbol)?.nameInPackage }
                                    file to BodyModel(file.packageFqName.asString(), owner, fn.name!!, callees)
                                }
                        }
                    }
                models.forEach { (file, model) -> context.codeGenerator.write(Dependencies.isolating(file), BodyRender.render(model)) }
            }
        }
}

// ---- TP-Defer（解決できるまで持ち越す） ----

public class DeferNativeProvider : TpNativeProvider(TpId.Defer) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val ready = mutableListOf<Pair<KtFile, DeferModel>>()
                analyze(context.sourceSet.module) {
                    val candidates =
                        classesWith(context.newFiles, TpAnnotations.DEFER_UNTIL).map { it.second } +
                            context.deferred.mapNotNull { it.pointer.restoreSymbol() as? KaClassLikeSymbol }
                    candidates.distinctBy { it.classId }.forEach { symbol ->
                        val target = annotationArgument(symbol, TpAnnotations.DEFER_UNTIL, "target") as String
                        if (findClass(classIdOf(target)) == null) {
                            @Suppress("UNCHECKED_CAST")
                            context.defer(symbol.createPointer() as KaSymbolPointer<KaSymbol>, target)
                        } else {
                            val file = (symbol.psi as KtClassOrObject).containingKtFile
                            ready += file to DeferModel(symbol.packageName, symbol.nameInPackage, target)
                        }
                    }
                }
                ready.forEach { (file, model) -> context.codeGenerator.write(Dependencies.isolating(file), DeferRender.render(model)) }
            }
        }
}

// ---- TP-Resource（Java・リソース・次のラウンドで Kotlin） ----

public class ResourceNativeProvider : TpNativeProvider(TpId.Resource) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            // ラウンドをまたいで持つのは文字列とファイルの相対パスだけ（ADR-014）
            private val pending = mutableListOf<Pair<ResourceModel, String>>()

            override fun process(context: AnalysisCodegenRoundContext) {
                val gen = context.codeGenerator
                analyze(context.sourceSet.module) {
                    val ready = pending.filter { (model, _) -> findClass(classIdOf(ResourceRender.javaFqn(model))) != null }
                    ready.forEach { (model, path) ->
                        val file = context.allFiles.first { it.virtualFilePath == path }
                        gen.write(Dependencies.isolating(file), ResourceRender.kotlinRef(model))
                    }
                    pending -= ready.toSet()

                    val classes = classesWith(context.newFiles, TpAnnotations.GENERATE_JAVA)
                    val models =
                        classes.map { (psi, symbol) ->
                            psi.containingKtFile to
                                ResourceModel(symbol.packageName, symbol.nameInPackage)
                        }
                    models.forEach { (file, model) ->
                        gen.write(Dependencies.isolating(file), ResourceRender.java(model))
                        gen.write(Dependencies.isolating(file), ResourceRender.properties(model))
                        pending += model to file.virtualFilePath
                    }
                    if (context.round == 1 && models.isNotEmpty()) {
                        gen.write(
                            Dependencies.aggregating(models.map { it.first }.distinct()),
                            ResourceRender.services(models.map { it.second }),
                        )
                    }
                }
            }

            override fun finish() {
                pending.forEach { (model, _) -> environment.logger.error("TP-Resource: ${ResourceRender.javaFqn(model)} が次のラウンドで解決できなかった") }
            }
        }
}

// ---- TP-Bad（わざと壊れる） ----

public class BadNativeProvider : TpNativeProvider(TpId.Bad) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val gen = context.codeGenerator
                val modes = options.badModes
                val all = Dependencies.all
                if (context.round == 1) {
                    if (BadMode.Collide in modes) {
                        val mapped =
                            analyze(context.sourceSet.module) {
                                classesWith(context.newFiles, TpAnnotations.MAPPED).map { (psi, s) ->
                                    psi.containingKtFile to
                                        BadRender.collide(s.packageName, s.nameInPackage)
                                }
                            }
                        mapped.forEach { (file, out) -> gen.write(Dependencies.isolating(file), out) }
                    }
                    if (BadMode.Duplicate in modes) {
                        gen.write(all, BadRender.simple("BadDuplicate"))
                        gen.write(all, BadRender.simple("BadDuplicate"))
                    }
                    if (BadMode.WriteOutside in modes) {
                        gen.write(all, BadRender.simple("../../../escaped/BadOutside", packageName = ""))
                    }
                    if (BadMode.LogError in modes) {
                        val target =
                            analyze(
                                context.sourceSet.module,
                            ) { classesWith(context.newFiles, TpAnnotations.BAD_TARGET).firstOrNull()?.first }
                        context.logger.error(BadRender.LOG_ERROR_MESSAGE, target)
                    }
                    if (BadMode.Throw in modes) {
                        gen.write(all, BadRender.simple("BadPartial"))
                        throw IllegalStateException(BadRender.THROW_MESSAGE)
                    }
                }
                if (BadMode.InfiniteRounds in modes || BadMode.StuckDeferWithProgress in modes) {
                    gen.write(all, BadRender.simple("BadRound${context.round}"))
                }
                if (BadMode.StuckDefer in modes || BadMode.StuckDeferWithProgress in modes) {
                    analyze(context.sourceSet.module) {
                        val targets =
                            classesWith(context.newFiles, TpAnnotations.BAD_TARGET).map { it.second } +
                                context.deferred.mapNotNull { it.pointer.restoreSymbol() as? KaClassLikeSymbol }
                        @Suppress("UNCHECKED_CAST")
                        targets.distinctBy { it.classId }.forEach {
                            context.defer(
                                it.createPointer() as KaSymbolPointer<KaSymbol>,
                                "stuck",
                            )
                        }
                    }
                }
            }
        }
}

// ---- TP-Probe（見えたものを記録する） ----

public class ProbeNativeProvider : TpNativeProvider(TpId.Probe) {
    override fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor {
        val instance = Integer.toHexString(System.identityHashCode(Any()))
        val unit = environment.executionUnit

        fun record(event: ProbeEvent) {
            options.probeOut?.let { ProbeEvent.append(it, event) }
        }
        record(
            ProbeEvent(
                "create",
                "native",
                instance,
                unit = unit.name,
                options = environment.options,
                extra =
                    mapOf(
                        "kotlinVersion" to environment.kotlinVersion,
                        "frameworkVersion" to environment.frameworkVersion,
                        "unitKind" to if (unit is ExecutionUnit.CompilationUnit) "compilation" else "sourceSet",
                    ),
            ),
        )
        return object : AnalysisCodegenProcessor {
            private var lastRound = 0

            override fun process(context: AnalysisCodegenRoundContext) {
                lastRound = context.round
                if (context.round == 1) emitLogs(context.logger, context.allFiles.firstOrNull())
                val deferred =
                    analyze(context.sourceSet.module) {
                        context.deferred.map { d ->
                            d.payload
                                ?: d.pointer.restoreSymbol().toString()
                        }
                    }
                record(
                    ProbeEvent(
                        "process",
                        "native",
                        instance,
                        unit = unit.name,
                        round = context.round,
                        sourceSet = context.sourceSet.name,
                        compilation = context.compilation?.let { "${it.targetName}/${it.compilationName}" },
                        platform = platformName(context.platform),
                        options = context.options,
                        allFiles = context.allFiles.map { it.virtualFilePath },
                        newFiles = context.newFiles.map { it.virtualFilePath },
                        changedFiles = context.changedFiles.map { it.virtualFilePath },
                        removedFilePaths = context.removedFilePaths.sorted(),
                        javaFiles = context.allJavaFiles.map { it.virtualFile.path },
                        deferred = deferred,
                        extra =
                            mapOf(
                                "ownFiles" to
                                    context.sourceSet.ownFiles.size
                                        .toString(),
                                "visibleFiles" to
                                    context.sourceSet.visibleFiles.size
                                        .toString(),
                                "dependsOn" to context.sourceSet.dependencies.joinToString(",") { it.name },
                            ),
                    ),
                )
            }

            override fun finish() = record(ProbeEvent("finish", "native", instance, unit = unit.name, round = lastRound))

            override fun onError() = record(ProbeEvent("onError", "native", instance, unit = unit.name, round = lastRound))

            private fun emitLogs(
                logger: CodegenLogger,
                anchor: KtFile?,
            ) {
                options.probeLogs.forEach { log ->
                    when (log.level) {
                        "logging" -> logger.logging(log.message, anchor)
                        "info" -> logger.info(log.message, anchor)
                        "warn" -> logger.warn(log.message, anchor)
                        "error" -> logger.error(log.message, anchor)
                    }
                }
            }
        }
    }
}

package me.tbsten.analysiscodegen.testprocessors.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import me.tbsten.analysiscodegen.testprocessors.ActualFunction
import me.tbsten.analysiscodegen.testprocessors.ActualModel
import me.tbsten.analysiscodegen.testprocessors.ActualRender
import me.tbsten.analysiscodegen.testprocessors.BadMode
import me.tbsten.analysiscodegen.testprocessors.BadRender
import me.tbsten.analysiscodegen.testprocessors.ChainModel
import me.tbsten.analysiscodegen.testprocessors.ChainRender
import me.tbsten.analysiscodegen.testprocessors.DeferModel
import me.tbsten.analysiscodegen.testprocessors.DeferRender
import me.tbsten.analysiscodegen.testprocessors.MapperModel
import me.tbsten.analysiscodegen.testprocessors.MapperProperty
import me.tbsten.analysiscodegen.testprocessors.MapperRender
import me.tbsten.analysiscodegen.testprocessors.PlatformNames
import me.tbsten.analysiscodegen.testprocessors.ProbeEvent
import me.tbsten.analysiscodegen.testprocessors.RegistryModel
import me.tbsten.analysiscodegen.testprocessors.RegistryRender
import me.tbsten.analysiscodegen.testprocessors.ResourceModel
import me.tbsten.analysiscodegen.testprocessors.ResourceRender
import me.tbsten.analysiscodegen.testprocessors.TpAnnotations
import me.tbsten.analysiscodegen.testprocessors.TpId
import me.tbsten.analysiscodegen.testprocessors.TpOptions

// 各 TP の振る舞いの定義は ADR-027 と test-processors/README.md。Native 版（test-processors/native）と生成物を一致させる

// ---- TP-Mapper（isolating） ----

public class MapperKspProvider : TpKspProvider(TpId.Mapper) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor =
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.classesWith(TpAnnotations.MAPPED).forEach { cls ->
                    environment.codeGenerator.write(Dependencies(false, cls.containingFile!!), MapperRender.render(cls.mapperModel()))
                }
                return emptyList()
            }
        }
}

internal fun KSClassDeclaration.mapperModel(): MapperModel =
    MapperModel(
        packageName = packageName.asString(),
        classNameInPackage = nameInPackage,
        properties =
            primaryConstructor
                ?.parameters
                .orEmpty()
                .filter { it.isVal || it.isVar }
                .map { MapperProperty(it.name!!.asString(), it.type.resolve().notation()) },
    )

// ---- TP-Registry（aggregating。ラウンド 1 だけ） ----

public class RegistryKspProvider : TpKspProvider(TpId.Registry) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor =
        object : SymbolProcessor {
            private var done = false

            override fun process(resolver: Resolver): List<KSAnnotated> {
                if (done) return emptyList()
                done = true
                val classes = resolver.classesWith(TpAnnotations.REGISTERED)
                if (classes.isEmpty()) return emptyList()
                val model =
                    RegistryModel(
                        packageName = options.registryPackage,
                        name = options.registryName,
                        entries = classes.mapNotNull { it.qualifiedName?.asString() },
                        platform = platformName(environment.platforms),
                    )
                val files = classes.mapNotNull { it.containingFile }.distinct().toTypedArray()
                environment.codeGenerator.write(Dependencies(true, *files), RegistryRender.render(model))
                return emptyList()
            }
        }
}

// ---- TP-Chain（ラウンドをまたいで N 段） ----

public class ChainKspProvider : TpKspProvider(TpId.Chain) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor =
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.classesWith(TpAnnotations.CHAIN).forEach { cls ->
                    val ann = cls.annotation(TpAnnotations.CHAIN)!!
                    val model =
                        ChainModel(
                            packageName = cls.packageName.asString(),
                            classNameInPackage = cls.nameInPackage,
                            depth = ann.argument("depth") as Int,
                            step = ann.argument("step") as? Int ?: 0,
                            origin = ann.argument("origin") as? String ?: "",
                        )
                    ChainRender.render(model)?.let { environment.codeGenerator.write(Dependencies(false, cls.containingFile!!), it) }
                }
                return emptyList()
            }
        }
}

// ---- TP-Actual（プラットフォームのコンパイルだけ。ラウンド 1 だけ） ----

public class ActualKspProvider : TpKspProvider(TpId.Actual) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor =
        object : SymbolProcessor {
            private var done = false

            override fun process(resolver: Resolver): List<KSAnnotated> {
                val platform = platformName(environment.platforms)
                if (done || platform == PlatformNames.COMMON) return emptyList()
                done = true
                resolver
                    .getSymbolsWithAnnotation(TpAnnotations.GENERATE_ACTUAL)
                    .filterIsInstance<KSFunctionDeclaration>()
                    .filter { it.isExpect }
                    .groupBy { it.containingFile!! }
                    .toSortedMap(compareBy { it.filePath })
                    .forEach { (file, functions) ->
                        val supported =
                            functions.filter { fn ->
                                val ok = fn.parameters.isEmpty() && fn.returnType?.resolve()?.notation() == "kotlin.String"
                                if (!ok) environment.logger.error("TP-Actual は `expect fun name(): String` だけを扱う", fn)
                                ok
                            }
                        val model =
                            ActualModel(
                                packageName = file.packageName.asString(),
                                sourceFileBaseName = file.fileName.removeSuffix(".kt"),
                                functions =
                                    supported.map {
                                        ActualFunction(
                                            it.simpleName.asString(),
                                            if (Modifier.INTERNAL in
                                                it.modifiers
                                            ) {
                                                "internal"
                                            } else {
                                                "public"
                                            },
                                        )
                                    },
                                platform = platform,
                            )
                        environment.codeGenerator.write(Dependencies(false, file), ActualRender.render(model))
                    }
                return emptyList()
            }
        }
}

// ---- TP-Defer（解決できるまで持ち越す） ----

public class DeferKspProvider : TpKspProvider(TpId.Defer) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor =
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                val waiting = mutableListOf<KSAnnotated>()
                resolver.classesWith(TpAnnotations.DEFER_UNTIL).forEach { cls ->
                    val target = cls.annotation(TpAnnotations.DEFER_UNTIL)!!.argument("target") as String
                    if (resolver.getClassDeclarationByName(resolver.getKSNameFromString(target)) == null) {
                        waiting += cls
                    } else {
                        val model = DeferModel(cls.packageName.asString(), cls.nameInPackage, target)
                        environment.codeGenerator.write(Dependencies(false, cls.containingFile!!), DeferRender.render(model))
                    }
                }
                return waiting
            }
        }
}

// ---- TP-Resource（Java・リソース・次のラウンドで Kotlin） ----

public class ResourceKspProvider : TpKspProvider(TpId.Resource) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor =
        object : SymbolProcessor {
            private var round = 0
            private val pending = mutableListOf<Pair<ResourceModel, KSClassDeclaration>>()

            override fun process(resolver: Resolver): List<KSAnnotated> {
                round++
                // 前のラウンドで作った Java を Kotlin から参照する（RND-06）
                val ready =
                    pending.filter { (model, _) ->
                        resolver.getClassDeclarationByName(resolver.getKSNameFromString(ResourceRender.javaFqn(model))) !=
                            null
                    }
                ready.forEach { (model, cls) ->
                    environment.codeGenerator.write(Dependencies(false, cls.containingFile!!), ResourceRender.kotlinRef(model))
                }
                pending -= ready.toSet()

                val classes = resolver.classesWith(TpAnnotations.GENERATE_JAVA)
                classes.forEach { cls ->
                    val model = ResourceModel(cls.packageName.asString(), cls.nameInPackage)
                    val deps = Dependencies(false, cls.containingFile!!)
                    environment.codeGenerator.write(deps, ResourceRender.java(model))
                    environment.codeGenerator.write(deps, ResourceRender.properties(model))
                    pending += model to cls
                }
                if (round == 1 && classes.isNotEmpty()) {
                    val models = classes.map { ResourceModel(it.packageName.asString(), it.nameInPackage) }
                    val files = classes.mapNotNull { it.containingFile }.distinct().toTypedArray()
                    environment.codeGenerator.write(Dependencies(true, *files), ResourceRender.services(models))
                }
                return emptyList()
            }

            override fun finish() {
                pending.forEach { (model, cls) ->
                    environment.logger.error("TP-Resource: ${ResourceRender.javaFqn(model)} が次のラウンドで解決できなかった", cls)
                }
            }
        }
}

// ---- TP-Bad（わざと壊れる） ----

public class BadKspProvider : TpKspProvider(TpId.Bad) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor =
        object : SymbolProcessor {
            private var round = 0

            override fun process(resolver: Resolver): List<KSAnnotated> {
                round++
                val gen = environment.codeGenerator
                val modes = options.badModes
                val targets = resolver.classesWith(TpAnnotations.BAD_TARGET)
                val deferred = mutableListOf<KSAnnotated>()
                if (round == 1) {
                    if (BadMode.Collide in modes) {
                        resolver.classesWith(TpAnnotations.MAPPED).forEach { cls ->
                            gen.write(
                                Dependencies(false, cls.containingFile!!),
                                BadRender.collide(cls.packageName.asString(), cls.nameInPackage),
                            )
                        }
                    }
                    if (BadMode.Duplicate in modes) {
                        gen.write(Dependencies(false), BadRender.simple("BadDuplicate"))
                        gen.write(Dependencies(false), BadRender.simple("BadDuplicate"))
                    }
                    if (BadMode.WriteOutside in modes) {
                        gen.write(Dependencies(false), BadRender.simple("../../../escaped/BadOutside", packageName = ""))
                    }
                    if (BadMode.LogError in modes) {
                        environment.logger.error(BadRender.LOG_ERROR_MESSAGE, targets.firstOrNull())
                    }
                    if (BadMode.Throw in modes) {
                        gen.write(Dependencies(false), BadRender.simple("BadPartial"))
                        throw IllegalStateException(BadRender.THROW_MESSAGE)
                    }
                }
                val underLimit = round <= options.badSafetyLimit
                if ((BadMode.InfiniteRounds in modes || BadMode.StuckDeferWithProgress in modes) && underLimit) {
                    gen.write(Dependencies(false), BadRender.simple("BadRound$round"))
                }
                if ((BadMode.StuckDefer in modes || BadMode.StuckDeferWithProgress in modes) && underLimit) {
                    deferred += targets
                }
                return deferred
            }
        }
}

// ---- TP-Probe（見えたものを記録する） ----

public class ProbeKspProvider : TpKspProvider(TpId.Probe) {
    override fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor {
        val instance = Integer.toHexString(System.identityHashCode(Any()))
        val platform = platformName(environment.platforms)

        fun record(event: ProbeEvent) {
            options.probeOut?.let { ProbeEvent.append(it, event) }
        }
        record(
            ProbeEvent(
                "create",
                "ksp",
                instance,
                platform = platform,
                options = environment.options,
                extra =
                    mapOf(
                        "kotlinVersion" to environment.kotlinVersion.toString(),
                    ),
            ),
        )
        return object : SymbolProcessor {
            private var round = 0

            override fun process(resolver: Resolver): List<KSAnnotated> {
                round++
                if (round == 1) {
                    val anchor = resolver.getAllFiles().firstOrNull()
                    options.probeLogs.forEach { log ->
                        when (log.level) {
                            "logging" -> environment.logger.logging(log.message, anchor)
                            "info" -> environment.logger.info(log.message, anchor)
                            "warn" -> environment.logger.warn(log.message, anchor)
                            "error" -> environment.logger.error(log.message, anchor)
                        }
                    }
                }
                val all = resolver.getAllFiles().toList()
                record(
                    ProbeEvent(
                        "process",
                        "ksp",
                        instance,
                        round = round,
                        platform = platform,
                        options = environment.options,
                        allFiles = all.filter { it.fileName.endsWith(".kt") }.map { it.filePath }.sorted(),
                        newFiles =
                            resolver
                                .getNewFiles()
                                .filter { it.fileName.endsWith(".kt") }
                                .map { it.filePath }
                                .toList()
                                .sorted(),
                        javaFiles = all.filter { it.fileName.endsWith(".java") }.map { it.filePath }.sorted(),
                    ),
                )
                return emptyList()
            }

            override fun finish() = record(ProbeEvent("finish", "ksp", instance, round = round, platform = platform))

            override fun onError() = record(ProbeEvent("onError", "ksp", instance, round = round, platform = platform))
        }
    }
}

package p01

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import ksp.com.intellij.openapi.util.Disposer
import ksp.org.jetbrains.kotlin.analysis.api.KaSession
import ksp.org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import ksp.org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import ksp.org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import ksp.org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import ksp.org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import ksp.org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import ksp.org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import ksp.org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import ksp.org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import ksp.org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import ksp.org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilder
import ksp.org.jetbrains.kotlin.analysis.project.structure.builder.KtLibraryModuleBuilder
import ksp.org.jetbrains.kotlin.analysis.project.structure.builder.KtSdkModuleBuilder
import ksp.org.jetbrains.kotlin.analysis.project.structure.builder.KtSourceModuleBuilder
import ksp.org.jetbrains.kotlin.idea.references.KtReference
import ksp.org.jetbrains.kotlin.lexer.KtTokens
import ksp.org.jetbrains.kotlin.platform.SimplePlatform
import ksp.org.jetbrains.kotlin.platform.TargetPlatform
import ksp.org.jetbrains.kotlin.platform.js.JsPlatforms
import ksp.org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import ksp.org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import ksp.org.jetbrains.kotlin.psi.KtDeclaration
import ksp.org.jetbrains.kotlin.psi.KtCallExpression
import ksp.org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import ksp.org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import ksp.org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import ksp.org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import ksp.org.jetbrains.kotlin.config.LanguageVersion
import ksp.org.jetbrains.kotlin.config.ApiVersion
import ksp.org.jetbrains.kotlin.config.LanguageFeature
import ksp.org.jetbrains.kotlin.psi.KtFile
import ksp.org.jetbrains.kotlin.psi.KtNameReferenceExpression
import ksp.org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SourceSetModel(
    val name: String,
    val platforms: List<String>,
    val sourceRoots: List<String>,
    val dependsOn: List<String>,
    val classpath: List<String>,
)

@Serializable
data class ProjectModel(val jdkHome: String, val sourceSets: List<SourceSetModel>)

fun platformOf(names: List<String>): TargetPlatform {
    val simple: Set<SimplePlatform> = names.flatMapTo(mutableSetOf()) { n ->
        when (n) {
            "jvm" -> JvmPlatforms.defaultJvmPlatform.componentPlatforms
            "js" -> JsPlatforms.defaultJsPlatform.componentPlatforms
            "wasm" -> WasmPlatforms.wasmJs.componentPlatforms
            else -> error("unknown platform $n")
        }
    }
    return TargetPlatform(simple)
}

/**
 * 使い方: analyzer <model.json> <out-dir> [mode]
 *  mode = isolated（既定）: SourceSet ごとに「その SourceSet と dependsOn の推移閉包だけ」で Session を作る
 *  mode = whole: 全 SourceSet を 1 つの Session に入れ、各 SourceSet を解析する
 */
fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    try { mainImpl(args) } catch (t: Throwable) { t.printStackTrace(); kotlin.system.exitProcess(1) }
}

fun mainImpl(args: Array<String>) {
    val model = Json { ignoreUnknownKeys = true }.decodeFromString<ProjectModel>(File(args[0]).readText())
    val outDir = File(args[1]).apply { mkdirs() }
    val mode = args.getOrElse(2) { "isolated" }
    val byName = model.sourceSets.associateBy { it.name }
    val baseDir = File(model.sourceSets.first().sourceRoots.first().substringBefore("/src/")) // sample/

    fun closure(name: String): List<String> {
        // 後順（dependsOn 先が先）＝ 上位（commonMain）から
        val seen = HashSet<String>(); val order = ArrayList<String>()
        fun go(n: String) { if (seen.add(n)) { byName.getValue(n).dependsOn.forEach(::go); order += n } }
        go(name)
        return order
    }

    val targets = model.sourceSets.map { it.name }
    val report = StringBuilder()
    if (mode == "whole") {
        val t0 = System.nanoTime()
        analyzeSession(model, targets.flatMap(::closure).distinct(), targets, baseDir, report)
        report.appendLine("# session-build+analysis ms: ${(System.nanoTime() - t0) / 1_000_000}")
    } else {
        for (t in targets) {
            val t0 = System.nanoTime()
            analyzeSession(model, closure(t), listOf(t), baseDir, report)
            report.appendLine("# [$t] session-build+analysis ms: ${(System.nanoTime() - t0) / 1_000_000}")
        }
    }
    File(outDir, "aa-$mode.txt").writeText(report.toString())
    println(report)
    kotlin.system.exitProcess(0)
}

fun analyzeSession(
    model: ProjectModel,
    moduleNames: List<String>,
    analyzeTargets: List<String>,
    baseDir: File,
    report: StringBuilder,
) {
    val byName = model.sourceSets.associateBy { it.name }
    val disposable = Disposer.newDisposable("p01")
    try {
        val sourceModules = LinkedHashMap<String, KaSourceModule>()
        // 注: aa-embeddable は再配置時に *.kotlin_module を落としているため、Kotlin から
        // トップレベル関数（buildStandaloneAnalysisAPISession / buildKtSourceModule / analyze など）が見えない。
        // ビルダーのコンストラクタとメンバー関数を直接使う。
        val session = StandaloneAnalysisAPISessionBuilder(disposable, false).apply {
            // aa-embeddable では XML 由来のアプリケーションサービス登録の一部が効かない（KSP2 も手で登録している）
            val p = "ksp.org.jetbrains.kotlin.analysis."
            registerAppServiceIfMissing(application, p + "api.permissions.KaAnalysisPermissionRegistry", p + "api.impl.base.permissions.KaBaseAnalysisPermissionRegistry")
            registerAppServiceIfMissing(application, p + "api.platform.permissions.KotlinAnalysisPermissionOptions", p + "api.standalone.base.permissions.KotlinStandaloneAnalysisPermissionOptions")
            registerAppServiceIfMissing(application, p + "api.platform.resolution.KaResolutionActivityTracker", p + "low.level.api.fir.lazy.resolve.LLFirResolutionActivityTracker")
            buildKtModuleProvider {
                val env = coreApplicationEnvironment
                val prj = project
                // 全モジュールの和集合（Provider の既定 platform）
                platform = platformOf(moduleNames.flatMap { byName.getValue(it).platforms }.distinct())
                val libs = HashMap<String, KaLibraryModule>()
                fun lib(path: String, ownerPlatforms: List<String>): KaLibraryModule = libs.getOrPut(path) {
                    addModule(KtLibraryModuleBuilder(env, prj, false).apply {
                        platform = platformOf(ownerPlatforms)
                        addBinaryRoot(Paths.get(path))
                        libraryName = File(path).name
                    }.build())
                }
                val jdk: KaLibraryModule by lazy {
                    addModule(KtSdkModuleBuilder(env, prj).apply {
                        platform = JvmPlatforms.defaultJvmPlatform
                        addBinaryRootsFromJdkHome(Paths.get(model.jdkHome), false)
                        libraryName = "JDK"
                    }.build())
                }
                // 上位から順に作る（dependsOn 先が先にできている必要がある）
                for (name in moduleNames) {
                    val ss = byName.getValue(name)
                    val m = KtSourceModuleBuilder(env, prj).apply {
                        moduleName = name
                        // KGP が付ける -Xmulti-platform 相当
                        languageVersionSettings = LanguageVersionSettingsImpl(
                            LanguageVersion.LATEST_STABLE,
                            ApiVersion.LATEST_STABLE,
                            specificFeatures = mapOf(LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED),
                        )
                        platform = platformOf(ss.platforms)
                        ss.sourceRoots.forEach { addSourceRoot(Paths.get(it)) }
                        // KGP は上位 SourceSet の metadata 出力（build/classes/...）も classpath に入れるが、
                        // ここでは dependsOn のソースモジュールで表すので除く
                        ss.classpath.filterNot { "/build/classes/" in it }.forEach { addRegularDependency(lib(it, ss.platforms)) }
                        if (ss.platforms == listOf("jvm")) addRegularDependency(jdk)
                        ss.dependsOn.forEach { d -> addDependsOnDependency(sourceModules.getValue(d)) }
                    }.build()
                    sourceModules[name] = addModule(m)
                }
            }
        }.build()
        val provider = KaSessionProvider.getInstance(session.project)
        val files = session.modulesWithFiles
        for (t in analyzeTargets) {
            val module = sourceModules.getValue(t)
            val ktFiles = files.getValue(module).filterIsInstance<KtFile>().sortedBy { it.virtualFilePath }
            report.appendLine("## sourceSet=$t modules=${moduleNames} files=${ktFiles.size}")
            for (f in ktFiles) {
                val rel = File(f.virtualFilePath).relativeTo(baseDir).path
                provider.analyze(f) {
                    for (d in f.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)) {
                        if (d.severity != KaSeverity.ERROR) continue
                        val off = d.textRanges.first().startOffset
                        val (line, col) = lineCol(f.text, off)
                        report.appendLine("E $t $rel:$line:$col ${d.factoryName} | ${d.defaultMessage.lineSequence().first()}")
                    }
                    dumpResolution(f, rel, t, report)
                }
            }
        }
    } finally {
        Disposer.dispose(disposable)
    }
}

@Suppress("UNCHECKED_CAST")
fun registerAppServiceIfMissing(app: ksp.com.intellij.openapi.application.Application, iface: String, impl: String) {
    val mock = app as ksp.com.intellij.mock.MockApplication
    val i = Class.forName(iface) as Class<Any>
    if (mock.getServiceIfCreated(i) == null && !mock.hasComponent(i)) mock.javaClass.getMethod("registerService", Class::class.java, Class::class.java).invoke(mock, i, Class.forName(impl))
}

fun lineCol(text: String, offset: Int): Pair<Int, Int> {
    var line = 1; var col = 1
    for (i in 0 until offset) { if (text[i] == '\n') { line++; col = 1 } else col++ }
    return line to col
}

fun KaSession.describe(s: KaSymbol?): String {
    if (s == null) return "<unresolved>"
    val id = when (s) {
        is ksp.org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol -> "${s.containingClassId}.<init>"
        is ksp.org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol -> "param:${s.name}"
        is KaCallableSymbol -> s.callableId?.toString() ?: s.toString()
        is KaClassLikeSymbol -> s.classId?.toString() ?: s.toString()
        else -> s.toString()
    }
    val mod = s.containingModule
    val modName = when (mod) {
        is KaSourceModule -> mod.name
        // Gradle の変換済み KLIB 名の末尾ハッシュ（-wJOvIw など）と拡張子を落とす
        is KaLibraryModule -> "lib:" + mod.libraryName.replace(Regex("""(-[A-Za-z0-9_]{6})?\.(klib|jar)$"""), "")
        else -> mod.javaClass.simpleName
    }
    // ソース宣言なら実際のファイル名も付ける（containingModule と PSI の所属がずれないかを見る）
    val file = s.psi?.containingFile?.name?.let { "[$it]" } ?: ""
    val ea = (s as? KaDeclarationSymbol)?.let { if (it.isExpect) " expect" else if (it.isActual) " actual" else "" } ?: ""
    return "$id@$modName$file$ea"
}

fun KaSession.dumpResolution(f: KtFile, rel: String, t: String, report: StringBuilder) {
    // 参照 → 解決先（FQ 名 @ 所属モジュール）
    f.accept(object : KtTreeVisitorVoid() {
        override fun visitReferenceExpression(expression: ksp.org.jetbrains.kotlin.psi.KtReferenceExpression) {
            super.visitReferenceExpression(expression)
            if (expression !is KtNameReferenceExpression) return
            // 注: standalone では PSI の reference provider が登録されず references が空になるため、resolveToCall で解決先を取る
            val target = (expression.parent as? KtCallExpression)?.takeIf { it.calleeExpression == expression } ?: expression
            val info = try { target.resolveToCall() } catch (e: Exception) { null } ?: return
            val sym: KaSymbol? = when (info) {
                is KaSuccessCallInfo -> (info.call as? KaCallableMemberCall<*, *>)?.partiallyAppliedSymbol?.signature?.symbol
                is KaErrorCallInfo -> null
            }
            if (info is KaSuccessCallInfo && info.call !is KaCallableMemberCall<*, *>) return
            val (line, col) = lineCol(f.text, expression.textOffset)
            report.appendLine("R $t $rel:$line:$col ${expression.getReferencedName()} -> ${describe(sym)}")
        }

        override fun visitDeclaration(dcl: KtDeclaration) {
            super.visitDeclaration(dcl)
            if (!dcl.hasModifier(KtTokens.ACTUAL_KEYWORD)) return
            val sym = dcl.symbol
            val expects = sym.getExpectsForActual().map { describe(it) }
            val (line, col) = lineCol(f.text, dcl.textOffset)
            report.appendLine("A $t $rel:$line:$col ${dcl.name} expects=$expects")
        }
    })
}

package me.tbsten.analysis.code.gen.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.util.Properties

/**
 * Worker に載せるエンジンと AA 一式を解決する（ADR-011、ADR-012、ADR-063）。
 *
 * - `analysisCodeGenRuntime`: analysis-code-gen-processing-engine（plugin と同じ版）と、AA 一式（エンジンをビルドした版。ADR-063）
 * - KSP Bridge（analysis-code-gen-ksp-bridge）: KSP の Processor があるときだけ解決する。Configuration には登録しない（ADR-050・074）
 *
 * AA 一式の座標は、ビルド時に version catalog の bundle `aa-runtime` から書き出した resource を読む。
 */
internal class AnalysisCodegenRuntime(
    private val project: Project,
    private val extension: AnalysisCodegenExtension,
) {
    private val buildInfo: Properties =
        Properties().also { props ->
            val stream =
                AnalysisCodegenRuntime::class.java.getResourceAsStream(BUILD_INFO)
                    ?: error("analysisCodeGen: plugin の $BUILD_INFO が無い（plugin のビルドが壊れている）")
            stream.use(props::load)
        }

    /** plugin 自身の版。エンジンも同じ版を使う。 */
    val pluginVersion: String = buildInfo.getProperty("version")

    /**
     * 実行する AA の版。DSL の指定が無ければ、エンジンをビルドした AA の版（catalog の版）。
     * エンジンは 1 つの AA の版に対してだけコンパイルされていて、版ごとの compat モジュール（ADR-011）が入るまでは
     * 利用者の Kotlin の版の AA を載せると NoClassDefFoundError になるため（ADR-063）。
     */
    val analysisApiVersion: Provider<String> =
        extension.analysisApiVersion.orElse(buildInfo.getProperty("analysisApiVersion"))

    val runtime: Configuration =
        project.configurations.create(RUNTIME_CONFIGURATION) { c ->
            c.description = "analysisCodeGen のエンジンと Analysis API 一式（Worker のクラスパス）"
            c.isCanBeConsumed = false
            c.isCanBeResolved = true
            c.isVisible = false
            c.jvmRuntimeAttributes()
            c.dependencies.addLater(
                project.provider { project.dependencies.create("$GROUP:analysis-code-gen-processing-engine:$pluginVersion") },
            )
            c.dependencies.addAllLater(analysisApiVersion.map(::analysisApiDependencies))
        }

    /**
     * KSP Bridge。KSP の Processor があるときだけ Worker に載せる。
     * ⭐️ プロジェクトの Configuration に登録しない（detached）。登録すると `dependencies` に KSP API が現れ、
     * Bridge が無効なプロジェクトの依存に KSP API が含まれてしまう（BRG-14、ADR-074）。
     */
    val kspBridgeRuntime: Configuration by lazy {
        project.configurations
            .detachedConfiguration(project.dependencies.create("$GROUP:analysis-code-gen-ksp-bridge:$pluginVersion"))
            .also { c ->
                c.isCanBeConsumed = false
                c.isCanBeResolved = true
                c.jvmRuntimeAttributes()
            }
    }

    /**
     * AA 一式。`*-for-ide` は推移的な依存を宣言していないため、bundle に全部を並べて非推移で解決する（acg.analysis-api と同じ扱い）。
     * catalog の AA の版で書かれた座標だけを、実行する AA の版に置き換える。
     *
     * 取得元（JetBrains の 2 つのリポジトリ）は利用者が settings に書く（docs のインストール）。plugin がプロジェクトに
     * リポジトリを足すと、Gradle は settings のリポジトリを無視してしまうため足さない（ADR-063）。
     */
    private fun analysisApiDependencies(version: String): List<Dependency> {
        val catalogVersion = buildInfo.getProperty("analysisApiVersion")
        return buildInfo
            .getProperty("aaRuntime")
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { coordinate ->
                val (group, name, v) = coordinate.split(':')
                val resolvedVersion = if (group == "org.jetbrains.kotlin" && v == catalogVersion) version else v
                project.dependencies.create("$group:$name:$resolvedVersion").also { dep ->
                    (dep as? org.gradle.api.artifacts.ModuleDependency)?.isTransitive = false
                }
            }
    }

    /** Processor の Configuration を作る。JVM の実行時の形（JAR）で解決する。 */
    fun processorConfiguration(name: String): Configuration =
        project.configurations.maybeCreate(name).also { c ->
            c.description = "analysisCodeGen の Processor（$name）"
            c.isCanBeConsumed = false
            c.isCanBeResolved = true
            c.jvmRuntimeAttributes()
        }

    private fun Configuration.jvmRuntimeAttributes() {
        attributes { a ->
            a.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
            a.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.LIBRARY))
            a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements::class.java, LibraryElements.JAR))
            a.attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
            a.attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                project.objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
            )
            a.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        }
    }

    companion object {
        const val GROUP: String = "me.tbsten.analysis.code.gen"
        const val RUNTIME_CONFIGURATION: String = "analysisCodeGenRuntime"
        private const val BUILD_INFO = "/me/tbsten/analysis/code/gen/gradle/acg-build-info.properties"
    }
}

package me.tbsten.analysis.code.gen.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * `analysisCodeGen { }`（ADR-016）。
 *
 * ```kotlin
 * analysisCodeGen {
 *     arg("key", "value")
 *     sourceSets { commonMain { processor("example:p:1.0") } }
 *     compilations { all { processor("example:registry:1.0") } }
 * }
 * ```
 */
public abstract class AnalysisCodegenExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /** 実行する AA のバージョン。省略時は利用者の KGP と同じ（ADR-011）。 */
        public abstract val analysisApiVersion: Property<String>

        /** 既定 100（ADR-014）。 */
        public abstract val maxRounds: Property<Int>

        /** 最終ラウンドで Deferred が残ったときの扱い。既定 [DeferredSeverity.Error]。 */
        public abstract val unresolvedDeferred: Property<DeferredSeverity>

        /** 全実行単位に渡すオプション。実行単位の指定が優先する。 */
        public abstract val args: MapProperty<String, String>

        public fun arg(
            key: String,
            value: String,
        ) {
            args.put(key, value)
        }

        @get:Nested
        public abstract val worker: WorkerSpec

        /**
         * KSP Bridge の設定（ADR-050）。KSP の Processor が依存にあるときだけ使われ、無ければ何も起きない。
         * Bridge の JAR は、KSP の Processor があるときだけ Worker のクラスパスに足す（座標の文字列で扱い、
         * この module は Bridge のクラスを import しない。ADR-012）。
         */
        @get:Nested
        public abstract val kspBridge: KspBridgeSpec

        public fun kspBridge(action: Action<in KspBridgeSpec>) {
            action.execute(kspBridge)
        }

        public fun worker(action: Action<in WorkerSpec>) {
            action.execute(worker)
        }

        /**
         * Kotlin の SourceSet を遅延で写したもの。Android（AGP 単体）の SourceSet は含まない。
         * `commonMain { }` などよく使う名前は、Kotlin の SourceSet がまだ作られていなくても書けるようメンバーで持つ（ADR-063）。
         */
        public val sourceSets: AnalysisCodegenSourceSets =
            AnalysisCodegenSourceSets(objects.domainObjectContainer(AnalysisCodegenSourceSetSpec::class.java))

        public fun sourceSets(action: Action<in AnalysisCodegenSourceSets>) {
            action.execute(sourceSets)
        }

        /** Compilation（`<target><Compilation>`、Android は Variant）を遅延で写したもの。 */
        public val compilations: NamedDomainObjectContainer<AnalysisCodegenCompilationSpec> =
            objects.domainObjectContainer(AnalysisCodegenCompilationSpec::class.java)

        public fun compilations(action: Action<in NamedDomainObjectContainer<AnalysisCodegenCompilationSpec>>) {
            action.execute(compilations)
        }

        init {
            maxRounds.convention(DEFAULT_MAX_ROUNDS)
            unresolvedDeferred.convention(DeferredSeverity.Error)
            worker.isolation.convention(WorkerIsolation.ClassLoader)
            kspBridge.processingMode.convention(KspProcessingMode.Compatible)
            kspBridge.kspVersionCheck.convention(true)
        }

        private companion object {
            const val DEFAULT_MAX_ROUNDS = 100
        }
    }

/**
 * `analysisCodeGen { sourceSets { } }` の入れ物。
 *
 * Kotlin DSL は、plugins { } を適用し終えた時点で存在する要素にしか `jvmMain { }` のような accessor を作らない。
 * KMP の `jvmMain` などは `kotlin { jvm() }` を評価してから作られるので、よく使う名前はメンバー関数で受ける（ADR-063）。
 * それ以外の名前は `named("x") { }`、`maybeCreate("x")`、`"x" { }` で書ける。
 */
public class AnalysisCodegenSourceSets internal constructor(
    private val delegate: NamedDomainObjectContainer<AnalysisCodegenSourceSetSpec>,
) : NamedDomainObjectContainer<AnalysisCodegenSourceSetSpec> by delegate {
    private fun spec(
        name: String,
        action: Action<in AnalysisCodegenSourceSetSpec>,
    ): AnalysisCodegenSourceSetSpec = maybeCreate(name).also(action::execute)

    public fun main(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("main", action)

    public fun test(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("test", action)

    public fun commonMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("commonMain", action)

    public fun commonTest(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("commonTest", action)

    public fun jvmMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("jvmMain", action)

    public fun jvmTest(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("jvmTest", action)

    public fun androidMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("androidMain", action)

    public fun jsMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("jsMain", action)

    public fun wasmJsMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("wasmJsMain", action)

    public fun nativeMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("nativeMain", action)

    public fun appleMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("appleMain", action)

    public fun iosMain(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("iosMain", action)

    public fun linuxX64Main(action: Action<in AnalysisCodegenSourceSetSpec>): AnalysisCodegenSourceSetSpec = spec("linuxX64Main", action)
}

public enum class DeferredSeverity { Error, Warning }

public enum class WorkerIsolation { ClassLoader, Process }

public abstract class WorkerSpec {
    /** 既定 [WorkerIsolation.ClassLoader]。 */
    public abstract val isolation: Property<WorkerIsolation>

    /** [WorkerIsolation.Process] のときだけ有効。例: `2g`。 */
    public abstract val maxHeapSize: Property<String>

    /** [WorkerIsolation.Process] のときだけ有効。 */
    public abstract val jvmArgs: org.gradle.api.provider.ListProperty<String>
}

/** `analysisCodeGen { kspBridge { } }`（ADR-050）。 */
public abstract class KspBridgeSpec {
    /** 既定 [KspProcessingMode.Compatible]。 */
    public abstract val processingMode: Property<KspProcessingMode>

    /** Processor がビルドされた KSP API のバージョンを検査する。既定 true。 */
    public abstract val kspVersionCheck: Property<Boolean>
}

public enum class KspProcessingMode {
    /** KSP2 と同じく、KSP Processor に Compilation の全ソースを見せる（既定）。 */
    Compatible,

    /** 実行単位の Processing Scope だけを見せる。共通の生成が 1 回で済む。 */
    SourceSet,
}

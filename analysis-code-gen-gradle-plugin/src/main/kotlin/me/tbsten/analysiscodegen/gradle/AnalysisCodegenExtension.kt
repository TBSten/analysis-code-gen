package me.tbsten.analysiscodegen.gradle

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
        ): Unit = TODO()

        @get:Nested
        public abstract val worker: WorkerSpec

        public fun worker(action: Action<in WorkerSpec>): Unit = TODO()

        /** Kotlin の SourceSet を遅延で写したもの。Android（AGP 単体）の SourceSet は含まない。 */
        public val sourceSets: NamedDomainObjectContainer<AnalysisCodegenSourceSetSpec> =
            objects.domainObjectContainer(AnalysisCodegenSourceSetSpec::class.java)

        public fun sourceSets(action: Action<in NamedDomainObjectContainer<AnalysisCodegenSourceSetSpec>>): Unit = TODO()

        /** Compilation（`<target><Compilation>`、Android は Variant）を遅延で写したもの。 */
        public val compilations: NamedDomainObjectContainer<AnalysisCodegenCompilationSpec> =
            objects.domainObjectContainer(AnalysisCodegenCompilationSpec::class.java)

        public fun compilations(action: Action<in NamedDomainObjectContainer<AnalysisCodegenCompilationSpec>>): Unit = TODO()
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

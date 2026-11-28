/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Ported-From: google/ksp@a3aaaabae570bae3ab4b84e72385483a14ab4498:kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/KotlinSymbolProcessing.kt
// Modified-By: analysis-code-gen contributors, 2026
// Modifications: ktlint で整形。AA 環境の構築（createAASession・registerProjectServices）、全ファイルの準備（prepareAllKSFiles）、生成物の差分反映（prepareNewKSFiles・dropCaches）だけを取り出した。KSPConfig の代わりに ProjectModel から SourceSet ごとの KaSourceModule と dependsOn を組む（ADR-009）。KS* の生成・ラウンドループ・増分は除いた（processing-engine 側）。
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UnstableApiUsage")

package me.tbsten.analysis.code.gen.analysis

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.diagnostic.PluginException
import com.intellij.diagnostic.PluginProblemReporter
import com.intellij.mock.MockProject
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.search.GlobalSearchScope
import me.tbsten.analysis.code.gen.model.CompilationModel
import me.tbsten.analysis.code.gen.model.PlatformKind
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.model.SourceSetModel
import org.jetbrains.kotlin.analysis.api.fir.utils.KaFirCacheCleaner
import org.jetbrains.kotlin.analysis.api.platform.KotlinMessageBusProvider
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.KotlinProjectMessageBusProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalSourceModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.platform.resolution.KaResolutionActivityTracker
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.standalone.KotlinStaticPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.KotlinStandalonePlatformSettings
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.permissions.KotlinStandaloneAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolutionActivityTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.project.structure.builder.KaModuleContainerBuilder
import org.jetbrains.kotlin.analysis.project.structure.impl.KotlinStandaloneProjectStructureProvider
import org.jetbrains.kotlin.cli.jvm.compiler.CliMetadataFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Path

/**
 * 1 実行単位の AA Standalone のセッション（KSP2 の createAASession 相当）。
 *
 * - 1 SourceSet = 1 [KaSourceModule]、dependsOn は `addDependsOnDependency`（ADR-009）
 * - ライブラリ（Compilation の classpath）・friend・JDK はそれぞれ 1 つの [KaLibraryModule] にし、全 SourceSet から参照する
 * - 生成物は [addFiles] で content scope と宣言 / パッケージの provider に足し、ソースのキャッシュだけを無効化する（ADR-010）
 */
internal class AaSession private constructor(
    private val lease: KotlinCoreProjectEnvironmentManager.Lease,
    val sourceModules: Map<String, KaSourceModule>,
    val libraryModules: List<KaLibraryModule>,
    private val javaFileManager: IncrementalJavaFileManager?,
) {
    val project: MockProject get() = lease.environment.project
    private val psiManager = PsiManager.getInstance(project)

    /** モジュールごとの現在の Kotlin / Java ファイル（初期のソース + これまでに足したファイル）。 */
    val kotlinFiles: Map<String, MutableList<KtFile>> =
        sourceModules.mapValues { (_, m) ->
            m.psiRoots.filterIsInstance<KtFile>().toMutableList()
        }
    val javaFiles: Map<String, MutableList<PsiJavaFile>> =
        sourceModules.mapValues { (_, m) -> m.psiRoots.filterIsInstance<PsiJavaFile>().toMutableList() }

    private var closed = false

    private fun prepareAllFiles() {
        val ktFiles = kotlinFiles.values.flatten()
        declarationFactory().update(ktFiles)
        packageFactory().update(ktFiles)
        javaFileManager?.initialize(sourceModules.values.toList() + libraryModules, javaFiles.values.flatten().toSet())
    }

    /** ソースのキャッシュを捨てる。ライブラリのキャッシュは残す（ADR-010 決定 1）。 */
    fun dropCaches() {
        IntelliJManager.maybeRunInWriteAction {
            project.publishGlobalSourceModuleStateModificationEvent()
            psiManager.dropPsiCaches()
        }
    }

    /**
     * ディスク上のファイルを [moduleName] のモジュールに足す（KSP2 の prepareNewKSFiles）。
     * 呼ぶ前に [dropCaches] を済ませておく。
     */
    fun addFiles(
        moduleName: String,
        paths: Collection<Path>,
    ): Pair<List<KtFile>, List<PsiJavaFile>> {
        val module = sourceModules.getValue(moduleName)
        val fs = StandardFileSystems.local()
        val psiFiles =
            paths.map { path ->
                val vFile = fs.refreshAndFindFileByPath(path.toString()) ?: error("ファイルが VFS に見つからない: $path")
                psiManager.findFile(vFile) ?: error("PSI を作れない: $path")
            }
        val ktFiles = psiFiles.filterIsInstance<KtFile>()
        val newJavaFiles = if (javaFileManager != null) psiFiles.filterIsInstance<PsiJavaFile>() else emptyList()

        // Add new files to content scope.
        val contentScope = module.contentScope as IncrementalGlobalSearchScope
        contentScope.addAll(ktFiles.map { it.virtualFile })
        contentScope.addAll(newJavaFiles.map { it.virtualFile })

        // Update Kotlin providers for newly generated source files.
        declarationFactory().update(ktFiles)
        packageFactory().update(ktFiles)

        // Update Java providers for newly generated source files.
        javaFileManager?.add(newJavaFiles.toSet())

        kotlinFiles.getValue(moduleName) += ktFiles
        javaFiles.getValue(moduleName) += newJavaFiles
        return ktFiles to newJavaFiles
    }

    fun close() {
        if (closed) return
        closed = true
        KotlinCoreProjectEnvironmentManager.close(lease)
    }

    private fun declarationFactory() =
        project.getService(KotlinDeclarationProviderFactory::class.java) as IncrementalKotlinDeclarationProviderFactory

    private fun packageFactory() = project.getService(KotlinPackageProviderFactory::class.java) as IncrementalKotlinPackageProviderFactory

    companion object {
        init {
            // We depend on swing (indirectly through PSI or something), so we want to declare headless mode,
            // to avoid accidentally starting the UI thread. But, don't set it if it was set externally.
            if (System.getProperty("java.awt.headless") == null) {
                System.setProperty("java.awt.headless", "true")
            }
            setupIdeaStandaloneExecution()
        }

        /** [extraFiles] は SourceSet 名ごとに足すファイル（ラウンドの生成物を作り直した環境に入れ直すとき）。 */
        fun create(
            model: ProjectModel,
            unitName: String,
            extraFiles: Map<String, List<Path>> = emptyMap(),
        ): AaSession {
            val lease = KotlinCoreProjectEnvironmentManager.open()
            try {
                return createAASession(lease, ModelView(model, unitName)).also { session ->
                    session.prepareAllFiles()
                    extraFiles.forEach { (module, paths) -> if (paths.isNotEmpty()) session.addFiles(module, paths) }
                }
            } catch (e: Throwable) {
                try {
                    KotlinCoreProjectEnvironmentManager.close(lease)
                } catch (suppressed: Throwable) {
                    e.addSuppressed(suppressed)
                }
                throw e
            }
        }

        private fun createAASession(
            lease: KotlinCoreProjectEnvironmentManager.Lease,
            view: ModelView,
        ): AaSession {
            val kotlinCoreProjectEnvironment = lease.environment
            val project: MockProject = kotlinCoreProjectEnvironment.project

            CoreApplicationEnvironment.registerExtensionPoint(
                project.extensionArea,
                KaResolveExtensionProvider.EP_NAME.name,
                KaResolveExtensionProvider::class.java,
            )

            val compilation = view.compilation
            val compilationPlatform = compilation.platform.toTargetPlatform(compilation.compilerOptions.jvmTarget)
            val isJvm = compilation.platform == PlatformKind.Jvm || compilation.platform == PlatformKind.Android
            val languageVersionSettings = view.languageVersionSettings()
            val sourceModules = linkedMapOf<String, KaSourceModule>()
            val libraryModules = mutableListOf<KaLibraryModule>()

            // replaces buildKtModuleProviderByCompilerConfiguration(compilerConfiguration)
            // AA 2.4.21 では KtModuleProviderBuilder が KaModuleContainerBuilder になり、Provider は別に作る
            val moduleContainer =
                KaModuleContainerBuilder(kotlinCoreProjectEnvironment.environment, project)
                    .apply {
                        val environment = kotlinCoreProjectEnvironment.environment
                        val libraries =
                            LibraryModuleBuilder(environment, project)
                                .apply {
                                    platform = compilationPlatform
                                    addBinaryRoots(compilation.classpath.map(view::resolve))
                                    libraryName = "Libraries for ${compilation.name}"
                                }.build()
                        val friends =
                            LibraryModuleBuilder(environment, project)
                                .apply {
                                    platform = compilationPlatform
                                    addBinaryRoots(compilation.friendPaths.map(view::resolve))
                                    libraryName = "Friends of ${compilation.name}"
                                }.build()
                        val jdk =
                            compilation.jdkHome?.takeIf { isJvm }?.let { jdkHome ->
                                SdkModuleBuilder(environment, project)
                                    .apply {
                                        platform = compilationPlatform
                                        addBinaryRootsFromJdkHome(view.resolve(jdkHome), isJre = false)
                                        libraryName = "JDK for ${compilation.name}"
                                    }.build()
                            }
                        libraryModules += listOfNotNull(libraries, friends, jdk)

                        // dependsOn の上流から順に作る（下流が上流の KaSourceModule を参照するため）
                        for (sourceSet in view.sourceSetsInDependencyOrder) {
                            val module =
                                SourceModuleBuilder(environment, project)
                                    .apply {
                                        this.languageVersionSettings = languageVersionSettings
                                        platform = sourceSet.platform.toTargetPlatform(compilation.compilerOptions.jvmTarget)
                                        moduleName = sourceSet.name
                                        addRegularDependency(libraries)
                                        addFriendDependency(friends)
                                        if (jdk != null && sourceSet.platform != PlatformKind.Common) addRegularDependency(jdk)
                                        sourceSet.dependsOn.forEach { addDependsOnDependency(sourceModules.getValue(it)) }
                                        addSourceRoots(view.sourceRoots(sourceSet, includeJava = isJvm))
                                    }.build()
                            sourceModules[sourceSet.name] = module
                            addModule(module)
                        }

                        this.platform = compilationPlatform
                    }.build()
            val projectStructureProvider = KotlinStandaloneProjectStructureProvider(compilationPlatform, project, moduleContainer)

            // register services and build session
            val modules = projectStructureProvider.allModules
            val allSourceFiles = projectStructureProvider.allSourceFiles
            StandaloneProjectFactory.registerServicesForProjectEnvironment(
                kotlinCoreProjectEnvironment,
                projectStructureProvider,
            )
            val ktFiles = allSourceFiles.filterIsInstance<KtFile>()
            val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(modules, kotlinCoreProjectEnvironment.environment)
            val createPackagePartProvider = StandaloneProjectFactory.createPackagePartsProvider(libraryRoots)

            kotlinCoreProjectEnvironment.registerApplicationServices(
                org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry::class.java,
                org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionRegistry::class.java,
            )
            kotlinCoreProjectEnvironment.registerApplicationServices(
                KotlinAnalysisPermissionOptions::class.java,
                KotlinStandaloneAnalysisPermissionOptions::class.java,
            )
            kotlinCoreProjectEnvironment.registerApplicationServices(
                KaResolutionActivityTracker::class.java,
                LLFirResolutionActivityTracker::class.java,
            )
            kotlinCoreProjectEnvironment.registerApplicationServices(
                PluginProblemReporter::class.java,
                NoopPluginProblemReporter::class.java,
            )

            val javaFileManager =
                registerProjectServices(
                    kotlinCoreProjectEnvironment,
                    ktFiles,
                    createPackagePartProvider,
                    libraryRoots.map { it.file }.distinct(),
                    isJvm,
                )

            CoreApplicationEnvironment.registerExtensionPoint(
                project.extensionArea,
                PsiTreeChangeListener.EP.name,
                PsiTreeChangeAdapter::class.java,
            )
            return AaSession(lease, sourceModules, libraryModules, javaFileManager)
        }

        private fun <T> KotlinCoreProjectEnvironment.registerApplicationServices(
            serviceInterface: Class<T>,
            serviceImplementation: Class<out T>,
        ) {
            val application = environment.application
            if (application.getServiceIfCreated(serviceInterface) == null) {
                KotlinCoreEnvironment.underApplicationLock {
                    if (application.getServiceIfCreated(serviceInterface) == null) {
                        application.registerService(serviceInterface, serviceImplementation)
                    }
                }
            }
        }

        // TODO: org.jetbrains.kotlin.analysis.providers.impl.KotlinStatic*
        private fun registerProjectServices(
            kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
            ktFiles: List<KtFile>,
            packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
            libraryRoots: List<VirtualFile>,
            isJvm: Boolean,
        ): IncrementalJavaFileManager? {
            val project = kotlinCoreProjectEnvironment.project
            var javaFileManager: IncrementalJavaFileManager? = null
            project.apply {
                registerService(
                    KotlinMessageBusProvider::class.java,
                    KotlinProjectMessageBusProvider::class.java,
                )
                FirStandaloneServiceRegistrar.registerProjectServices(project)
                FirStandaloneServiceRegistrar.registerProjectExtensionPoints(project)
                FirStandaloneServiceRegistrar.registerProjectModelServices(
                    project,
                    kotlinCoreProjectEnvironment.parentDisposable,
                )

                registerService(
                    KotlinModificationTrackerFactory::class.java,
                    KotlinStandaloneModificationTrackerFactory::class.java,
                )
                registerService(
                    KotlinLifetimeTokenFactory::class.java,
                    KotlinAlwaysAccessibleLifetimeTokenFactory::class.java,
                )

                // Despite being a static implementation, this is only used by IDE tests
                registerService(
                    KotlinAnnotationsResolverFactory::class.java,
                    KotlinStandaloneAnnotationsResolverFactory(project, ktFiles),
                )
                registerService(
                    KotlinDeclarationProviderFactory::class.java,
                    IncrementalKotlinDeclarationProviderFactory(this, kotlinCoreProjectEnvironment.environment),
                )
                registerService(
                    KotlinDirectInheritorsProvider::class.java,
                    StandaloneDirectInheritorsProvider::class.java,
                )
                registerService(
                    KotlinDeclarationProviderMerger::class.java,
                    KotlinStandaloneDeclarationProviderMerger(this),
                )
                // Package provider factory requires binary library roots to discover packages
                // declared in precompiled KLIBs (e.g. for Kotlin/Native and JS/Wasm targets).
                registerService(
                    KotlinPackageProviderFactory::class.java,
                    IncrementalKotlinPackageProviderFactory(project, libraryRoots),
                )

                registerService(
                    SealedClassInheritorsProvider::class.java,
                    LLSealedInheritorsProvider::class.java,
                )

                registerService(
                    KotlinPackagePartProviderFactory::class.java,
                    KotlinStaticPackagePartProviderFactory(packagePartProvider),
                )

                registerService(
                    KotlinPlatformSettings::class.java,
                    KotlinStandalonePlatformSettings(),
                )
                // replace KaFirStopWorldCacheCleaner with no op implementation
                registerService(KaFirCacheCleaner::class.java, NoOpCacheCleaner::class.java)

                if (isJvm) {
                    val rootsIndex = JvmDependenciesDynamicCompoundIndex(true)
                    val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, false, null)
                    // VirtualFileFinderFactory, MetadataFinderFactory, and PackageIndex are already
                    // registered during project environment creation with separate indices, so we use
                    // replaceService to point them at our own custom rootsIndex.
                    replaceService(VirtualFileFinderFactory::class.java, fileFinderFactory)
                    replaceService(
                        MetadataFinderFactory::class.java,
                        CliMetadataFinderFactory(fileFinderFactory),
                    )
                    replaceService(PackageIndex::class.java, JvmDependenciesPackageIndex(rootsIndex))
                    javaFileManager = IncrementalJavaFileManager(kotlinCoreProjectEnvironment, rootsIndex)
                    registerService(IncrementalJavaFileManager::class.java, javaFileManager)
                }
            }
            return javaFileManager
        }

        private fun <T : Any> MockProject.replaceService(
            serviceInterface: Class<T>,
            serviceImplementation: T,
        ) {
            picoContainer.unregisterComponent(serviceInterface.name)
            registerService(serviceInterface, serviceImplementation)
        }
    }

    /*
     * NoOp implementation of the KaFirCacheCleaner
     *
     * The stop world cache cleaner that is registered by default [KaFirStopWorldCacheCleaner] can
     * get analysis session into an invalid state which leads to build failures.
     */
    internal class NoOpCacheCleaner : KaFirCacheCleaner {
        override fun enterAnalysis() {}

        override fun exitAnalysis() {}

        override fun scheduleCleanup() {}
    }

    // 上流は SAM の匿名クラスを登録していた。クラスとして登録するため名前を付けた
    internal class NoopPluginProblemReporter : PluginProblemReporter {
        override fun createPluginExceptionByClass(
            message: String,
            cause: Throwable?,
            pluginClass: Class<*>,
        ): PluginException = PluginException(message, cause, null)
    }
}

/** ProjectModel のうち 1 実行単位の解析に要る部分（パスは rootDir で解決する）。 */
internal class ModelView(
    private val model: ProjectModel,
    unitName: String,
) {
    private val root = File(model.rootDir)
    val unit = model.units.singleOrNull { it.name == unitName } ?: error("実行単位 $unitName が ProjectModel に無い")
    val compilation: CompilationModel =
        model.compilations.singleOrNull { it.name == unit.analysisCompilation }
            ?: error("Compilation ${unit.analysisCompilation}（実行単位 $unitName）が ProjectModel に無い")
    private val sourceSetsByName = model.sourceSets.associateBy { it.name }

    /** Compilation の SourceSet と、その dependsOn の閉包を、上流が先になる順に並べたもの。 */
    val sourceSetsInDependencyOrder: List<SourceSetModel> =
        buildList {
            val visited = mutableSetOf<String>()

            fun visit(name: String) {
                if (!visited.add(name)) return
                val sourceSet = sourceSetsByName[name] ?: error("SourceSet $name が ProjectModel に無い")
                sourceSet.dependsOn.forEach(::visit)
                add(sourceSet)
            }
            compilation.sourceSets.forEach(::visit)
        }

    fun resolve(path: String): Path = root.resolve(path).toPath().normalize()

    fun sourceRoots(
        sourceSet: SourceSetModel,
        includeJava: Boolean,
    ): List<Path> =
        (sourceSet.kotlinSourceRoots + (if (includeJava) sourceSet.javaSourceRoots else emptyList()) + sourceSet.upstreamGeneratedRoots)
            .map(::resolve)
            .distinct()

    fun upstreamGeneratedRoots(sourceSet: String): List<Path> = sourceSetsByName.getValue(sourceSet).upstreamGeneratedRoots.map(::resolve)

    fun languageVersionSettings(): LanguageVersionSettings {
        val options = compilation.compilerOptions
        val languageVersion = options.languageVersion?.let(LanguageVersion::fromVersionString) ?: LanguageVersion.LATEST_STABLE
        val apiVersion =
            options.apiVersion?.let(LanguageVersion::fromVersionString)?.let(ApiVersion::createByLanguageVersion)
                ?: ApiVersion.createByLanguageVersion(languageVersion)
        val multiplatform = sourceSetsInDependencyOrder.size > 1 || sourceSetsInDependencyOrder.any { it.platform == PlatformKind.Common }
        return LanguageVersionSettingsImpl(
            languageVersion,
            apiVersion,
            analysisFlags = mapOf(AnalysisFlags.optIn to options.optIns),
            specificFeatures =
                buildMap {
                    put(LanguageFeature.AnnotationDefaultTargetMigrationWarning, LanguageFeature.State.ENABLED)
                    put(LanguageFeature.PropertyParamAnnotationDefaultTargetMode, LanguageFeature.State.ENABLED)
                    // 無いと expect / actual がすべてエラーになる（ADR-009 決定 2）
                    if (multiplatform) put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED)
                },
        )
    }
}

internal fun PlatformKind.toTargetPlatform(jvmTarget: String?): TargetPlatform =
    when (this) {
        PlatformKind.Jvm, PlatformKind.Android ->
            JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget?.let(JvmTarget::fromString) ?: JvmTarget.DEFAULT)
        PlatformKind.Common -> CommonPlatforms.defaultCommonPlatform
        PlatformKind.Js -> JsPlatforms.defaultJsPlatform
        PlatformKind.Wasm -> WasmPlatforms.wasmJs
        PlatformKind.Native -> NativePlatforms.unspecifiedNativePlatform
    }

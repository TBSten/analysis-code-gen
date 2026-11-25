@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UnstableApiUsage")

package poc.p02

import com.google.devtools.ksp.standalone.IncrementalGlobalSearchScope
import com.google.devtools.ksp.standalone.IncrementalJavaFileManager
import com.google.devtools.ksp.standalone.IncrementalKotlinDeclarationProviderFactory
import com.google.devtools.ksp.standalone.IncrementalKotlinPackageProviderFactory
import com.google.devtools.ksp.standalone.JvmDependenciesPackageIndex
import com.google.devtools.ksp.standalone.KspLibraryModuleBuilder
import com.google.devtools.ksp.standalone.KspModuleBuilder
import com.google.devtools.ksp.standalone.KspStandaloneDirectInheritorsProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.diagnostic.PluginException
import com.intellij.diagnostic.PluginProblemReporter
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeListener
import org.jetbrains.kotlin.analysis.api.KaSession
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
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.platform.resolution.KaResolutionActivityTracker
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
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
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.CliMetadataFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

/**
 * KSP2 (google/ksp 78f7882, KotlinSymbolProcessing.createAASession) の解析環境構築を JVM 向けに絞って写したもの。
 *
 * - [addGeneratedFiles] が「維持方式」のラウンド間更新（KSP2 の dropCaches + prepareNewKSFiles 相当）。
 * - 「再構築方式」は [AaEnv] を close して、生成物ディレクトリを source root に含めて作り直す。
 */
class AaEnv(
    private val sourceRoots: List<Path>,
    private val libraries: List<Path>,
    private val jdkHome: Path,
) : AutoCloseable {
    private val disposable: Disposable = run {
        setupIdeaStandaloneExecution() // IntelliJ の system property。環境を作る前に呼ぶ（KSP2 と同じ順）
        Disposer.newDisposable("poc-p02")
    }
    private val coreEnv: KotlinCoreProjectEnvironment =
        StandaloneProjectFactory.createProjectEnvironment(disposable, KotlinCoreApplicationEnvironmentMode.Production)
    val project: MockProject = coreEnv.project
    val module: KaSourceModule
    private val psiManager: PsiManager

    init {
        module = createSession()
        psiManager = PsiManager.getInstance(project)
        project.getService(IncrementalJavaFileManager::class.java).initialize(listOf(module) + module.directRegularDependencies, emptySet())
        val ktFiles = module.psiRoots.filterIsInstance<KtFile>()
        declarationFactory().update(ktFiles)
        packageFactory().update(ktFiles)
    }

    /** 解析対象の Kotlin ファイル（初期ソース + これまでに追加した生成物）。 */
    val ktFiles: MutableList<KtFile> = module.psiRoots.filterIsInstance<KtFile>().toMutableList()

    fun <R> analyze(action: KaSession.() -> R): R = analyzeModule(module, action)

    /**
     * 維持方式: 生成物をディスクに書いた後に呼ぶ。VFS から読み、content scope・宣言/パッケージ provider に足し、
     * ソースモジュールの変更イベントを発行して FIR キャッシュを無効化する（ライブラリのキャッシュは残る）。
     */
    fun addGeneratedFiles(files: List<Path>) = addGeneratedFiles(files, Invalidation.GLOBAL_SOURCE_STATE)

    /** 狭い無効化の試験: ソースモジュールの out-of-block 変更イベントだけを出す。 */
    fun addGeneratedFilesWithModuleOutOfBlock(files: List<Path>) = addGeneratedFiles(files, Invalidation.MODULE_OUT_OF_BLOCK)

    /** 対照実験用: キャッシュ無効化（変更イベント・PSI キャッシュ破棄）を抜いた差分反映。 */
    fun addGeneratedFilesWithoutInvalidation(files: List<Path>) = addGeneratedFiles(files, Invalidation.NONE)

    enum class Invalidation { GLOBAL_SOURCE_STATE, MODULE_OUT_OF_BLOCK, NONE }

    private fun addGeneratedFiles(files: List<Path>, invalidation: Invalidation) {
        when (invalidation) {
            Invalidation.GLOBAL_SOURCE_STATE -> runWriteAction {
                publishGlobalSourceModuleStateModification(project)
                psiManager.dropPsiCaches()
            }
            Invalidation.MODULE_OUT_OF_BLOCK -> runWriteAction {
                publishModuleOutOfBlockModification(module)
                psiManager.dropPsiCaches()
            }
            Invalidation.NONE -> Unit
        }
        val fs = StandardFileSystems.local()
        val newKtFiles = files.mapNotNull { p ->
            val vf = fs.refreshAndFindFileByPath(p.toString()) ?: error("not found in VFS: $p")
            psiManager.findFile(vf) as? KtFile
        }
        (module.contentScope as IncrementalGlobalSearchScope).addAll(newKtFiles.map { it.virtualFile })
        declarationFactory().update(newKtFiles)
        packageFactory().update(newKtFiles)
        ktFiles += newKtFiles
    }

    override fun close() {
        runWriteAction { Disposer.dispose(disposable) }
    }

    private fun declarationFactory() =
        project.getService(KotlinDeclarationProviderFactory::class.java) as IncrementalKotlinDeclarationProviderFactory

    private fun packageFactory() =
        project.getService(KotlinPackageProviderFactory::class.java) as IncrementalKotlinPackageProviderFactory

    private fun createSession(): KaSourceModule {
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            KaResolveExtensionProvider.EP_NAME.name,
            KaResolveExtensionProvider::class.java,
        )
        val platform = JvmPlatforms.defaultJvmPlatform
        val appEnv = coreEnv.environment
        lateinit var sourceModule: KaSourceModule
        val provider = KtModuleProviderBuilder(appEnv, project).apply {
            val libs = KspLibraryModuleBuilder(appEnv, project).apply {
                this.platform = platform
                addBinaryRoots(libraries)
                libraryName = "Libraries"
            }.build()
            val jdk = KspLibraryModuleBuilder(appEnv, project).apply {
                this.platform = platform
                addBinaryRoots(org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils.findClassesFromJdkHome(jdkHome, false))
                libraryName = "JDK"
            }.build(isSdk = true)
            sourceModule = KspModuleBuilder(appEnv, project).apply {
                languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
                this.platform = platform
                moduleName = "main"
                addRegularDependency(libs)
                addRegularDependency(jdk)
                addSourceRoots(sourceRoots)
            }.build()
            addModule(sourceModule)
            this.platform = platform
        }.build()

        StandaloneProjectFactory.registerServicesForProjectEnvironment(coreEnv, provider)
        val modules = provider.allModules
        val ktFiles = provider.allSourceFiles.filterIsInstance<KtFile>()
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(modules, appEnv)
        val packagePartProvider = StandaloneProjectFactory.createPackagePartsProvider(libraryRoots)

        registerAppService(
            org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry::class.java,
            org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionRegistry::class.java,
        )
        registerAppService(KotlinAnalysisPermissionOptions::class.java, KotlinStandaloneAnalysisPermissionOptions::class.java)
        registerAppService(KaResolutionActivityTracker::class.java, LLFirResolutionActivityTracker::class.java)
        registerAppService(PluginProblemReporter::class.java, NoopProblemReporter::class.java)

        project.apply {
            registerService(KotlinMessageBusProvider::class.java, KotlinProjectMessageBusProvider::class.java)
            FirStandaloneServiceRegistrar.registerProjectServices(project)
            FirStandaloneServiceRegistrar.registerProjectExtensionPoints(project)
            FirStandaloneServiceRegistrar.registerProjectModelServices(project, coreEnv.parentDisposable)
            registerService(KotlinModificationTrackerFactory::class.java, KotlinStandaloneModificationTrackerFactory::class.java)
            registerService(KotlinLifetimeTokenFactory::class.java, KotlinAlwaysAccessibleLifetimeTokenFactory::class.java)
            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStandaloneAnnotationsResolverFactory(project, ktFiles))
            registerService(KotlinDeclarationProviderFactory::class.java, IncrementalKotlinDeclarationProviderFactory(this, appEnv))
            registerService(KotlinDirectInheritorsProvider::class.java, KspStandaloneDirectInheritorsProvider::class.java)
            registerService(KotlinDeclarationProviderMerger::class.java, KotlinStandaloneDeclarationProviderMerger(this))
            registerService(
                KotlinPackageProviderFactory::class.java,
                IncrementalKotlinPackageProviderFactory(project, libraryRoots.map { it.file }.distinct()),
            )
            registerService(SealedClassInheritorsProvider::class.java, LLSealedInheritorsProvider::class.java)
            registerService(KotlinPackagePartProviderFactory::class.java, KotlinStaticPackagePartProviderFactory(packagePartProvider))
            registerService(KotlinPlatformSettings::class.java, KotlinStandalonePlatformSettings())
            registerService(KaFirCacheCleaner::class.java, NoOpCacheCleaner::class.java)

            val rootsIndex = JvmDependenciesDynamicCompoundIndex(true)
            val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, false, null)
            replaceService(VirtualFileFinderFactory::class.java, fileFinderFactory)
            replaceService(MetadataFinderFactory::class.java, CliMetadataFinderFactory(fileFinderFactory))
            replaceService(PackageIndex::class.java, JvmDependenciesPackageIndex(rootsIndex))
            registerService(IncrementalJavaFileManager::class.java, IncrementalJavaFileManager(coreEnv, rootsIndex))
        }
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea, PsiTreeChangeListener.EP.name, PsiTreeChangeAdapter::class.java,
        )
        return sourceModule
    }

    private fun <T> registerAppService(iface: Class<T>, impl: Class<out T>) {
        val app = coreEnv.environment.application
        if (app.getServiceIfCreated(iface) == null) {
            KotlinCoreEnvironment.underApplicationLock {
                if (app.getServiceIfCreated(iface) == null) app.registerService(iface, impl)
            }
        }
    }

    private fun <T : Any> MockProject.replaceService(iface: Class<T>, impl: T) {
        picoContainer.unregisterComponent(iface.name)
        registerService(iface, impl)
    }

    class NoOpCacheCleaner : KaFirCacheCleaner {
        override fun enterAnalysis() {}
        override fun exitAnalysis() {}
        override fun scheduleCleanup() {}
    }

    class NoopProblemReporter : PluginProblemReporter {
        override fun createPluginExceptionByClass(message: String, cause: Throwable?, pluginClass: Class<*>): PluginException =
            PluginException(message, cause, null)
    }

    companion object {
        fun <R> runWriteAction(f: () -> R): R = synchronized(com.intellij.util.ui.EDT::class.java) {
            if (!com.intellij.util.ui.EDT.isCurrentThreadEdt()) com.intellij.util.ui.EDT.updateEdt()
            val app = ApplicationManager.getApplication()
            if (app != null) {
                var r: R? = null
                app.runWriteAction { r = f() }
                @Suppress("UNCHECKED_CAST")
                r as R
            } else {
                f()
            }
        }
    }
}

// --- top-level 関数の呼び出し ---
// aa-embeddable には .kotlin_module が無く、Kotlin から top-level 関数を直接参照できないため reflection で呼ぶ。

private fun setupIdeaStandaloneExecution() {
    Class.forName("org.jetbrains.kotlin.cli.jvm.compiler.CompatKt")
        .getMethod("setupIdeaStandaloneExecution").invoke(null)
}

private fun publishGlobalSourceModuleStateModification(project: com.intellij.openapi.project.Project) {
    Class.forName("org.jetbrains.kotlin.analysis.api.platform.modification.UtilsKt")
        .getMethod("publishGlobalSourceModuleStateModificationEvent", com.intellij.openapi.project.Project::class.java)
        .invoke(null, project)
}

private fun publishModuleOutOfBlockModification(module: KaModule) {
    Class.forName("org.jetbrains.kotlin.analysis.api.platform.modification.UtilsKt")
        .getMethod("publishModuleOutOfBlockModificationEvent", KaModule::class.java)
        .invoke(null, module)
}

private val analyzeMethod by lazy {
    Class.forName("org.jetbrains.kotlin.analysis.api.AnalyzeKt")
        .getMethod("analyze", KaModule::class.java, Function1::class.java)
}

@Suppress("UNCHECKED_CAST")
private fun <R> analyzeModule(module: KaModule, action: KaSession.() -> R): R =
    analyzeMethod.invoke(null, module, { s: KaSession -> s.action() } as Function1<KaSession, R>) as R

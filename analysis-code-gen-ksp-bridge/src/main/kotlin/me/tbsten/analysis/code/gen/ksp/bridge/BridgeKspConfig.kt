@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.ksp.bridge

import com.google.devtools.ksp.IncrementalContextLoggingOptions
import com.google.devtools.ksp.processing.KSPConfig
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.PlatformInfo
import me.tbsten.analysis.code.gen.engine.spi.EngineProcessorEnvironment
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.JsPlatformInfoImpl
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.JvmPlatformInfoImpl
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.NativePlatformInfoImpl
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.UnknownPlatformInfoImpl
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import java.io.File

/**
 * 移植コード（ResolverAAImpl・KS* 実装）が読む [KSPConfig] を組み立てる（ADR-062）。
 *
 * 移植コードが実際に読むのは `experimentalPsiResolution` だけ。ディレクトリなどはエンジンが持つので、ここでは使わない値を入れる。
 * KSP の Gradle プロパティの読み取り（ADR-035）は Gradle plugin 側の担当で、ここには options として届く。
 */
internal fun bridgeKspConfig(environment: EngineProcessorEnvironment): KSPConfig {
    val unused = File(".")
    val version = environment.kotlinVersion.toKotlinVersion()
    val languageVersion = "${version.major}.${version.minor}"
    return KSPJvmConfig(
        javaSourceRoots = emptyList(),
        javaOutputDir = unused,
        jdkHome = null,
        jvmTarget = "17",
        jvmDefaultMode = "disable",
        moduleName = environment.executionUnit.name,
        sourceRoots = emptyList(),
        commonSourceRoots = emptyList(),
        libraries = emptyList(),
        friends = emptyList(),
        processorOptions = environment.options,
        projectBaseDir = unused,
        outputBaseDir = unused,
        cachesDir = unused,
        classOutputDir = unused,
        kotlinOutputDir = unused,
        resourceOutputDir = unused,
        incremental = false,
        incrementalContextLoggingOptions = IncrementalContextLoggingOptions(false, null),
        modifiedSources = emptyList(),
        removedSources = emptyList(),
        changedClasses = emptyList(),
        languageVersion = languageVersion,
        apiVersion = languageVersion,
        allWarningsAsErrors = false,
        mapAnnotationArgumentsInJava = false,
        experimentalPsiResolution = environment.options["ksp.experimental.psi.resolution"]?.toBoolean() ?: false,
    )
}

/** KSP2 の `KotlinSymbolProcessing.getPlatformInfo` と同じ対応で [PlatformInfo] にする。 */
internal fun TargetPlatform.toPlatformInfo(kspConfig: KSPConfig): List<PlatformInfo> =
    componentPlatforms.map { platform ->
        when (platform) {
            is JdkPlatform ->
                JvmPlatformInfoImpl(
                    platformName = platform.platformName,
                    jvmTarget = platform.targetVersion.toString(),
                    jvmDefaultMode = (kspConfig as? KSPJvmConfig)?.jvmDefaultMode ?: "disable",
                )
            is JsPlatform -> JsPlatformInfoImpl(platformName = platform.platformName)
            is NativePlatform -> NativePlatformInfoImpl(platformName = platform.platformName, targetName = platform.targetName)
            else -> UnknownPlatformInfoImpl(platform.toString())
        }
    }

/** `2.4.21` や `2.5.0-Beta1` を [KotlinVersion] にする。読めなければ実行中の版。 */
internal fun String.toKotlinVersion(): KotlinVersion {
    val parts = substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }
    return when (parts.size) {
        2 -> KotlinVersion(parts[0], parts[1])
        3 -> KotlinVersion(parts[0], parts[1], parts[2])
        else -> KotlinVersion.CURRENT
    }
}

package me.tbsten.analysiscodegen.testprocessors.native

import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.CodeGenerator
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.testprocessors.PlatformNames
import me.tbsten.analysiscodegen.testprocessors.TpFile
import me.tbsten.analysiscodegen.testprocessors.TpId
import me.tbsten.analysiscodegen.testprocessors.TpOptions
import me.tbsten.analysiscodegen.testprocessors.TpResourceFile
import me.tbsten.analysiscodegen.testprocessors.TypeNotation
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.types.Variance

/**
 * `tp.processors` で有効なときだけ本物の Processor を作る Provider（KSP 版の TpKspProvider と同じ規則）。
 */
public abstract class TpNativeProvider(private val id: TpId) : AnalysisCodegenProcessorProvider {
    final override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor {
        val options = TpOptions(environment.options)
        return if (options.isEnabled(id)) createEnabled(environment, options) else NoopProcessor
    }

    protected abstract fun createEnabled(
        environment: AnalysisCodegenProcessorEnvironment,
        options: TpOptions,
    ): AnalysisCodegenProcessor
}

internal object NoopProcessor : AnalysisCodegenProcessor {
    override fun process(context: AnalysisCodegenRoundContext) {}
}

internal fun CodeGenerator.write(
    dependencies: Dependencies,
    file: TpFile,
) {
    when (file.extension) {
        "kt" -> generateKotlin(file.packageName, file.fileName, dependencies, file.content)
        "java" -> generateJava(file.packageName, file.fileName, dependencies, file.content)
        else -> error("未対応の拡張子: ${file.extension}")
    }
}

internal fun CodeGenerator.write(
    dependencies: Dependencies,
    file: TpResourceFile,
) {
    generateResource(file.relativePath, dependencies, file.content.toByteArray(Charsets.UTF_8))
}

internal fun classIdOf(fqn: String): ClassId = ClassId.topLevel(FqName(fqn))

/** ファイル内のクラス（ネストも含む）のうち、[annotationFqn] が付いたもの。FQN 順。 */
internal fun KaSession.classesWith(
    files: List<KtFile>,
    annotationFqn: String,
): List<Pair<KtClassOrObject, KaClassLikeSymbol>> {
    val classId = classIdOf(annotationFqn)
    return files
        .flatMap { it.collectDescendantsOfType<KtClassOrObject>() }
        .mapNotNull { psi ->
            val symbol = psi.symbol as? KaClassLikeSymbol ?: return@mapNotNull null
            if (classId in symbol.annotations) psi to symbol else null
        }.sortedBy { it.second.classId?.asFqNameString() }
}

/** 名前付き引数の定数値。省略された引数（既定値）は null。 */
internal fun annotationArgument(
    symbol: KaAnnotated,
    annotationFqn: String,
    name: String,
): Any? {
    val annotation = symbol.annotations[classIdOf(annotationFqn)].firstOrNull() ?: return null
    val value = annotation.arguments.firstOrNull { it.name.asString() == name }?.expression ?: return null
    return (value as? org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue.ConstantValue)?.value?.value
}

/** パッケージ内での名前（ネストは `.`）。 */
internal val KaClassLikeSymbol.nameInPackage: String
    get() = classId!!.relativeClassName.asString()

internal val KaClassLikeSymbol.packageName: String
    get() = classId!!.packageFqName.asString()

/** [TypeNotation] の規則で型を書く（KSP 版の notation() と同じ文字列にする）。 */
internal fun KaSession.notation(type: KaType): String {
    val expanded = type.fullyExpandedType
    val nullable = expanded.isMarkedNullable
    return when (expanded) {
        is KaErrorType -> TypeNotation.ERROR
        is KaTypeParameterType -> TypeNotation.typeParameter(expanded.name.asString(), nullable)
        is KaClassType ->
            TypeNotation.classType(
                expanded.classId.asFqNameString(),
                expanded.typeArguments.map { arg ->
                    when (arg) {
                        is KaStarTypeProjection -> TypeNotation.argument(null, null)
                        is KaTypeArgumentWithVariance ->
                            when (arg.variance) {
                                Variance.OUT_VARIANCE -> TypeNotation.argument("out", notation(arg.type))
                                Variance.IN_VARIANCE -> TypeNotation.argument("in", notation(arg.type))
                                Variance.INVARIANT -> TypeNotation.argument(null, notation(arg.type))
                            }
                    }
                },
                nullable,
            )
        else -> TypeNotation.ERROR
    }
}

/** [PlatformNames] にする。 */
internal fun platformName(platform: TargetPlatform): String =
    when {
        platform.isCommon() -> PlatformNames.COMMON
        platform.isJvm() -> PlatformNames.JVM
        platform.isWasm() -> PlatformNames.WASM
        platform.isJs() -> PlatformNames.JS
        platform.isNative() -> PlatformNames.NATIVE
        else -> PlatformNames.UNKNOWN
    }

/** 実行単位のプラットフォーム。Compilation 単位ならその Compilation、SourceSet 単位ならその SourceSet。 */
internal val AnalysisCodegenRoundContext.platform: TargetPlatform
    get() = compilation?.platform ?: sourceSet.platform

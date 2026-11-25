package me.tbsten.analysiscodegen.testprocessors.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.JsPlatformInfo
import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.NativePlatformInfo
import com.google.devtools.ksp.processing.PlatformInfo
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Variance
import me.tbsten.analysiscodegen.testprocessors.PlatformNames
import me.tbsten.analysiscodegen.testprocessors.TpFile
import me.tbsten.analysiscodegen.testprocessors.TpId
import me.tbsten.analysiscodegen.testprocessors.TpOptions
import me.tbsten.analysiscodegen.testprocessors.TpResourceFile
import me.tbsten.analysiscodegen.testprocessors.TypeNotation

/**
 * `tp.processors` で有効なときだけ本物の Processor を作る Provider。1 つの JAR に全部入れても、選んだものだけが動く。
 */
public abstract class TpKspProvider(private val id: TpId) : SymbolProcessorProvider {
    final override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val options = TpOptions(environment.options)
        return if (options.isEnabled(id)) createEnabled(environment, options) else NoopProcessor
    }

    protected abstract fun createEnabled(
        environment: SymbolProcessorEnvironment,
        options: TpOptions,
    ): SymbolProcessor
}

internal object NoopProcessor : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> = emptyList()
}

internal fun CodeGenerator.write(
    dependencies: Dependencies,
    file: TpFile,
) {
    createNewFile(dependencies, file.packageName, file.fileName, file.extension).use { it.write(file.content.toByteArray(Charsets.UTF_8)) }
}

internal fun CodeGenerator.write(
    dependencies: Dependencies,
    file: TpResourceFile,
) {
    val path = file.relativePath
    val dot = path.lastIndexOf('.')
    val (base, ext) = if (dot > path.lastIndexOf('/')) path.substring(0, dot) to path.substring(dot + 1) else path to ""
    createNewFileByPath(dependencies, base, ext).use { it.write(file.content.toByteArray(Charsets.UTF_8)) }
}

internal fun Resolver.classesWith(annotationFqn: String): List<KSClassDeclaration> =
    getSymbolsWithAnnotation(annotationFqn).filterIsInstance<KSClassDeclaration>().sortedBy { it.qualifiedName?.asString() }.toList()

internal fun KSAnnotated.annotation(fqn: String): KSAnnotation? =
    annotations.firstOrNull {
        it.annotationType
            .resolve()
            .declaration.qualifiedName
            ?.asString() == fqn
    }

/** 名前付き引数の値。KSP2 は既定値も arguments に入れるが、入らない版のために defaultArguments も見る。 */
internal fun KSAnnotation.argument(name: String): Any? =
    arguments.firstOrNull { it.name?.asString() == name }?.value
        ?: defaultArguments.firstOrNull { it.name?.asString() == name }?.value

/** パッケージ内での名前（ネストは `.`）。 */
internal val KSDeclaration.nameInPackage: String
    get() {
        val fqn = qualifiedName?.asString() ?: simpleName.asString()
        val pkg = packageName.asString()
        return if (pkg.isEmpty()) fqn else fqn.removePrefix("$pkg.")
    }

/** [TypeNotation] の規則で型を書く。 */
internal fun KSType.notation(): String {
    if (isError) return TypeNotation.ERROR
    return when (val decl = declaration) {
        is KSTypeAlias -> {
            val expanded = decl.type.resolve()
            if (isMarkedNullable) expanded.makeNullable().notation() else expanded.notation()
        }
        is KSTypeParameter -> TypeNotation.typeParameter(decl.name.asString(), isMarkedNullable)
        else ->
            TypeNotation.classType(
                decl.qualifiedName?.asString() ?: decl.simpleName.asString(),
                arguments.map { arg ->
                    val type = arg.type?.resolve()?.notation()
                    when (arg.variance) {
                        Variance.STAR -> TypeNotation.argument(null, null)
                        Variance.COVARIANT -> TypeNotation.argument("out", type)
                        Variance.CONTRAVARIANT -> TypeNotation.argument("in", type)
                        Variance.INVARIANT -> TypeNotation.argument(null, type)
                    }
                },
                isMarkedNullable,
            )
    }
}

/** KSP のプラットフォーム情報を [PlatformNames] にする。複数なら common（KMP の metadata コンパイル）。 */
internal fun platformName(platforms: List<PlatformInfo>): String {
    val single = platforms.singleOrNull() ?: return PlatformNames.COMMON
    // KSP2 は wasm を UnknownPlatformInfo（名前に wasm を含む）で渡す
    if (single.platformName.contains("wasm", ignoreCase = true)) return PlatformNames.WASM
    return when (single) {
        is JvmPlatformInfo -> PlatformNames.JVM
        is JsPlatformInfo -> PlatformNames.JS
        is NativePlatformInfo -> PlatformNames.NATIVE
        else -> PlatformNames.UNKNOWN
    }
}

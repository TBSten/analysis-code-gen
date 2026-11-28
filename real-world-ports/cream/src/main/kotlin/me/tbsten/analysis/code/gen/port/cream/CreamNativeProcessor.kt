package me.tbsten.analysis.code.gen.port.cream

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysis.code.gen.processor.Dependencies
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.types.Variance

// 参考: TBSten/cream@2162ec8d5261e3cb047de6825545f589102caab5:cream-ksp/src/main/kotlin/me/tbsten/cream/ksp/
//   CreamSymbolProcessor.kt、feature/copyTo/ProcessCopyTo.kt、feature/copyFrom/ProcessCopyFrom.kt、
//   feature/copyToChildren/ProcessCopyToChildren.kt（どの注釈からどのファイルを作るか。KSP の API を AA に置き換えて書き直した）

private const val CREAM_PACKAGE = "me.tbsten.cream"
private val COPY_TO = ClassId.topLevel(FqName("$CREAM_PACKAGE.CopyTo"))
private val COPY_FROM = ClassId.topLevel(FqName("$CREAM_PACKAGE.CopyFrom"))
private val COPY_TO_CHILDREN = ClassId.topLevel(FqName("$CREAM_PACKAGE.CopyToChildren"))
private val SUPPORTED = setOf(COPY_TO, COPY_FROM, COPY_TO_CHILDREN)

/** 1 つの生成ファイル。 */
private class Output(
    val packageName: String,
    val fileName: String,
    val content: String,
    val inputs: List<PsiFile>,
)

/** M1 の範囲外。位置つきで error にする（ADR-071）。 */
private class Unsupported(
    val message: String,
    val element: PsiElement?,
)

internal class CreamNativeProcessor(
    rawOptions: Map<String, String>,
) : AnalysisCodegenProcessor {
    private val options = CreamOptions.parse(rawOptions)

    override fun process(context: AnalysisCodegenRoundContext) {
        if (context.round == 1) {
            options.invalid.forEach { context.logger.error("cream.kt のオプションが不正: $it") }
            options.unsupported.forEach { context.logger.error("cream.kt の Native 版（M1）はオプション $it に対応していない（ADR-071）") }
            if (options.invalid.isNotEmpty() || options.unsupported.isNotEmpty()) return
        }
        val problems = mutableListOf<Unsupported>()
        val outputs = analyze(context.sourceSet.module) { collect(context.newFiles, problems) }
        problems.forEach { context.logger.error(it.message, it.element) }
        if (problems.isNotEmpty()) return
        outputs.forEach { output ->
            context.codeGenerator.generateKotlin(output.packageName, output.fileName, Dependencies.isolating(output.inputs), output.content)
        }
    }

    private fun KaSession.collect(
        files: List<KtFile>,
        problems: MutableList<Unsupported>,
    ): List<Output> =
        files.flatMap { file ->
            file.collectDescendantsOfType<KtClassOrObject>().flatMap { psi ->
                val symbol = psi.symbol as? KaNamedClassSymbol ?: return@flatMap emptyList()
                checkUnsupportedAnnotations(psi, symbol, problems)
                buildList {
                    symbol.annotations[COPY_TO].firstOrNull()?.let { annotation ->
                        if (checkArguments(annotation, "targets", psi, problems)) {
                            val targets = classArguments(annotation, "targets")
                            addIfNotNull(output("CopyTo", symbol, file, targets.map { symbol to it }, problems))
                        }
                    }
                    symbol.annotations[COPY_FROM].firstOrNull()?.let { annotation ->
                        if (checkArguments(annotation, "sources", psi, problems)) {
                            val sources = classArguments(annotation, "sources")
                            addIfNotNull(output("CopyFrom", symbol, file, sources.map { it to symbol }, problems))
                        }
                    }
                    symbol.annotations[COPY_TO_CHILDREN].firstOrNull()?.let { annotation ->
                        if (checkArguments(annotation, null, psi, problems)) {
                            if (symbol.modality != KaSymbolModality.SEALED) {
                                problems += Unsupported("@CopyToChildren は sealed class / interface にだけ付けられる", psi)
                            } else {
                                addIfNotNull(output("CopyToChildren", symbol, file, leavesOf(symbol).map { symbol to it }, problems))
                            }
                        }
                    }
                }
            }
        }

    /** `<注釈名>__<付けたクラスのパッケージより下の名前>.kt` を、付けたクラスのパッケージに作る（cream と同じ）。 */
    private fun KaSession.output(
        annotationName: String,
        annotated: KaNamedClassSymbol,
        file: KtFile,
        pairs: List<Pair<KaNamedClassSymbol, KaNamedClassSymbol>>,
        problems: MutableList<Unsupported>,
    ): Output? {
        val name = annotated.className()
        val functions = pairs.mapNotNull { (source, target) -> copyFunctions(annotationName, annotated, source, target, problems) }
        if (functions.isEmpty()) return null
        val inputs = (listOf(file) + pairs.flatMap { (s, t) -> listOfNotNull(s.psi?.containingFile, t.psi?.containingFile) }).distinct()
        val content =
            buildString {
                append("package ${name.packageName}\n\n")
                append("import $CREAM_PACKAGE.*\n\n")
                append(functions.joinToString("\n"))
            }
        return Output(name.packageName, "${annotationName}__${name.underPackageName}", content, inputs)
    }

    private fun KaSession.copyFunctions(
        annotationName: String,
        annotated: KaNamedClassSymbol,
        source: KaNamedClassSymbol,
        target: KaNamedClassSymbol,
        problems: MutableList<Unsupported>,
    ): String? {
        if (source.typeParameters.isNotEmpty() || target.typeParameters.isNotEmpty()) {
            problems += Unsupported("cream.kt の Native 版（M1）は型パラメータを持つクラスに対応していない（ADR-071）", annotated.psi)
            return null
        }
        val rendering =
            CopyRendering(
                annotationName = annotationName,
                annotated = annotated.className(),
                source = source.className(),
                target = target.className(),
                sourceType = source.qualifiedName(),
                targetType = target.qualifiedName(),
                functionName = options.copyFunctionName(source.className(), target.className()),
                visibility = visibilityModifier(source, target),
            )
        if (target.classKind == KaClassKind.OBJECT) return rendering.objectFunction()
        return target.memberScope.constructors
            .toList()
            .filter { it.visibility == KaSymbolVisibility.PUBLIC || it.visibility == KaSymbolVisibility.INTERNAL }
            .joinToString("\n") { constructor ->
                val parameters =
                    constructor.valueParameters.map { parameter ->
                        val type = parameter.returnType
                        val matched =
                            if (parameter.isVararg) {
                                null
                            } else {
                                source.memberScope
                                    .callables(parameter.name)
                                    .filterIsInstance<KaPropertySymbol>()
                                    .firstOrNull { it.returnType.isSubtypeOf(type) }
                            }
                        CopyParameter(
                            name = parameter.name.asString(),
                            type = renderType(type),
                            isVararg = parameter.isVararg,
                            defaultProperty = matched?.name?.asString(),
                        )
                    }
                rendering.classFunction(parameters)
            }
    }

    /** sealed の親から、入れ子の sealed をたどった具象の子すべて（cream の `@CopyToChildren`）。 */
    private fun KaSession.leavesOf(parent: KaNamedClassSymbol): List<KaNamedClassSymbol> =
        parent.sealedClassInheritors.flatMap { child ->
            when {
                child.modality == KaSymbolModality.SEALED -> leavesOf(child)
                child.modality == KaSymbolModality.ABSTRACT -> emptyList()
                child.classKind == KaClassKind.INTERFACE -> emptyList()
                else -> listOf(child)
            }
        }

    /** 注釈の引数のうち、M1 で扱うもの（[allowed]）以外が明示されていたら範囲外。 */
    private fun checkArguments(
        annotation: KaAnnotation,
        allowed: String?,
        psi: PsiElement,
        problems: MutableList<Unsupported>,
    ): Boolean {
        val extra = annotation.arguments.map { it.name.asString() }.filter { it != allowed }
        if (extra.isEmpty()) return true
        problems +=
            Unsupported(
                "cream.kt の Native 版（M1）は @${annotation.classId?.shortClassName} の引数 ${extra.joinToString()} に対応していない（ADR-071）",
                psi,
            )
        return false
    }

    /** cream の注釈のうち M1 で扱わないもの（@CopyMapping・@CombineTo・@CopyTo.Map など）が付いていたら範囲外。 */
    private fun KaSession.checkUnsupportedAnnotations(
        psi: KtClassOrObject,
        symbol: KaNamedClassSymbol,
        problems: MutableList<Unsupported>,
    ) {
        val annotated =
            listOf(psi to symbol.annotations.mapNotNull { it.classId }) +
                psi.primaryConstructorParameters.map { p -> p to p.symbol.annotations.mapNotNull { it.classId } }
        for ((element, classIds) in annotated) {
            classIds
                .filter { it.packageFqName.asString() == CREAM_PACKAGE && it !in SUPPORTED }
                .forEach { problems += Unsupported("cream.kt の Native 版（M1）は @${it.relativeClassName} に対応していない（ADR-071）", element) }
        }
    }

    private fun KaSession.classArguments(
        annotation: KaAnnotation,
        name: String,
    ): List<KaNamedClassSymbol> {
        val value = annotation.arguments.firstOrNull { it.name.asString() == name }?.expression ?: return emptyList()
        val values = if (value is KaAnnotationValue.ArrayValue) value.values else listOf(value)
        return values.mapNotNull { ((it as? KaAnnotationValue.ClassLiteralValue)?.type as? KaClassType)?.symbol as? KaNamedClassSymbol }
    }

    /** cream の `CopyVisibility.INHERIT`: どちらかが internal なら internal。 */
    private fun visibilityModifier(
        source: KaNamedClassSymbol,
        target: KaNamedClassSymbol,
    ): String =
        if (source.visibility == KaSymbolVisibility.INTERNAL ||
            target.visibility == KaSymbolVisibility.INTERNAL
        ) {
            "internal"
        } else {
            "public"
        }

    private fun KaNamedClassSymbol.className(): ClassName {
        val id = classId!!
        return ClassName(id.packageFqName.asString(), id.relativeClassName.asString())
    }

    private fun KaNamedClassSymbol.qualifiedName(): String = classId!!.asFqNameString().escapedFqName()

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.renderType(type: KaType): String = type.render(KaTypeRendererForSource.WITH_QUALIFIED_NAMES, Variance.INVARIANT)

    private fun <T : Any> MutableList<T>.addIfNotNull(value: T?) {
        if (value != null) add(value)
    }
}

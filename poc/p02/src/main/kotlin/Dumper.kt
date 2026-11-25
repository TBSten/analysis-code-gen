package poc.p02

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.types.Variance

/** 解析結果を決まった順のテキストにする。維持方式と再構築方式の結果をこの文字列で比べる。 */
enum class Profile {
    /** Processor 相当: クラス・上位型・アノテーション・メンバーのシグネチャ。採用判定に使う。 */
    DECL,

    /** DECL + 関数本体の参照解決と診断。参考値。 */
    FULL,
}

data class Dump(val text: String, val errors: Int, val unresolved: Int)

object Dumper {
    fun dump(session: KaSession, files: List<KtFile>, profile: Profile): Dump = with(session) {
        val sb = StringBuilder()
        var errors = 0
        var unresolved = 0
        for (file in files.sortedBy { it.virtualFilePath }) {
            sb.append("FILE ").append(file.virtualFile.name).append(" pkg=").append(file.packageFqName).append('\n')
            for (cls in file.symbol.fileScope.classifiers.filterIsInstance<KaNamedClassSymbol>().sortedBy { it.name.asString() }) {
                sb.append("  class ").append(cls.classId?.asFqNameString()).append(" kind=").append(cls.classKind)
                sb.append(" annotations=").append(cls.annotations.map { it.classId?.asFqNameString() })
                sb.append(" supertypes=").append(cls.superTypes.map { render(it) }).append('\n')
                for (m in cls.declaredMemberScope.callables.map { (it.callableId?.toString() ?: "<init>") + ": " + render(it.returnType) }.sorted()) {
                    sb.append("    member ").append(m).append('\n')
                }
            }
            for (c in file.symbol.fileScope.callables.sortedBy { it.callableId?.toString() }) {
                sb.append("  top ").append(c.callableId)
                c.receiverParameter?.let { sb.append(" receiver=").append(render(it.returnType)) }
                sb.append(": ").append(render(c.returnType)).append('\n')
            }
            if (profile == Profile.FULL) {
                for (ref in PsiTreeUtil.collectElementsOfType(file, KtNameReferenceExpression::class.java).sortedBy { it.textOffset }) {
                    val targets = ref.references.filterIsInstance<org.jetbrains.kotlin.idea.references.KtReference>().flatMap { r -> r.resolveToSymbols() }
                    if (targets.isEmpty()) unresolved++
                    sb.append("    ref ").append(ref.getReferencedName()).append('@').append(ref.textOffset)
                        .append(" -> ").append(targets.map { describe(it) }.sorted().ifEmpty { listOf("UNRESOLVED") }).append('\n')
                }
                val diags = file.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
                for (d in diags.sortedBy { it.textRanges.first().startOffset }) {
                    if (d.severity.name == "ERROR") errors++
                    sb.append("    diag ").append(d.severity).append(' ').append(d.factoryName)
                        .append('@').append(d.textRanges.first().startOffset).append('\n')
                }
            }
        }
        Dump(sb.toString(), errors, unresolved)
    }

    private fun KaSession.render(type: KaType): String =
        type.render(KaTypeRendererForSource.WITH_QUALIFIED_NAMES, Variance.INVARIANT)

    private fun describe(s: KaSymbol): String = when (s) {
        is KaClassLikeSymbol -> "class:" + s.classId?.asFqNameString()
        is KaCallableSymbol -> "callable:" + (s.callableId?.toString() ?: ("local:" + (s as? KaNamedSymbol)?.name))
        else -> s.javaClass.simpleName
    }
}

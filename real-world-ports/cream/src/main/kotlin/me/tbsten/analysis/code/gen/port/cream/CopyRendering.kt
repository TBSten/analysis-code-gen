package me.tbsten.analysis.code.gen.port.cream

// 参考: TBSten/cream@2162ec8d5261e3cb047de6825545f589102caab5:cream-ksp/src/main/kotlin/me/tbsten/cream/ksp/
//   core/copyFun/Class.kt・core/copyFun/Object.kt・core/common/KDoc.kt・util/EscapeIdentifier.kt
//   （生成する関数の形。KDoc は短くし、型の書き方は AA の描画に置き換えて書き直した）

/** 写す先のコンストラクタのパラメータ 1 つ。[defaultProperty] があれば `= this.<名前>` を既定値にする。 */
internal class CopyParameter(
    val name: String,
    val type: String,
    val isVararg: Boolean,
    val defaultProperty: String?,
)

/** 写し関数 1 つ分の文字列を作る。 */
internal class CopyRendering(
    private val annotationName: String,
    private val annotated: ClassName,
    private val source: ClassName,
    private val target: ClassName,
    private val sourceType: String,
    private val targetType: String,
    private val functionName: String,
    private val visibility: String,
) {
    private fun kdoc(): String =
        """
        |/**
        | * (Auto generate by @[$annotationName] annotation of [${annotated.underPackageName}])
        | *
        | * ${source.underPackageName} -> ${target.underPackageName} copy function.
        | *
        | * @see ${source.underPackageName}
        | * @see ${target.underPackageName}
        | */
        |
        """.trimMargin()

    /** 写す先が object のとき: 引数なしで object を返す。 */
    fun objectFunction(): String = kdoc() + "$visibility fun $sourceType.$functionName(): $targetType = $targetType\n"

    /** 写す先のコンストラクタ 1 つ分。写し元に同じ名前で型の合うプロパティがあれば既定値にする。 */
    fun classFunction(parameters: List<CopyParameter>): String =
        buildString {
            append(kdoc())
            append("$visibility fun $sourceType.$functionName(\n")
            parameters.forEach { p ->
                append("    ")
                if (p.isVararg) append("vararg ")
                append("${p.name.escapedIdentifier()}: ${p.type}")
                p.defaultProperty?.let { append(" = this.${it.escapedIdentifier()}") }
                append(",\n")
            }
            append("): $targetType = $targetType(\n")
            parameters.forEach { p ->
                val name = p.name.escapedIdentifier()
                append("    $name = ${if (p.isVararg) "*" else ""}$name,\n")
            }
            append(")\n")
        }
}

private val KOTLIN_HARD_KEYWORDS =
    setOf(
        "as",
        "break",
        "class",
        "continue",
        "do",
        "else",
        "false",
        "for",
        "fun",
        "if",
        "in",
        "interface",
        "is",
        "null",
        "object",
        "package",
        "return",
        "super",
        "this",
        "throw",
        "true",
        "try",
        "typealias",
        "typeof",
        "val",
        "var",
        "when",
        "while",
    )

/** Kotlin のキーワードや識別子に使えない文字を含む名前を `` ` `` で囲む。 */
internal fun String.escapedIdentifier(): String =
    if (this in KOTLIN_HARD_KEYWORDS || !all { it.isLetterOrDigit() || it == '_' } || firstOrNull()?.isDigit() == true) "`$this`" else this

/** `a.b.C` の各部分を [escapedIdentifier] する。 */
internal fun String.escapedFqName(): String = split('.').joinToString(".") { it.escapedIdentifier() }

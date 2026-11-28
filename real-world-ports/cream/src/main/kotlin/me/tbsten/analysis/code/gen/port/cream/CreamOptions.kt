package me.tbsten.analysis.code.gen.port.cream

// 参考: TBSten/cream@2162ec8d5261e3cb047de6825545f589102caab5:cream-ksp/src/main/kotlin/me/tbsten/cream/ksp/options/
//   （CreamOptions.kt・CopyFunNamingStrategy.kt・EscapeDot.kt。キーと既定値、名前の決め方を同じにして書き直した）

/** 関数名の決め方（cream の `CopyFunNamingStrategy`）。 */
internal enum class NamingStrategy(
    val key: String,
) {
    UnderPackage("under-package"),
    Diff("diff"),
    SimpleName("simple-name"),
    FullName("full-name"),
    InnerName("inner-name"),
}

/** 名前の `.` の扱い（cream の `EscapeDot`）。 */
internal enum class EscapeDot(
    val key: String,
) {
    LowerCamelCase("lower-camel-case"),
    ReplaceToUnderscore("replace-to-underscore"),
}

/** cream の KSP オプション（`cream.*`）のうち M1 で扱うもの。扱わないキーに既定以外の値があれば [unsupported] に入れる。 */
internal class CreamOptions(
    val copyFunNamePrefix: String,
    val namingStrategy: NamingStrategy,
    val escapeDot: EscapeDot,
    val unsupported: List<String>,
    val invalid: List<String>,
) {
    companion object {
        private val UNSUPPORTED_DEFAULTS =
            mapOf(
                "cream.notCopyToObject" to "false",
                "cream.defaultVisibility" to "inherit",
                "cream.autoValueClassMapping" to "false",
            )

        fun parse(options: Map<String, String>): CreamOptions {
            val invalid = mutableListOf<String>()
            val naming =
                options["cream.copyFunNamingStrategy"]?.let { value ->
                    NamingStrategy.entries.firstOrNull { it.key == value } ?: run {
                        invalid += "cream.copyFunNamingStrategy = $value（${NamingStrategy.entries.joinToString { it.key }} のどれか）"
                        null
                    }
                } ?: NamingStrategy.UnderPackage
            val escape =
                options["cream.escapeDot"]?.let { value ->
                    EscapeDot.entries.firstOrNull { it.key == value } ?: run {
                        invalid += "cream.escapeDot = $value（${EscapeDot.entries.joinToString { it.key }} のどれか）"
                        null
                    }
                } ?: EscapeDot.LowerCamelCase
            val unsupported =
                UNSUPPORTED_DEFAULTS
                    .filter { (key, default) -> options[key]?.let { it.lowercase() != default } ?: false }
                    .keys
                    .sorted()
            return CreamOptions(options["cream.copyFunNamePrefix"] ?: "copyTo", naming, escape, unsupported, invalid)
        }
    }
}

/** 名前の材料（cream の `ClassDeclarationInfo`）。[underPackageName] はパッケージより下の `Outer.Inner`。 */
internal class ClassName(
    val packageName: String,
    val underPackageName: String,
) {
    val simpleName: String get() = underPackageName.substringAfterLast('.')
    val fullName: String get() = if (packageName.isEmpty()) underPackageName else "$packageName.$underPackageName"
}

/** cream の `copyFunctionName` と同じ規則で、[source] から [target] への関数名を作る。 */
internal fun CreamOptions.copyFunctionName(
    source: ClassName,
    target: ClassName,
): String {
    val raw =
        when (namingStrategy) {
            NamingStrategy.UnderPackage -> target.underPackageName
            NamingStrategy.Diff -> target.fullName.substring(source.fullName.commonPrefixWith(target.fullName).length)
            NamingStrategy.SimpleName -> target.simpleName
            NamingStrategy.FullName -> target.fullName
            NamingStrategy.InnerName ->
                target.underPackageName
                    .split(".")
                    .let { if (it.size <= 1) it else it.drop(1) }
                    .joinToString(".")
        }
    val escaped =
        when (escapeDot) {
            EscapeDot.LowerCamelCase ->
                raw
                    .split(".")
                    .joinToString("") { s ->
                        s.replaceFirstChar {
                            it.uppercase()
                        }
                    }.replaceFirstChar { it.lowercase() }
            EscapeDot.ReplaceToUnderscore -> ("_" + raw.replace(".", "_")).replace(Regex("_+"), "_")
        }
    val name = if (copyFunNamePrefix.lastOrNull()?.isLetter() == true) escaped.replaceFirstChar { it.uppercase() } else escaped
    return copyFunNamePrefix + name
}

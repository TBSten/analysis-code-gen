package me.tbsten.analysiscodegen.kspcompatibilitytests.kspt

/**
 * KSP 公式テストの testData 1 ファイルを読んだもの（ADR-024）。
 *
 * 書式は KSP の AbstractKSPTest と Kotlin コンパイラのテスト基盤（`// MODULE:` / `// FILE:`）に合わせる。
 * 上流のテスト基盤は Kotlin コンパイラのテスト用ライブラリ（Maven Central に無い）に依存するため写さず、
 * 必要な書式だけを読む実装を新しく書いた。参考: google/ksp の
 * kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/AbstractKSPTest.kt と AbstractKSPAATest.kt
 */
data class KsptTestData(
    val path: String,
    /** 依存される順。最後が KSP を流すモジュール（main）。 */
    val modules: List<KsptModule>,
    val processorName: String,
    /** `// PROCESSOR INPUT:`。無ければ null（Processor のコンストラクタが変わる）。 */
    val processorArguments: List<String>?,
    val expected: Map<KsptFeatures, String>,
    val jvmTarget: String?,
    /** `// WITH_FIXED_TARGET:` や `// TARGET_BACKEND: NATIVE` があれば Kotlin/Native のターゲット名。 */
    val nativeTarget: String?,
) {
    val mainModule: KsptModule get() = modules.last()
    val libraryModules: List<KsptModule> get() = modules.dropLast(1)

    companion object {
        private val MODULE = Regex("""^//\s*MODULE:\s*(.+)$""")
        private val FILE = Regex("""^//\s*FILE:\s*(\S+)\s*$""")
        private const val COMPILER_MODULE_NAME = "// COMPILER MODULE NAME:"

        fun parse(
            path: String,
            text: String,
        ): KsptTestData {
            val lines = text.lines()
            val defaultFileName = path.substringAfterLast('/')
            val modules = parseModules(lines, defaultFileName)
            val processor =
                lines.single { it.startsWith("// TEST PROCESSOR:") }.substringAfter("// TEST PROCESSOR:").trim()
            val arguments =
                lines
                    .find { it.startsWith("// PROCESSOR INPUT:") }
                    ?.substringAfter("// PROCESSOR INPUT:")
                    ?.split(',')
                    ?.map { it.trim() }
            val directive = { name: String ->
                lines.firstNotNullOfOrNull { l ->
                    Regex("""^//\s*$name:\s*(.+)$""")
                        .find(l)
                        ?.groupValues
                        ?.get(1)
                        ?.trim()
                }
            }
            val native =
                directive("WITH_FIXED_TARGET") ?: if (directive("TARGET_BACKEND") == "NATIVE") "linux_x64" else null
            return KsptTestData(
                path = path,
                modules = modules,
                processorName = processor,
                processorArguments = arguments,
                expected = parseExpected(lines),
                jvmTarget = directive("JVM_TARGET"),
                nativeTarget = native,
            )
        }

        /**
         * Kotlin のテスト基盤と同じく、各ファイルの前を空行で埋めて元のファイルと行番号をそろえる。
         * 期待値に行番号が出るテスト（locations.kt など）があるため。
         */
        private fun parseModules(
            lines: List<String>,
            defaultFileName: String,
        ): List<KsptModule> {
            class FileBuilder(
                val name: String,
                val startLine: Int,
            ) {
                val body = mutableListOf<String>()
            }

            class ModuleBuilder(
                val header: String,
            ) {
                val files = mutableListOf<FileBuilder>()
                var compilerModuleName: String? = null
                var preamble: FileBuilder? = null
            }

            val modules = mutableListOf<ModuleBuilder>()
            var current: ModuleBuilder? = null
            var file: FileBuilder? = null
            lines.forEachIndexed { index, line ->
                val module = MODULE.find(line)
                val fileMatch = FILE.find(line)
                when {
                    module != null -> {
                        current = ModuleBuilder(module.groupValues[1].trim()).also { modules += it }
                        file = null
                    }
                    fileMatch != null -> {
                        val m =
                            current ?: ModuleBuilder("main").also {
                                modules += it
                                current = it
                            }
                        file = FileBuilder(fileMatch.groupValues[1], index + 1).also { m.files += it }
                    }
                    else -> {
                        val m = current
                        if (m != null && line.startsWith(COMPILER_MODULE_NAME)) {
                            m.compilerModuleName = line.substringAfter(COMPILER_MODULE_NAME).trim()
                        }
                        val f =
                            file ?: m?.let { it.preamble ?: FileBuilder(defaultFileName, index).also { p -> it.preamble = p } }
                        f?.body?.add(line)
                    }
                }
            }
            if (modules.isEmpty()) {
                // MODULE も FILE も無い: ファイル全体が main の 1 ファイル
                return listOf(
                    KsptModule("main", "main", emptyList(), emptyList(), listOf(KsptFile(defaultFileName, lines.joinToString("\n")))),
                )
            }
            return modules.map { m ->
                val (name, deps, friends) = parseHeader(m.header)
                val files =
                    m.files.ifEmpty { listOfNotNull(m.preamble) }.map { f ->
                        KsptFile(f.name, (List(f.startLine) { "" } + f.body).joinToString("\n"))
                    }
                KsptModule(name, m.compilerModuleName ?: name, deps, friends, files)
            }
        }

        /** `name(dep1, dep2)(friend)` を読む。 */
        private fun parseHeader(header: String): Triple<String, List<String>, List<String>> {
            val name = header.substringBefore('(').trim()
            val groups =
                Regex("""\(([^)]*)\)""")
                    .findAll(header)
                    .map { g ->
                        g.groupValues[1]
                            .split(',')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }.toList()
            return Triple(name, groups.getOrElse(0) { emptyList() }, groups.getOrElse(1) { emptyList() })
        }

        /** AbstractKSPTest.parseExpectedOutput と同じ規則で、API 機能の組ごとの期待値を作る。 */
        private fun parseExpected(lines: List<String>): Map<KsptFeatures, String> {
            val raw =
                lines
                    .dropWhile { !it.startsWith("// EXPECTED:") }
                    .drop(1)
                    .takeWhile { !it.startsWith("// END") }
            val out = KsptFeatures.ALL.associateWith { mutableListOf<String>() }
            for (line in raw) {
                if (line.startsWith("// EXPECT")) {
                    val conditions =
                        line
                            .drop("// EXPECT".length)
                            .takeWhile { it != ':' }
                            .split(',')
                            .map { it.trim() }
                    val value = line.dropWhile { it != ':' }.drop(1).trim()
                    KsptFeatures.ALL.filter { f -> conditions.all { f.matches(it) } }.forEach { out.getValue(it) += value }
                } else {
                    out.values.forEach { it += line.drop(2).trim() }
                }
            }
            return out.mapValues { it.value.joinToString("\n") }
        }
    }
}

data class KsptModule(
    val name: String,
    /** `// COMPILER MODULE NAME:`（無ければ [name]）。コンパイラと KSP に渡すモジュール名。 */
    val compilerModuleName: String,
    val dependencies: List<String>,
    val friends: List<String>,
    val files: List<KsptFile>,
)

data class KsptFile(
    val name: String,
    val content: String,
) {
    val isJava: Boolean get() = name.endsWith(".java")
}

/** KSP の ApiFeatures の組（backing field と context parameter の有無）。KSPT は 4 つすべてで流す。 */
data class KsptFeatures(
    val enableBackingFields: Boolean,
    val enableContextParameters: Boolean,
) {
    fun matches(condition: String): Boolean {
        val (key, value) = condition.split('=').map { it.trim() }
        val expected = value.lowercase().toBooleanStrict()
        return when (key) {
            "ENABLE_BACKING_FIELDS" -> enableBackingFields == expected
            "ENABLE_CONTEXT_PARAMETERS" -> enableContextParameters == expected
            else -> error("知らない API 機能: $key")
        }
    }

    override fun toString(): String = "backingFields=$enableBackingFields, contextParameters=$enableContextParameters"

    companion object {
        val ALL: List<KsptFeatures> =
            listOf(
                KsptFeatures(enableBackingFields = false, enableContextParameters = false),
                KsptFeatures(enableBackingFields = true, enableContextParameters = false),
                KsptFeatures(enableBackingFields = false, enableContextParameters = true),
                KsptFeatures(enableBackingFields = true, enableContextParameters = true),
            )
    }
}

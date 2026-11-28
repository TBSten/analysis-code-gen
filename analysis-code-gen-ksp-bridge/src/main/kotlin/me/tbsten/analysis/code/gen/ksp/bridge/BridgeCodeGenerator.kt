@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.ksp.bridge

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.intellij.psi.PsiFile
import me.tbsten.analysis.code.gen.engine.spi.DuplicatePolicy
import me.tbsten.analysis.code.gen.engine.spi.OutputKind
import me.tbsten.analysis.code.gen.engine.spi.OutputSink
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import com.google.devtools.ksp.processing.Dependencies as KspDependencies
import me.tbsten.analysis.code.gen.processor.Dependencies as EngineDependencies

/**
 * KSP の [CodeGenerator] を、エンジンの出力トランザクション（[OutputSink]、ADR-014）へ流す（ADR-017、ADR-062）。
 *
 * パスの決め方（拡張子で出力先を選ぶ・パッケージをディレクトリにする）は KSP2 の CodeGeneratorImpl
 * （google/ksp の common-util/src/main/kotlin/com/google/devtools/ksp/common/impl/CodeGeneratorImpl.kt）に合わせて書き直したもの。
 * ファイルへは書かず、stream を閉じたときに内容をまとめて [OutputSink.write] に渡す。
 */
internal class BridgeCodeGenerator : CodeGenerator {
    private var sink: OutputSink? = null

    /** KSFile のパス → PSI。KSP の Dependencies をエンジンの Dependencies に写すのに使う。 */
    private var filesByPath: Map<String, PsiFile> = emptyMap()

    private val opened = hashSetOf<String>()
    private val written = linkedSetOf<String>()

    val knownFiles: List<PsiFile> get() = filesByPath.values.toList()

    /** ラウンド（または finish）の頭で、今の出力口と、その時点のソースを受け取る。 */
    fun begin(
        output: OutputSink,
        files: List<PsiFile>,
    ) {
        sink = output
        filesByPath = files.associateBy { it.pathKey() }
    }

    override val generatedFile: Collection<File>
        get() = written.map(::File)

    override fun createNewFile(
        dependencies: KspDependencies,
        packageName: String,
        fileName: String,
        extensionName: String,
    ): OutputStream = open(dependencies, pathOf(packageName, fileName, extensionName), extensionName)

    override fun createNewFileByPath(
        dependencies: KspDependencies,
        path: String,
        extensionName: String,
    ): OutputStream = open(dependencies, withExtension(path, extensionName), extensionName)

    override fun associate(
        sources: List<KSFile>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = associateByPathInternal(sources, pathOf(packageName, fileName, extensionName), extensionName)

    override fun associateByPath(
        sources: List<KSFile>,
        path: String,
        extensionName: String,
    ) = associateByPathInternal(sources, withExtension(path, extensionName), extensionName)

    override fun associateWithClasses(
        classes: List<KSClassDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = associate(classes.mapNotNull { it.containingFile }, packageName, fileName, extensionName)

    override fun associateWithFunctions(
        functions: List<KSFunctionDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = associate(functions.mapNotNull { it.containingFile }, packageName, fileName, extensionName)

    override fun associateWithProperties(
        properties: List<KSPropertyDeclaration>,
        packageName: String,
        fileName: String,
        extensionName: String,
    ) = associate(properties.mapNotNull { it.containingFile }, packageName, fileName, extensionName)

    private fun open(
        dependencies: KspDependencies,
        path: String,
        extensionName: String,
    ): OutputStream {
        val output = checkNotNull(sink) { "KSP の CodeGenerator はラウンドの外では使えません" }
        // KSP2 と同じく、作った時点で重複を検出する（ラウンドをまたいでも同じ。ADR-017・TRM-04）
        if (!opened.add(path)) throw FileAlreadyExistsException(File(path))
        val engineDependencies = dependencies.toEngine()
        val kind = kindOf(extensionName)
        return object : ByteArrayOutputStream() {
            private var closed = false

            override fun close() {
                if (closed) return
                closed = true
                super.close()
                // 同じパスへの 2 回目は KSP と同じ FileAlreadyExistsException（ADR-017・TRM-04）
                output.write(kind, path, toByteArray(), engineDependencies, DuplicatePolicy.ThrowKspException)
                written += path
            }
        }
    }

    private fun associateByPathInternal(
        sources: List<KSFile>,
        path: String,
        extensionName: String,
    ) {
        val output = checkNotNull(sink) { "KSP の CodeGenerator はラウンドの外では使えません" }
        output.associate(kindOf(extensionName), path, EngineDependencies.isolating(sources.mapNotNull(::psiOf)))
    }

    private fun KspDependencies.toEngine(): EngineDependencies {
        if (isAllSources) return EngineDependencies.all
        val files = originatingFiles.mapNotNull(::psiOf)
        return if (aggregating) EngineDependencies.aggregating(files) else EngineDependencies.isolating(files)
    }

    private fun psiOf(file: KSFile): PsiFile? = filesByPath[file.filePath]

    private companion object {
        fun pathOf(
            packageName: String,
            fileName: String,
            extensionName: String,
        ): String {
            val packageDirs = if (packageName != "") packageName.split(".").joinToString("/") + "/" else ""
            return withExtension(packageDirs + fileName, extensionName)
        }

        fun withExtension(
            path: String,
            extensionName: String,
        ): String = if (extensionName != "") "$path.$extensionName" else path

        // KSP は class を classes ディレクトリへ書くが、エンジンに対応する出力先が無いのでリソースとして扱う（ADR-062）
        fun kindOf(extensionName: String): OutputKind =
            when (extensionName) {
                "kt" -> OutputKind.Kotlin
                "java" -> OutputKind.Java
                else -> OutputKind.Resource
            }
    }
}

/** KSFile.filePath と突き合わせるためのキー（KSP2 の KSFile.filePath は VirtualFile のパス）。 */
internal fun PsiFile.pathKey(): String = virtualFile?.path ?: name

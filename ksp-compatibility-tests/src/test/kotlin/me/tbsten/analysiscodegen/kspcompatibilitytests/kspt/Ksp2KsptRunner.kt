package me.tbsten.analysiscodegen.kspcompatibilitytests.kspt

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.buildApiFeatures
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KSPNativeConfig
import com.google.devtools.ksp.processor.AbstractTestProcessor
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.tools.ToolProvider

/**
 * KSPT の testData を公式 KSP2（K）で流す（ADR-024）。上流の AbstractKSPAATest.runTest と同じ手順:
 * ライブラリのモジュールを kotlinc / javac でコンパイルし、main を KotlinSymbolProcessing で処理して、
 * テスト用 Processor の toResult() を返す。
 */
object Ksp2KsptRunner {
    /** KSP2 2.3.14 の AA（2.4.20-dev）が既定で使う言語バージョン。上流のテストもこの値になる。 */
    private const val LANGUAGE_VERSION = "2.4"

    fun run(
        data: KsptTestData,
        features: KsptFeatures,
        workDir: File,
    ): String {
        workDir.deleteRecursively()
        val outDirs = mutableMapOf<String, File>()
        for (lib in data.libraryModules) {
            val deps = (lib.dependencies + lib.friends).map { outDirs.getValue(it) }
            outDirs[lib.name] = compileLibrary(data, lib, deps, File(workDir, lib.name))
        }
        val main = data.mainModule
        val root = File(workDir, main.name)
        val (kotlinSrc, javaSrc) = writeSources(main, root)
        val processor = newProcessor(data, features)
        val libraries = main.dependencies.map { outDirs.getValue(it) } + runtimeLibraries()
        val friends = main.friends.map { outDirs.getValue(it) }
        val config =
            if (data.nativeTarget != null) {
                KSPNativeConfig
                    .Builder()
                    .apply {
                        target = data.nativeTarget
                        moduleName = main.compilerModuleName
                        sourceRoots = listOf(kotlinSrc)
                        languageVersion = LANGUAGE_VERSION
                        apiVersion = LANGUAGE_VERSION
                        this.libraries = main.dependencies.map { outDirs.getValue(it) }
                        this.friends = friends
                        commonDirs(root)
                    }.build()
            } else {
                KSPJvmConfig
                    .Builder()
                    .apply {
                        moduleName = main.compilerModuleName
                        sourceRoots = listOf(kotlinSrc)
                        javaSourceRoots = listOf(javaSrc)
                        jdkHome = this@Ksp2KsptRunner.jdkHome
                        jvmTarget = data.jvmTarget ?: "1.8"
                        languageVersion = LANGUAGE_VERSION
                        apiVersion = LANGUAGE_VERSION
                        this.libraries = libraries
                        this.friends = friends
                        javaOutputDir = File(root, "kspTest/src/main/java")
                        commonDirs(root)
                    }.build()
            }
        val exitCode = KotlinSymbolProcessing(config, listOf(processor), CommandLineKSPLogger()).execute()
        val result =
            if (exitCode != KotlinSymbolProcessing.ExitCode.OK) {
                listOf("KSP FAILED WITH EXIT CODE: ${exitCode.name}") + processor.toResult()
            } else {
                processor.toResult()
            }
        return result.joinToString("\n").replace(RELOCATED, "")
    }

    /**
     * 期待値は JDK 17 の標準ライブラリを前提にしている（上流の CI が JDK 17。JDK 21 では List.reversed() などが増える）。
     * `acg.kspt.jdkHome` か `JAVA_HOME_17_X64`（GitHub Actions の setup-java）があればそれを、無ければ実行中の JDK を使う。
     */
    val jdkHome: File =
        File(System.getProperty("acg.kspt.jdkHome") ?: System.getenv("JAVA_HOME_17_X64") ?: System.getProperty("java.home"))

    /** [jdkHome] が JDK 17 か。違うとき、JDK の版で出力が変わるテスト（[KsptCases.jdk17Only]）は合わない。 */
    val isJdk17: Boolean by lazy {
        File(jdkHome, "release").takeIf { it.isFile }?.readText()?.contains("JAVA_VERSION=\"17") == true
    }

    /**
     * symbol-processing-aa-embeddable は IntelliJ と Kotlin コンパイラを `ksp.` 接頭辞へ移している。
     * 上流のテスト（移していない jar で動く）の期待値と比べるため、出力からこの接頭辞を外す。
     */
    private val RELOCATED = Regex("""\bksp\.(?=(com\.intellij|org\.jetbrains\.kotlin)\.)""")

    private fun com.google.devtools.ksp.processing.KSPConfig.Builder.commonDirs(root: File) {
        projectBaseDir = root
        classOutputDir = File(root, "kspTest/classes/main")
        kotlinOutputDir = File(root, "kspTest/src/main/kotlin")
        resourceOutputDir = File(root, "kspTest/src/main/resources")
        cachesDir = File(root, "kspTest/kspCaches")
        outputBaseDir = File(root, "kspTest")
        incremental = true
    }

    /** 上流の AbstractKSPTest.mkProcessor と同じく、Processor のコンストラクタに ApiFeatures（と引数）を渡す。 */
    private fun newProcessor(
        data: KsptTestData,
        features: KsptFeatures,
    ): AbstractTestProcessor {
        val cls = Class.forName("com.google.devtools.ksp.processor.${data.processorName}")
        val apiFeatures =
            buildApiFeatures {
                enableBackingFields = features.enableBackingFields
                enableContextParameters = features.enableContextParameters
            }
        return if (data.processorArguments == null) {
            cls.getDeclaredConstructor(ApiFeatures::class.java).newInstance(apiFeatures)
        } else {
            cls.getDeclaredConstructor(List::class.java, ApiFeatures::class.java).newInstance(data.processorArguments, apiFeatures)
        } as AbstractTestProcessor
    }

    private fun writeSources(
        module: KsptModule,
        root: File,
    ): Pair<File, File> {
        val kotlinSrc = File(root, "kotlinSrc").apply { mkdirs() }
        // 上流のテスト基盤と同じく java-sources/<モジュール名>/ に置く。Record* の期待値がこの後ろのパスを出すため
        val javaSrc = File(root, "java-sources/${module.name}").apply { mkdirs() }
        for (f in module.files) {
            File(if (f.isJava) javaSrc else kotlinSrc, f.name).apply {
                parentFile.mkdirs()
                writeText(f.content)
            }
        }
        return kotlinSrc to javaSrc
    }

    private fun compileLibrary(
        data: KsptTestData,
        module: KsptModule,
        deps: List<File>,
        root: File,
    ): File {
        val (kotlinSrc, javaSrc) = writeSources(module, root)
        val out = File(root, "out").apply { mkdirs() }
        val ktFiles = kotlinSrc.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val classpath = (deps + runtimeLibraries()).joinToString(File.pathSeparator) { it.absolutePath }
        if (data.nativeTarget != null) {
            // Native はライブラリを KLIB（メタデータ）として渡す（上流の compileMetadata と同じ）
            compile(
                KotlinMetadataCompiler(),
                listOf("-d", out.path, "-module-name", module.compilerModuleName, "-classpath", classpath) + ktFiles.map { it.path },
            )
            return out
        }
        if (ktFiles.isNotEmpty()) {
            compile(
                K2JVMCompiler(),
                buildList {
                    addAll(listOf(kotlinSrc.path, javaSrc.path, "-d", out.path, "-no-stdlib", "-no-reflect"))
                    addAll(listOf("-module-name", module.compilerModuleName, "-classpath", "$classpath${File.pathSeparator}$kotlinSrc"))
                    data.jvmTarget?.let { addAll(listOf("-jvm-target", it)) }
                },
            )
        }
        val javaFiles = javaSrc.walkTopDown().filter { it.isFile && it.extension == "java" }.toList()
        if (javaFiles.isNotEmpty()) {
            val javac = ToolProvider.getSystemJavaCompiler()
            val err = ByteArrayOutputStream()
            val args = listOf("-classpath", "$classpath${File.pathSeparator}$out", "-d", out.path) + javaFiles.map { it.path }
            check(javac.run(null, null, err, *args.toTypedArray()) == 0) { "javac に失敗した（${module.name}）:\n$err" }
        }
        return out
    }

    private fun compile(
        compiler: Any,
        args: List<String>,
    ) {
        val out = ByteArrayOutputStream()
        // 上流（AbstractKSPAATest.runCompiler）と同じく exec(PrintStream, String[]) を呼ぶ
        val exec = compiler.javaClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
        val code = exec.invoke(compiler, PrintStream(out), args.toTypedArray())
        check(code == ExitCode.OK) { "Kotlin のコンパイルに失敗した（$code）:\n$out" }
    }

    /** ライブラリのコンパイルと KSP に渡す実行時ライブラリ（Kotlin 標準ライブラリと JetBrains の注釈）。 */
    private fun runtimeLibraries(): List<File> =
        listOf(KotlinVersion::class.java, org.jetbrains.annotations.NotNull::class.java).map {
            File(
                it.protectionDomain.codeSource.location
                    .toURI(),
            )
        }
}

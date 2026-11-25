package me.tbsten.analysis.code.gen.differential

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import java.io.File

/**
 * KSP の Processor を包んで、Logger の出力とラウンドごとの生成ファイルを記録する（ADR-023）。
 *
 * kct は Logger の出力から位置と `[ksp]` の印を落とすので、Processor から見える入口（Logger・CodeGenerator・process）で直接記録する。
 * 包むだけで振る舞いは変えない（Logger も CodeGenerator も元に流す）。B（Bridge）も同じ包み方で記録できる。
 */
internal class ProcessorRecorder {
    private val logs = mutableListOf<DiagnosticRecord>()
    private val rounds = mutableListOf<MutableSet<File>>()
    private val generators = mutableListOf<CodeGenerator>()
    private var currentResolver: Resolver? = null
    private val seen = mutableSetOf<File>()

    fun wrap(provider: SymbolProcessorProvider): SymbolProcessorProvider =
        SymbolProcessorProvider { env ->
            generators += env.codeGenerator
            val wrapper = RecordingProcessor()

            // registerProcessorForNewFeatures は KSP が Processor の同一性で見るので、包んだ方を登録させる
            @Suppress("DEPRECATION")
            val register = env.registerProcessorForNewFeatures
            wrapper.delegate = provider.create(env.withOverrides(RecordingLogger(env.logger)) { register(wrapper) })
            wrapper
        }

    /** 記録した Logger の出力（出た順）。 */
    fun logs(): List<DiagnosticRecord> = synchronized(this) { logs.toList() }

    /** ラウンドごとの生成ファイル（絶対パス）。 */
    fun rounds(): List<List<File>> = synchronized(this) { rounds.map { it.sortedBy(File::getPath) } }

    private fun onProcess(resolver: Resolver) =
        synchronized(this) {
            collectGenerated()
            // KSP2 はラウンドごとに新しい Resolver を作るので、Resolver が変わったら次のラウンド
            if (currentResolver !== resolver) {
                currentResolver = resolver
                rounds += mutableSetOf<File>()
            }
        }

    private fun afterProcess() = synchronized(this) { collectGenerated() }

    private fun collectGenerated() {
        val current = rounds.lastOrNull() ?: return
        for (file in generators.flatMap { it.generatedFile }) {
            if (seen.add(file.absoluteFile)) current += file.absoluteFile
        }
    }

    private inner class RecordingProcessor : SymbolProcessor {
        lateinit var delegate: SymbolProcessor

        // KSP は Processor が有効にした API の機能を見て振る舞いを変えるので、元の Processor の値をそのまま見せる
        override val apiFeatures get() = delegate.apiFeatures

        override fun process(resolver: Resolver): List<KSAnnotated> {
            onProcess(resolver)
            try {
                return delegate.process(resolver)
            } finally {
                afterProcess()
            }
        }

        override fun finish() {
            delegate.finish()
            afterProcess()
        }

        override fun onError() = delegate.onError()
    }

    private inner class RecordingLogger(private val delegate: KSPLogger) : KSPLogger {
        private fun record(
            severity: DiagnosticRecord.Severity,
            message: String,
            symbol: KSNode?,
        ) {
            val location = (symbol?.location as? FileLocation)?.let { "${it.filePath}:${it.lineNumber}" }
            synchronized(this@ProcessorRecorder) { logs += DiagnosticRecord(severity, message, location, fromProcessor = true) }
        }

        override fun logging(
            message: String,
            symbol: KSNode?,
        ) {
            record(DiagnosticRecord.Severity.Logging, message, symbol)
            delegate.logging(message, symbol)
        }

        override fun info(
            message: String,
            symbol: KSNode?,
        ) {
            record(DiagnosticRecord.Severity.Info, message, symbol)
            delegate.info(message, symbol)
        }

        override fun warn(
            message: String,
            symbol: KSNode?,
        ) {
            record(DiagnosticRecord.Severity.Warning, message, symbol)
            delegate.warn(message, symbol)
        }

        override fun error(
            message: String,
            symbol: KSNode?,
        ) {
            record(DiagnosticRecord.Severity.Error, message, symbol)
            delegate.error(message, symbol)
        }

        override fun exception(e: Throwable) {
            // スタックトレースは実行系で変わるので、例外の型と文言だけを残す
            record(DiagnosticRecord.Severity.Error, "${e::class.java.name}: ${e.message}", null)
            delegate.exception(e)
        }
    }
}

/**
 * Logger と registerProcessorForNewFeatures だけを差し替えた環境。KSP の版で引数が増えても追従できるよう、主コンストラクタ（引数が最多のもの）に
 * フィールドを宣言順に詰め直す。Kotlin の主コンストラクタの引数とバッキングフィールドは同じ順で並ぶ。
 */
private fun SymbolProcessorEnvironment.withOverrides(
    logger: KSPLogger,
    register: (SymbolProcessor) -> Unit,
): SymbolProcessorEnvironment {
    val type = SymbolProcessorEnvironment::class.java
    val fields =
        type.declaredFields.filter {
            !java.lang.reflect.Modifier
                .isStatic(it.modifiers)
        }
    val ctor = type.constructors.maxBy { it.parameterCount }
    check(ctor.parameterCount == fields.size && ctor.parameterTypes.zip(fields).all { (p, f) -> p == f.type }) {
        "SymbolProcessorEnvironment の形が想定と違う（KSP の版が変わった？）: ${ctor.parameterTypes.toList()} / ${fields.map { it.type }}"
    }
    val args =
        fields.map { f ->
            if (f.type == KSPLogger::class.java) {
                logger
            } else if (f.type == Function1::class.java) {
                register
            } else {
                f.isAccessible = true
                f.get(this)
            }
        }
    return ctor.newInstance(*args.toTypedArray()) as SymbolProcessorEnvironment
}

package me.tbsten.analysis.code.gen.processor

/**
 * analysisCodeGen のモジュール間でだけ使う宣言に付ける印。利用者のコードからは使わない。
 *
 * ADR-012。
 */
@RequiresOptIn(
    message = "analysisCodeGen の内部 API です。互換性は保証されません。",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class InternalAnalysisCodegenApi

/**
 * 今後シグネチャが変わりうる公開 API に付ける印。
 *
 * ADR-012。
 */
@RequiresOptIn(
    message = "実験的な analysisCodeGen の API です。今後変わる可能性があります。",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalAnalysisCodegenApi

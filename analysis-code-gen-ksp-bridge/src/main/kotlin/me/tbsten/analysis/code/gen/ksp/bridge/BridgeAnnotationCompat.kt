package me.tbsten.analysis.code.gen.ksp.bridge

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * 注釈の use-site target（`@receiver:`・`@all:`・`@delegate:` など）。
 *
 * AA 2.4.21 では [KaAnnotation.useSiteTarget] が非推奨になり、ソースに明示した use-site target でも null を返すことがある。
 * KSP2 2.3.14（AA 2.4.20-dev）が見ていた値に合わせるため、null のときは注釈の PSI に書かれた値を使う（ADR-064）。
 */
@Suppress("DEPRECATION")
internal fun KaAnnotation.useSiteTargetOrSource(): AnnotationUseSiteTarget? =
    useSiteTarget ?: (psi as? KtAnnotationEntry)?.useSiteTarget?.getAnnotationUseSiteTarget()

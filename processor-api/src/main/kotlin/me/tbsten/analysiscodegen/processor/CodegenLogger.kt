package me.tbsten.analysiscodegen.processor

import com.intellij.psi.PsiElement

/**
 * ビルド出力へのログ。[error] が 1 回でも呼ばれると、そのビルドは失敗し `onError()` が呼ばれる（ADR-013、API-04）。
 *
 * [element] を渡すと、ファイル名と行番号を付けて出す。
 */
public interface CodegenLogger {
    public fun logging(message: String, element: PsiElement? = null)

    public fun info(message: String, element: PsiElement? = null)

    public fun warn(message: String, element: PsiElement? = null)

    public fun error(message: String, element: PsiElement? = null)

    public fun exception(throwable: Throwable)
}

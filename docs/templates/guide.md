---
title: 要素名（ガイドページの雛形）
description: この要素で何ができるかを 1 文で書く。
---

この要素が何をするものかを 1〜2 文で紹介する。基準は ADR-028 と `docs/doc-lint.toml` にある。

## 基本の使い方

使い方を 1〜2 文で紹介する。

```kotlin
class HelloProcessor : Processor {
    override fun process(context: ProcessingContext) {
        context.files.generate("Hello.kt") { "object Hello" }
    }
}
```

コードの要点を 1〜2 文で説明する。細かい挙動は [詳細ページ](../details/) に置く。

## 設定を変える

設定の要素を 1〜2 文で紹介する。

```kotlin
analysisCodeGen {
    processor("hello")
}
```

既定値と変えたときの違いを 1〜2 文で説明する。

## まとめ

| 要素 | 役割 |
|---|---|
| `Processor` | 生成の入口 |
| `analysisCodeGen { }` | Gradle での設定 |

## 次のステップ

- [次に読むページ](../next/)

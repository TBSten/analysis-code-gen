package me.tbsten.analysis.code.gen.test.architecture.docs

import me.tbsten.katachi.dsl.DeclarationContainerScope

// ドキュメント（ADR-028、ADR-031、ADR-033、ADR-036）
fun DeclarationContainerScope.docsGroup() = "Docs".group {
    summary = "人が読むドキュメント。利用者向けのサイトと実装者向けの設計メモ"

    "DesignMemo".role {
        summary = "実装者向けの設計メモ（ADR の「守ること」を抜き出したもの）"
        layout { "docs/design" / "*.md".file() }
    }
    "SitePage".role {
        summary = "docs サイトのページ（区分は start / guides / testing / migration / internal）"
        layout {
            "docs/site/src/content/docs" {
                "index.mdx".file()
                "{start,guides,testing,migration,internal}" / "*.mdx".file()
            }
        }
    }
    "SiteFeedback".group {
        summary = "docs サイトの開発時だけ使うフィードバック機能（ADR-043）"
        documented = false

        "FeedbackIntegration".role {
            summary = "Astro に組み込む部分（画面の UI・統合・送受信の型）"
            layout { "docs/site/src/integrations/feedback" / "feedback-*.ts".file() }
        }
        "FeedbackProtocol".role {
            summary = "フィードバックの記録の形（ブラウザと dev サーバーで共有する型）"
            layout { "docs/site/src/integrations/feedback" / "protocol.ts".file() }
        }
        "FeedbackCore".role {
            summary = "記録の保存先（Sink）と dev サーバーの中身、その単体テスト（*.test.mjs）"
            layout { "docs/site/src/integrations/feedback/core" / "*.mjs".file() }
        }
        "FeedbackDistCheck".role {
            summary = "本番ビルドにフィードバック機能が入っていないことを確かめるスクリプト"
            layout { "docs/site/src/integrations/feedback" / "check-*.sh".file() }
        }
    }
    "Site".group {
        summary = "docs サイト（Astro Starlight）のプロジェクト"
        documented = false

        "AstroConfig".role {
            summary = "Astro と Starlight の設定（サイドバー・Dokka の取り込み・統合）"
            layout { "docs/site" / "astro.config.mjs".file() }
        }
        "ContentConfig".role {
            summary = "コンテンツ（ページ・翻訳）のスキーマ"
            layout { "docs/site/src" / "content.config.ts".file() }
        }
        "TypeScriptConfig".role {
            summary = "TypeScript の設定"
            layout { "docs/site" / "tsconfig.json".file() }
        }
        "SiteDependency".role {
            summary = "npm の依存とロックファイル"
            layout {
                "docs/site" {
                    "package.json".file()
                    "package-lock.json".file()
                }
            }
        }
        "SiteTranslation".role {
            summary = "UI 文言の翻訳"
            layout { "docs/site/src/content/i18n" / "*.json".file() }
        }
        "SitePublicAsset".role {
            summary = "そのまま配信する静的ファイル（favicon など）"
            layout { "docs/site/public" / "*.svg".file() }
        }
        "SiteReadme".role {
            summary = "docs サイトのビルド・プレビューの手順"
            layout { "docs/site" / "README.md".file() }
        }
    }
    "DocsTooling".group {
        summary = "ドキュメントを書くための説明・雛形・分量チェックの設定"
        documented = false

        "DocsReadme".role {
            summary = "docs/ の構成と、ドキュメントの書き方の入口"
            layout { "docs" / "README.md".file() }
        }
        "PageTemplate".role {
            summary = "ガイドページの雛形（docs-style の型）"
            layout { "docs/templates" / "*.md".file() }
        }
        "DocLintConfig".role {
            summary = "分量チェック（doc_lint.py）の基準（ADR-028）"
            layout { "docs" / "doc-lint.toml".file() }
        }
    }
}

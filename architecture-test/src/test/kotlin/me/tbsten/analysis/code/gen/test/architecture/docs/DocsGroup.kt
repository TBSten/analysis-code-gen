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
    "SiteFeedback".role {
        summary = "docs サイトの開発時だけ使うフィードバック機能（ADR-043）"
        documented = false
        layout {
            "docs/site/src/integrations/feedback" {
                "*.ts".file()
                "*.sh".file()
                "core" / "*.mjs".file()
            }
        }
    }
    "Site".group {
        summary = "docs サイト（Astro Starlight）のプロジェクト"
        documented = false

        "SiteConfig".role {
            summary = "Astro・TypeScript・コンテンツの設定"
            layout {
                "docs/site" {
                    "astro.config.mjs".file()
                    "tsconfig.json".file()
                    "src" / "content.config.ts".file()
                }
            }
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
            layout { "docs/site/public" / "*".file() }
        }
        "SiteReadme".role {
            summary = "docs サイトのビルド・プレビューの手順"
            layout { "docs/site" / "README.md".file() }
        }
    }
    "DocsTooling".role {
        summary = "ドキュメントの説明・ページの雛形・分量チェックの設定"
        documented = false
        layout {
            "docs" {
                "README.md".file()
                "doc-lint.toml".file()
                "templates" / "*.md".file()
            }
        }
    }
}

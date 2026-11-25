package me.tbsten.analysis.code.gen.test.architecture.repository

import me.tbsten.katachi.dsl.DeclarationContainerScope

// リポジトリの運用（Agent 向けのルール・CI・スクリプト・ルート直下のファイル。ADR-026、ADR-029）
fun DeclarationContainerScope.repositoryGroup() = "Repository".group {
    summary = "リポジトリの運用を支えるファイル。プロダクトのコードではない"
    documented = false

    "Agent".group {
        summary = "AI Agent 向けの決まりと手順（ADR-029）"

        "AgentInstruction".role {
            summary = "Agent が最初に読む決まり（Claude 以外の Agent も AGENTS.md から同じものを読む）"
            layout {
                "CLAUDE.md".file()
                "AGENTS.md".file()
            }
        }
        "AgentRule".role {
            summary = "場面ごとの詳しいルール（.claude/rules/<kebab>.md）"
            layout { ".claude/rules" / "*.md".file() }
        }
        "AgentSkill".role {
            summary = "繰り返す手順（.claude/skills/<kebab>/SKILL.md）"
            layout { ".claude/skills/*" / "SKILL.md".file() }
        }
        "AgentSetting".role {
            summary = "Claude Code の設定と SessionStart hook（Maven Central のミラー設定など）"
            layout {
                ".claude" / "settings.json".file()
                ".claude/hooks" {
                    "*.sh".file()
                    "*.init.gradle".file()
                }
            }
        }
    }

    "Ci".group {
        summary = "GitHub Actions と GitHub のテンプレート・設定（ADR-026）"

        "Workflow".role {
            summary = "GitHub Actions のワークフロー（PR・夜間・定期実行・docs のデプロイ）"
            layout { ".github/workflows" / "*.yml".file() }
        }
        "GitHubTemplate".role {
            summary = "Issue と PR のテンプレート"
            layout {
                ".github/ISSUE_TEMPLATE" / "*.yml".file()
                ".github" / "PULL_REQUEST_TEMPLATE.md".file()
            }
        }
        "GitHubConfig".role {
            summary = "CI の説明・コードオーナー・Renovate の設定"
            layout {
                ".github" {
                    "CI.md".file()
                    "CODEOWNERS".file()
                    "renovate.json5".file()
                }
            }
        }
    }

    "RepositoryScript".role {
        summary = "CI と開発で使う Python スクリプト（scripts/<topic>/）と、その単体テスト（test_*.py）"
        layout {
            "scripts" {
                "*.py".file()
                "*" / "*.py".file()
                "*" / "*.sh".file()
                "*" / "README.md".file()
            }
        }
    }

    "Root".group {
        summary = "ルート直下の説明とツールの設定"

        "ProjectReadme".role {
            summary = "リポジトリの入口の説明（モジュール一覧と役割）"
            layout { "README.md".file() }
        }
        "License".role {
            summary = "このリポジトリのライセンスと、写したコードの著作権表示"
            layout {
                "LICENSE".file()
                "NOTICE".file()
            }
        }
        "EditorConfig".role {
            summary = "エディタと ktlint の整形設定"
            layout { ".editorconfig".file() }
        }
        "GitIgnore".role {
            summary = "git に渡さないファイルの一覧（ルートと、独立したビルドごと）"
            layout {
                ".gitignore".file()
                "docs/site" / ".gitignore".file()
                "poc/p*" / "**" / ".gitignore".file()
            }
        }
        "GitAttributes".role {
            summary = "git の改行・差分の扱い"
            layout { ".gitattributes".file() }
        }
    }
}

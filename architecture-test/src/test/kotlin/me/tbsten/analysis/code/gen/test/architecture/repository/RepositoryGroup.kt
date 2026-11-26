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
        "Skill".group {
            summary = "繰り返す手順（skill）。自前のものと、gh skill install で入れた外部のもの"

            "AgentSkill".role {
                summary = "繰り返す手順（.claude/skills/<kebab>/SKILL.md）"
                layout { ".claude/skills/*" / "SKILL.md".file() }
            }
            "VendoredSkillReference".role {
                summary = "gh skill install で入れた外部の skill（katachi）の手順書。手で直さず gh skill update で上げる"
                layout { ".claude/skills/katachi/references" / "**" / "*.md".file() }
            }
            "VendoredSkillAsset".role {
                summary = "外部の skill（katachi）が導入のときに写す、チェックリストの雛形"
                layout { ".claude/skills/katachi/assets/*" / "check-list.html".file() }
            }
        }
        "Setting".group {
            summary = "Claude Code の設定と hook"

            "AgentSetting".role {
                summary = "Claude Code の設定（hook・許可するコマンド）"
                layout { ".claude" / "settings.json".file() }
            }
            "SessionStartHook".role {
                summary = "クラウド環境の SessionStart hook（Maven Central のミラー設定と依存の取得）"
                layout { ".claude/hooks" / "session-start.sh".file() }
            }
            "GradleInitScript".role {
                summary = "SessionStart hook が Gradle に渡す init script"
                layout { ".claude/hooks" / "*.init.gradle".file() }
            }
        }
    }

    "Ci".group {
        summary = "GitHub Actions と GitHub のテンプレート・設定（ADR-026）"

        "Workflow".role {
            summary = "GitHub Actions のワークフロー（PR・夜間・定期実行・docs のデプロイ）"
            layout { ".github/workflows" / "*.yml".file() }
        }
        "IssueTemplate".role {
            summary = "Issue のフォームと、Issue を作るときの案内"
            layout { ".github/ISSUE_TEMPLATE" / "*.yml".file() }
        }
        "PullRequestTemplate".role {
            summary = "PR の本文の見出し"
            layout { ".github" / "PULL_REQUEST_TEMPLATE.md".file() }
        }
        "CiGuide".role {
            summary = "CI の中身と、ブランチ保護・Pages など GitHub 側の設定手順"
            layout { ".github" / "CI.md".file() }
        }
        "CodeOwners".role {
            summary = "レビューを求める人の割り当て"
            layout { ".github" / "CODEOWNERS".file() }
        }
        "RenovateConfig".role {
            summary = "依存の自動更新（Renovate）の設定"
            layout { ".github" / "renovate.json5".file() }
        }
    }

    "Scripts".group {
        summary = "CI と開発で使うスクリプト（scripts/<topic>/）"

        "RepositoryScript".role {
            summary = "CI と開発で使う Python スクリプトと、その単体テスト（test_<script>.py）"
            layout {
                "scripts" {
                    "*.py".file()
                    "*" / "*.py".file()
                }
            }
        }
        "ShellScript".role {
            summary = "CI から呼ぶシェルスクリプト"
            layout { "scripts/*" / "*.sh".file() }
        }
        "ScriptReadme".role {
            summary = "スクリプトの使い方"
            layout { "scripts/*" / "README.md".file() }
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

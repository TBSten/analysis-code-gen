// @ts-check
// docs サイトの設定（ADR-031）。日本語だけのサイトで、GitHub Pages の project site として公開する
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import mermaid from 'astro-mermaid';
import starlightLlmsTxt from 'starlight-llms-txt';
import starlightLinksValidator from 'starlight-links-validator';
// `astro dev` のときだけ Dev Toolbar に Feedback を出す（ADR-043）。build には何も足さない
import feedbackMode from './src/integrations/feedback/feedback-integration.ts';

const repo = 'analysis-code-gen';

export default defineConfig({
	site: 'https://tbsten.github.io',
	base: `/${repo}`,
	integrations: [
		feedbackMode({
			appName: 'analysisCodeGen docs',
			pointer: '案内: analysisCodeGen の docs サイトへのフィードバック。ページは docs/site/src/content/docs/、コード例は fixtures/docs/、決まりは docs/site/README.md と ADR-028 / ADR-031',
		}),
		// starlight より前に置く（```mermaid を Starlight の処理より先に置き換えるため）
		mermaid({ theme: 'neutral', autoTheme: true }),
		starlight({
			title: 'analysisCodeGen',
			description: 'Kotlin Analysis API を直接使える、KMP ファーストなコード生成フレームワーク',
			// 当面は日本語だけ（pm/14）。英語を足すときは locales を足し、日本語を /ja/ へ移すかを ADR で決める
			defaultLocale: 'root',
			locales: { root: { label: '日本語', lang: 'ja' } },
			plugins: [
				starlightLlmsTxt(),
				// 内部リンク切れでビルドを落とす（DOC-01）。Dokka の出力は Starlight の外なので対象外
				starlightLinksValidator({ exclude: [`/${repo}/api/`, `/${repo}/api/**`] }),
			],
			// 長い行は折り返す（レビューで横スクロールさせない）
			expressiveCode: { defaultProps: { wrap: true } },
			social: [{ icon: 'github', label: 'GitHub', href: `https://github.com/TBSten/${repo}` }],
			editLink: { baseUrl: `https://github.com/TBSten/${repo}/edit/main/docs/site/` },
			// 順番は人が決めるので autogenerate は使わない。ページを足したらここにも足す
			sidebar: [
				{ label: '概要', slug: '' },
				{
					label: 'はじめる',
					items: [
						{ label: 'クイックスタート', slug: 'start/quick-start' },
						{ label: 'インストール', slug: 'start/install' },
					],
				},
				{
					label: 'テスト',
					items: [
						{ label: 'Processor をテストする', slug: 'testing' },
						{ label: 'いろいろな状況をテストする', slug: 'testing/advanced' },
					],
				},
				{
					label: '既存の KSP Processor を移行する',
					items: [
						{ label: '移行の流れ', slug: 'migration' },
						{ label: '1. KSP Bridge に載せ替える', slug: 'migration/bridge' },
						{ label: '2. Hybrid で AA を使う', slug: 'migration/hybrid' },
						{ label: '3. Native Processor に書き換える', slug: 'migration/native' },
					],
				},
				{
					label: '内部の仕組み',
					collapsed: true,
					items: [
						{ label: 'アーキテクチャ', slug: 'internal' },
						{ label: 'モジュールと依存の向き', slug: 'internal/modules' },
						{ label: '解析環境と Module Graph', slug: 'internal/analysis-environment' },
						{ label: 'ラウンドと増分処理', slug: 'internal/rounds' },
						{ label: 'KSP Bridge の仕組み', slug: 'internal/ksp-bridge' },
					],
				},
				// Dokka の HTML。docs ワークフローが ./gradlew dokkaGenerate の出力を /api/ に置く。base は Starlight が付ける
				{ label: 'API リファレンス', link: '/api/' },
			],
		}),
	],
});

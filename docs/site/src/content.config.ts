import { defineCollection } from 'astro:content';
import { z } from 'astro/zod';
import { docsLoader, i18nLoader } from '@astrojs/starlight/loaders';
import { docsSchema, i18nSchema } from '@astrojs/starlight/schema';

export const collections = {
	docs: defineCollection({
		loader: docsLoader(),
		schema: docsSchema({
			extend: z.object({
				// 分量チェックのプロファイルの上書き（ADR-028）
				docLint: z.string().optional(),
				// このページのコード例の出所（リポジトリ直下からのパス）。scripts/docsnippets が照合する（ADR-031）
				fixture: z.string().optional(),
			}),
		}),
	}),
	// locales を設定しているので宣言する（無いとビルドのたびに警告が出る）
	i18n: defineCollection({ loader: i18nLoader(), schema: i18nSchema() }),
};

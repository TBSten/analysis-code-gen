@file:OptIn(ExperimentalKatachiApi::class)

package me.tbsten.analysis.code.gen.test.architecture

import me.tbsten.analysis.code.gen.test.architecture.build.buildGroup
import me.tbsten.analysis.code.gen.test.architecture.docs.docsGroup
import me.tbsten.analysis.code.gen.test.architecture.fixtures.fixturesGroup
import me.tbsten.analysis.code.gen.test.architecture.product.productGroup
import me.tbsten.analysis.code.gen.test.architecture.quality.qualityGroup
import me.tbsten.analysis.code.gen.test.architecture.repository.repositoryGroup
import me.tbsten.analysis.code.gen.test.architecture.testing.testingGroup
import me.tbsten.katachi.ExperimentalKatachiApi
import me.tbsten.katachi.dsl.architecture

// ファイルの置き場所の唯一の一覧（ACG-50、ADR-041）。新しい種類のファイルを足す PR は、同じ PR でここも直す
val projectArchitecture = architecture {
    title = "analysis-code-gen"
    description = "Kotlin Analysis API を直接使うコード生成基盤。どの種類のファイルをどこに置くかを Role ごとに書く。"

    productGroup()
    testingGroup()
    fixturesGroup()
    qualityGroup()
    docsGroup()
    buildGroup()
    repositoryGroup()
}

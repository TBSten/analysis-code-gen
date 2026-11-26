@file:OptIn(ExperimentalKatachiApi::class)

package me.tbsten.analysis.code.gen.test.architecture

import io.kotest.core.spec.style.FreeSpec
import me.tbsten.katachi.ExperimentalKatachiApi
import me.tbsten.katachi.check.FileConstraintCheck
import me.tbsten.katachi.check.assert

class ProjectArchitectureTest : FreeSpec({
    "プロジェクトの構成が定義どおりになっている" {
        projectArchitecture.assert(FileConstraintCheck())
    }
})

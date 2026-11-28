package me.tbsten.analysis.code.gen.e2e.det

import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.tbsten.analysis.code.gen.e2e.support.Fixture
import me.tbsten.analysis.code.gen.e2e.support.fixtureProject
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec

// 5.8 決定性（DET）のうち E2E のケース。DET-02 は DetPbtTest
class DetE2ETest : E2ESpec({
    case("DET-01", "同じ入力で 2 回クリーンビルドすると、生成物がバイト単位で一致する") {
        val p = fixtureProject(Fixture.JVM)
        p.build("clean", "analysisCodeGenMainSourceSet", "--no-build-cache")
        val first = p.file("build/generated/analysisCodeGen").bytesByPath()
        p.build("clean", "analysisCodeGenMainSourceSet", "--no-build-cache", "--rerun-tasks")
        val second = p.file("build/generated/analysisCodeGen").bytesByPath()
        first.shouldNotBeEmpty()
        second.keys shouldBe first.keys
        first.forEach { (path, bytes) -> second.getValue(path).contentEquals(bytes) shouldBe true }
    }

    case("DET-03", "別のディレクトリに checkout してビルドしても生成物が一致する（絶対パスが混ざらない）") {
        val a = fixtureProject(Fixture.JVM_MULTI)
        val b = fixtureProject(Fixture.JVM_MULTI)
        (a.dir == b.dir) shouldBe false
        a.build("build", "--no-build-cache")
        b.build("build", "--no-build-cache")
        for (module in listOf("lib", "app")) {
            b.allGenerated(module) shouldBe a.allGenerated(module)
            a.allGenerated(module).values.none {
                it.contains(a.dir.parentFile.absolutePath) ||
                    it.contains(a.dir.parentFile.invariantSeparatorsPath)
            } shouldBe
                true
        }
    }
})

private fun java.io.File.bytesByPath(): Map<String, ByteArray> =
    walkTopDown().filter { it.isFile }.associate { it.relativeTo(this).invariantSeparatorsPath to it.readBytes() }.toSortedMap()

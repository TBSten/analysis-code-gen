package me.tbsten.analysiscodegen.e2e.lic

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.tbsten.analysiscodegen.e2e.support.repoRoot
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import java.util.concurrent.TimeUnit

// 5.27 ライセンス（LIC）。検査の本体は scripts/lic01/check_provenance.py（ADR-018）。CI の lic-01.yml でも流しているが、
// ケースとして数えるためにテストからも呼ぶ。script と NOTICE・port-map が P1 でそろったので最初から有効にする（ADR-037）

class LicenseProvenanceTest : UnitSpec({
    case("LIC-01", "KSP から移植したファイルすべてに出所（パス・コミット ID）と著作権表示があり、NOTICE がある（スクリプトで検査）") {
        val process =
            ProcessBuilder("python3", "scripts/lic01/check_provenance.py", "--root", repoRoot.absolutePath)
                .directory(repoRoot)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(5, TimeUnit.MINUTES) shouldBe true

        withClue(output) {
            process.exitValue() shouldBe 0
            output shouldContain "LIC-01: PASS"
        }
        repoRoot.resolve("NOTICE").isFile shouldBe true
    }
})

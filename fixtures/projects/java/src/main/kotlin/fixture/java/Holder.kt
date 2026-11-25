package fixture.java

import me.tbsten.analysiscodegen.tp.DeferUntil
import me.tbsten.analysiscodegen.tp.GenerateJava
import me.tbsten.analysiscodegen.tp.Mapped
import me.tbsten.analysiscodegen.tp.TpResourceMarker

/** Java の型を持つ。Mapper の型表記に fixture.java.JavaPerson が出れば Java の宣言を解決できている。 */
@Mapped
data class Holder(
    val person: JavaPerson,
    val people: List<JavaPerson>,
    val builder: java.lang.StringBuilder?,
)

/** 生成した Java（ExportedJava）を Kotlin から使う。 */
@GenerateJava
class Exported : TpResourceMarker

/** 手書きの Java クラスはラウンド 1 から解決できる。 */
@DeferUntil("fixture.java.JavaPerson")
class NeedsJava

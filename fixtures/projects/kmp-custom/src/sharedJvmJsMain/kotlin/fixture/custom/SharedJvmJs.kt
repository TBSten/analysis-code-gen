package fixture.custom

import me.tbsten.analysiscodegen.tp.Mapped

/**
 * 中間 SourceSet のクラス。生成物 JvmJsModelMapper は jvm と js の両方から使う（KMP-17）。
 *
 * KSP2 では中間 SourceSet の分をターゲットごとに生成するため、K2 の規則上この SourceSet からは生成物を参照できない。
 * 両方のエンジンで同じソースをビルドするため、参照はターゲットの SourceSet（jvmMain・jsMain）から行う。
 */
@Mapped
data class JvmJsModel(val id: Int, val name: String)

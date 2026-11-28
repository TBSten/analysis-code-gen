// PATTERN: CMN-O-0005
// TITLE: 背景コード: パッケージ宣言の無いファイル・バッククォートの名前・@file アノテーション・const
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: verified
// FILE: main/NoPackage.kt
@file:JvmName("NoPackageKt")

const val GLOBAL_LIMIT = 10

class `Strange Name`(val `when`: String, val `is`: Boolean)

object TopLevelObject {
    @JvmStatic
    fun ping(): String = "pong"
}

// PATTERN: KOI-R-0016
// TITLE: codymikol/git-down / root / ObjectMapperBean
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/beans/ObjectMapperBean.kt
// LICENSE: MIT
// STATUS: rejected
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので K（KSP2 2.3.14）: K でビルドが失敗した: Unresolved reference 'jackson'.
// FILE: root/main/com/codymikol/beans/ObjectMapperBean.kt
package com.codymikol.beans

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.koin.core.annotation.Single

@Single
class ObjectMapperBean {
    val value: ObjectMapper by lazy { ObjectMapper().registerKotlinModule() }

}

// PATTERN: KOI-R-0036
// TITLE: darriousliu/PiPixiv / common:network / HttpClientAnnotation
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:darriousliu/PiPixiv@1a2e76353cc3/common/network/src/commonMain/kotlin/com/mrl/pixiv/common/network/HttpClientAnnotation.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate
// FILE: network/commonMain/com/mrl/pixiv/common/network/HttpClientAnnotation.kt
package com.mrl.pixiv.common.network

import org.koin.core.annotation.Named

@Named
annotation class ApiClient

@Named
annotation class AuthClient

@Named
annotation class ImageClient

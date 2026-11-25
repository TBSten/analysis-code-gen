package fixture.custom

fun describeJvmJs(): String = "${platformName()}:${JvmJsModelMapper.toMap(JvmJsModel(1, "a"))}"

package com.example.nfc_demo.access_management


data class Department(
    val name: String,
    val code: Int
) {
    companion object {
        val DEPARTMENTS = listOf(
            Department("Admin 2", 0x0001),
            Department("Admin all", 0x0002),
            Department("FRM", 0x0003),
            Department("FRM OSE", 0x0004),
            Department("General", 0x0005),
            Department("Infosec", 0x0006),
            Department("Infosec OSE", 0x0007),
            Department("IT", 0x0008),
            Department("IT only", 0x0009),
            Department("Operations", 0x000A),
            Department("Operations OSE", 0x000B),
            Department("Service Provider", 0x000C)
        )
    }
}


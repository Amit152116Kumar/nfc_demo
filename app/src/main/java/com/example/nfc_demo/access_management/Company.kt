package com.example.nfc_demo.access_management

data class Company(
    val name: String,
    val code: Int
) {
    companion object {
        val COMPANIES = listOf(
            Company("NPCI", 0x00000001),
            Company("NIPL", 0x00000002),
            Company("NBSL", 0x00000003),
            Company("BBPS", 0x00000004)
        )
    }
}

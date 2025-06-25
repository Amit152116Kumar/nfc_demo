package com.example.nfc_demo.access_management

data class BaseLocation(
    val name: String,
    val code: Int
) {
    companion object {
        val LOCATIONS = listOf(
            BaseLocation("Bangalore", 0x01),
            BaseLocation("Bhopal", 0x02),
            BaseLocation("Chennai - Kanchipuram", 0x03),
            BaseLocation("Chennai - Porur", 0x04),
            BaseLocation("Delhi", 0x05),
            BaseLocation("Guwahati", 0x06),
            BaseLocation("Hyderabad - Gandipet", 0x07),
            BaseLocation("Hyderabad - HITEC city", 0x08),
            BaseLocation("Kerala", 0x09),
            BaseLocation("Kolkata", 0x0A),
            BaseLocation("Lucknow", 0x0B),
            BaseLocation("Mumbai - BKC", 0x0C),
            BaseLocation("Mumbai - Goregaon", 0x0D),
            BaseLocation("Mumbai - Westin", 0x0E)
        )
    }
}

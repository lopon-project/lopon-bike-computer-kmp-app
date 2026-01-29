package ru.lopon.core.settings

object WheelCircumferenceValidator {
    const val MIN_MM = 1000.0

    const val MAX_MM = 3500.0

    fun validate(mm: Double): ValidationResult {
        return when {
            mm < MIN_MM -> ValidationResult.TooSmall(MIN_MM)
            mm > MAX_MM -> ValidationResult.TooLarge(MAX_MM)
            else -> ValidationResult.Valid
        }
    }

    fun isValid(mm: Double): Boolean = mm in MIN_MM..MAX_MM

    fun commonSizes(): List<WheelSize> = listOf(
        // Шоссейные (700c)
        WheelSize("700x20c", 2074.0, "Road racing"),
        WheelSize("700x23c", 2096.0, "Road standard"),
        WheelSize("700x25c", 2105.0, "Road comfortable"),
        WheelSize("700x28c", 2136.0, "Road/gravel"),
        WheelSize("700x32c", 2155.0, "Gravel"),
        WheelSize("700x35c", 2168.0, "Touring"),
        WheelSize("700x38c", 2180.0, "Touring/hybrid"),
        WheelSize("700x40c", 2200.0, "Touring wide"),
        WheelSize("700x45c", 2237.0, "Universal touring wide"),

        // 26" MTB
        WheelSize("26x1.5", 1985.0, "MTB narrow"),
        WheelSize("26x1.75", 2023.0, "MTB light"),
        WheelSize("26x1.95", 2050.0, "MTB standard"),
        WheelSize("26x2.0", 2055.0, "MTB trail"),
        WheelSize("26x2.1", 2068.0, "MTB trail wide"),
        WheelSize("26x2.25", 2100.0, "MTB all-mountain"),

        // 27.5" / 650B
        WheelSize("27.5x1.75", 2065.0, "MTB 27.5 narrow"),
        WheelSize("27.5x2.0", 2089.0, "MTB 27.5 standard"),
        WheelSize("27.5x2.1", 2107.0, "MTB 27.5 trail"),
        WheelSize("27.5x2.25", 2141.0, "MTB 27.5 wide"),
        WheelSize("27.5x2.35", 2177.0, "MTB 27.5 enduro"),

        // 29" / 700c MTB
        WheelSize("29x1.95", 2260.0, "MTB 29 narrow"),
        WheelSize("29x2.0", 2272.0, "MTB 29 standard"),
        WheelSize("29x2.1", 2288.0, "MTB 29 trail"),
        WheelSize("29x2.25", 2326.0, "MTB 29 wide"),
        WheelSize("29x2.35", 2346.0, "MTB 29 enduro")
    )

    fun findClosestSize(circumferenceMm: Double): WheelSize? {
        if (!isValid(circumferenceMm)) return null
        return commonSizes().minByOrNull {
            kotlin.math.abs(it.circumferenceMm - circumferenceMm)
        }
    }
}

sealed class ValidationResult {
    data object Valid : ValidationResult()

    data class TooSmall(val minValue: Double) : ValidationResult()

    data class TooLarge(val maxValue: Double) : ValidationResult()
}

data class WheelSize(
    val name: String,
    val circumferenceMm: Double,
    val description: String = ""
)

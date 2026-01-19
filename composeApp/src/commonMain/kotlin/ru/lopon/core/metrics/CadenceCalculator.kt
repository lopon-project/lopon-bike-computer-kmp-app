package ru.lopon.core.metrics


class CadenceCalculator(
    private val smoothingWindowSize: Int = 3 // среднее за три оборота, скользящее окно
) {
    private val cadenceHistory = ArrayDeque<Double>()
    private var cadenceSum: Double = 0.0
    private var cadenceCount: Int = 0

    fun addCadence(cadenceRpm: Double?): CadenceStats {
        if (cadenceRpm == null) {
            return CadenceStats(
                current = cadenceHistory.lastOrNull(),
                average = if (cadenceCount > 0) cadenceSum / cadenceCount else null
            )
        }

        cadenceHistory.addLast(cadenceRpm)
        if (cadenceHistory.size > smoothingWindowSize) {
            cadenceHistory.removeFirst()
        }

        cadenceSum += cadenceRpm
        cadenceCount++

        // Рассчитываем сглаженный текущий каденс
        val smoothedCurrent = if (cadenceHistory.isNotEmpty()) {
            cadenceHistory.sum() / cadenceHistory.size
        } else {
            null
        }

        return CadenceStats(
            current = smoothedCurrent,
            average = cadenceSum / cadenceCount
        )
    }


    fun reset() {
        cadenceHistory.clear()
        cadenceSum = 0.0
        cadenceCount = 0
    }

    val hasData: Boolean get() = cadenceCount > 0
}


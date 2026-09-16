package com.example.data.model

import java.util.Locale
import kotlin.math.roundToInt

data class ImtuStandard(
    val age: Int,
    val isMale: Boolean,
    val sdMinus3: Float,
    val sdMinus2: Float,
    val median: Float,
    val sdPlus1: Float,
    val sdPlus2: Float
)

data class ImtuResult(
    val bmi: Float,
    val zScore: Float,
    val zScoreFormatted: String,
    val category: String, // "Gizi Buruk", "Gizi Kurang", "Gizi Baik (Normal)", "Gizi Lebih", "Obesitas"
    val categoryRange: String, // e.g. "-2 SD s/d +1 SD"
    val standardReference: String = "Standar Antropometri Kemenkes RI (Permenkes No. 2/2020)",
    val normalRangeText: String, // e.g. "16.7 – 24.2 kg/m²"
    val statusColorType: String // "NORMAL", "WARNING", "DANGER"
)

data class MealDistribution(
    val breakfastCalories: Int,
    val lunchCalories: Int,
    val dinnerCalories: Int,
    val snackCalories: Int
)

data class BmrTdeeResult(
    val bmr: Float,
    val tdee: Float,
    val activityMultiplier: Float,
    val activityDescription: String,
    val formulaUsed: String,
    val mealDistribution: MealDistribution
)

object GiziCalculator {

    // Kemenkes RI Permenkes No. 2 Tahun 2020 (Standar Antropometri Anak IMT/U Usia 10-18 Tahun)
    private val boysTable = mapOf(
        10 to ImtuStandard(10, true, 12.4f, 13.7f, 16.6f, 19.4f, 22.2f),
        11 to ImtuStandard(11, true, 12.7f, 14.1f, 17.2f, 20.2f, 23.2f),
        12 to ImtuStandard(12, true, 13.2f, 14.5f, 17.8f, 21.0f, 24.2f),
        13 to ImtuStandard(13, true, 13.7f, 15.1f, 18.5f, 21.9f, 25.2f),
        14 to ImtuStandard(14, true, 14.3f, 15.7f, 19.2f, 22.7f, 26.0f),
        15 to ImtuStandard(15, true, 14.7f, 16.2f, 19.9f, 23.5f, 26.8f),
        16 to ImtuStandard(16, true, 15.1f, 16.7f, 20.5f, 24.2f, 27.5f),
        17 to ImtuStandard(17, true, 15.4f, 17.1f, 21.1f, 24.9f, 28.2f),
        18 to ImtuStandard(18, true, 15.7f, 17.5f, 21.7f, 25.6f, 28.8f)
    )

    private val girlsTable = mapOf(
        10 to ImtuStandard(10, false, 12.2f, 13.5f, 16.6f, 19.6f, 22.6f),
        11 to ImtuStandard(11, false, 12.6f, 13.9f, 17.2f, 20.5f, 23.7f),
        12 to ImtuStandard(12, false, 13.1f, 14.4f, 18.0f, 21.5f, 25.0f),
        13 to ImtuStandard(13, false, 13.6f, 14.9f, 18.8f, 22.5f, 26.2f),
        14 to ImtuStandard(14, false, 14.0f, 15.4f, 19.6f, 23.4f, 27.3f),
        15 to ImtuStandard(15, false, 14.4f, 15.9f, 20.2f, 24.1f, 28.1f),
        16 to ImtuStandard(16, false, 14.6f, 16.2f, 20.7f, 24.7f, 28.8f),
        17 to ImtuStandard(17, false, 14.7f, 16.4f, 21.0f, 25.2f, 29.3f),
        18 to ImtuStandard(18, false, 14.7f, 16.4f, 21.3f, 25.5f, 29.7f)
    )

    /**
     * Hitung IMT dan Klasifikasi IMT/U sesuai Standar Kemenkes RI (Permenkes No. 2/2020)
     */
    fun calculateImtu(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String
    ): ImtuResult {
        val heightM = heightCm / 100f
        val rawBmi = if (heightM > 0) weightKg / (heightM * heightM) else 0f
        val bmi = (rawBmi * 10f).roundToInt() / 10f

        val isMale = gender.contains("Laki", ignoreCase = true) || gender.contains("Pria", ignoreCase = true)
        val clampedAge = age.coerceIn(10, 18)
        val std = if (isMale) {
            boysTable[clampedAge] ?: boysTable[16]!!
        } else {
            girlsTable[clampedAge] ?: girlsTable[16]!!
        }

        // Z-score calculation based on standard deviations
        val zScore = when {
            bmi < std.sdMinus2 -> {
                // Below -2 SD
                val sdWidth = (std.sdMinus2 - std.sdMinus3).coerceAtLeast(0.5f)
                -2f - ((std.sdMinus2 - bmi) / sdWidth)
            }
            bmi < std.median -> {
                // Between -2 SD and Median
                val sdWidth = (std.median - std.sdMinus2) / 2f
                -2f + ((bmi - std.sdMinus2) / sdWidth)
            }
            bmi <= std.sdPlus1 -> {
                // Between Median and +1 SD
                val sdWidth = (std.sdPlus1 - std.median).coerceAtLeast(0.5f)
                (bmi - std.median) / sdWidth
            }
            else -> {
                // Above +1 SD
                val sdWidth = (std.sdPlus2 - std.sdPlus1).coerceAtLeast(0.5f)
                1f + ((bmi - std.sdPlus1) / sdWidth)
            }
        }

        val roundedZ = (zScore * 10f).roundToInt() / 10f
        val zScoreStr = String.format(Locale.US, "%+.1f SD", roundedZ)

        // Kategori Permenkes No. 2 Tahun 2020:
        // Gizi Buruk: < -3 SD
        // Gizi Kurang: -3 SD s/d < -2 SD
        // Gizi Baik (Normal): -2 SD s/d +1 SD
        // Gizi Lebih: > +1 SD s/d +2 SD
        // Obesitas: > +2 SD
        val (category, rangeText, colorType) = when {
            bmi < std.sdMinus3 -> Triple("Gizi Buruk", "< -3 SD", "DANGER")
            bmi < std.sdMinus2 -> Triple("Gizi Kurang", "-3 SD s/d < -2 SD", "WARNING")
            bmi <= std.sdPlus1 -> Triple("Gizi Baik (Normal)", "-2 SD s/d +1 SD", "NORMAL")
            bmi <= std.sdPlus2 -> Triple("Gizi Lebih", "> +1 SD s/d +2 SD", "WARNING")
            else -> Triple("Obesitas", "> +2 SD", "DANGER")
        }

        val normalRange = "${std.sdMinus2} – ${std.sdPlus1} kg/m²"

        return ImtuResult(
            bmi = bmi,
            zScore = roundedZ,
            zScoreFormatted = zScoreStr,
            category = category,
            categoryRange = rangeText,
            standardReference = "Standar Antropometri Kemenkes RI (Permenkes No. 2/2020)",
            normalRangeText = normalRange,
            statusColorType = colorType
        )
    }

    /**
     * Rumus Harris-Benedict yang diperbarui (Roza and Shizgal 1984):
     * Pria:
     *   BMR = 88,362 + (13,397 x berat dalam kg) + (4,799 x tinggi dalam cm) - (5,677 x usia)
     * Wanita:
     *   BMR = 447,593 + (9,247 x berat dalam kg) + (3,098 x tinggi dalam cm) - (4,330 x usia)
     */
    fun calculateBmr(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): Float {
        val bmr = if (isMale) {
            88.362f + (13.397f * weightKg) + (4.799f * heightCm) - (5.677f * age.toFloat())
        } else {
            447.593f + (9.247f * weightKg) + (3.098f * heightCm) - (4.330f * age.toFloat())
        }
        return (bmr * 10f).roundToInt() / 10f
    }

    /**
     * Hitung TDEE:
     * Jika aktif berolahraga 3-5 kali dalam seminggu: kalikan 1,55
     * Jika jarang olahraga: kalikan 1,2
     */
    fun calculateTdee(
        bmr: Float,
        isActive3to5Times: Boolean
    ): BmrTdeeResult {
        val multiplier = if (isActive3to5Times) 1.55f else 1.2f
        val activityDesc = if (isActive3to5Times) {
            "Aktif berolahraga 3-5 kali/minggu (Faktor Aktivitas: 1,55)"
        } else {
            "Jarang olahraga (Faktor Aktivitas: 1,20)"
        }

        val rawTdee = bmr * multiplier
        val tdee = (rawTdee * 10f).roundToInt() / 10f

        val formulaText = "TDEE = BMR ($bmr kkal) × ${String.format(Locale.US, "%.2f", multiplier)}"

        // Rekomendasi pembagian kalori harian gizi seimbang remaja
        val bFast = (tdee * 0.25f).roundToInt()
        val lunch = (tdee * 0.35f).roundToInt()
        val dinner = (tdee * 0.30f).roundToInt()
        val snack = (tdee * 0.10f).roundToInt()

        return BmrTdeeResult(
            bmr = bmr,
            tdee = tdee,
            activityMultiplier = multiplier,
            activityDescription = activityDesc,
            formulaUsed = formulaText,
            mealDistribution = MealDistribution(
                breakfastCalories = bFast,
                lunchCalories = lunch,
                dinnerCalories = dinner,
                snackCalories = snack
            )
        )
    }
}

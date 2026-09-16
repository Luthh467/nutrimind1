package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // e.g. "SISWA-001" or email hash
    val email: String,
    val name: String,
    val grade: String, // e.g. "X MA 1", "XI IPA", "XII MA"
    val age: Int,
    val gender: String, // "Laki-laki" or "Perempuan"
    val studentIdNumber: String, // NISN
    val role: String = "SISWA", // "SISWA" or "UKS"
    val createdAt: Long = System.currentTimeMillis(),
    val isProfileComplete: Boolean = false,
    val lastLoginAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "uks_officers")
data class UksOfficerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val madrasahName: String,
    val officerCode: String,
    val passwordHash: String,
    val isVerified: Boolean = false,
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "gizi_checks")
data class GiziCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Float,
    val heightCm: Float,
    val bmi: Float,
    val bmiCategory: String, // "Gizi Buruk", "Gizi Kurang", "Gizi Baik (Normal)", "Gizi Lebih", "Obesitas"
    val riskCategory: String, // "Risiko Rendah", "Perlu Perhatian", "Risiko Tinggi"
    val breakfastHabit: String,
    val vegetableIntake: String,
    val fruitIntake: String,
    val junkFoodIntake: String,
    val sweetDrinkIntake: String,
    val physicalActivity: String,
    val sedentaryTime: String,
    val factorsToNote: String,
    val generalAdvice: String,
    val zScore: Float = 0f,
    val standardReference: String = "Permenkes RI No. 2 Tahun 2020 (IMT/U)"
)

@Entity(tableName = "daily_checks")
data class DailyCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val date: String, // e.g. "15 Sep 2026"
    val timestamp: Long = System.currentTimeMillis(),
    val breakfast: String, // "Ya" / "Tidak"
    val mealsCount: String, // "1 kali", "2 kali", "3 kali", ">3 kali"
    val eatVeg: String, // "Ya" / "Belum"
    val eatFruit: String, // "Ya" / "Belum"
    val sweetDrinks: String, // "Tidak ada", "1 kali", "2 kali", ">=3 kali"
    val activityDone: String, // "Ya" / "Tidak"
    val activityDuration: String, // "<30 menit", "30–60 menit", ">60 menit"
    val sedentaryHours: String, // "<3 jam", "3-6 jam", ">6 jam"
    val sleepHours: String, // "<6 jam", "6–8 jam", ">8 jam"
    val bodyCondition: String, // "Sangat Bugar", "Cukup Baik", "Kurang Berenergi", "Mudah Lelah", "Sakit Perut / Pusing"
    val summaryText: String,
    val aiAdvice: String,
    val bmr: Float = 0f,
    val tdee: Float = 0f,
    val activityLevel: String = ""
)

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String = "",
    val foodName: String,
    val detectedItems: String,
    val foodGroups: String,
    val carbSource: String,
    val proteinSource: String,
    val vegFruitSource: String,
    val estimatedCalories: String,
    val balanceEvaluation: String,
    val educationalNote: String
)

@Entity(tableName = "education_articles")
data class EducationArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // "Gizi Seimbang", "Sarapan", "Buah dan Sayur", "Makanan dan Minuman", "Aktivitas Fisik", "Tidur dan Kebiasaan Sehat"
    val summary: String,
    val content: String,
    val readTime: String,
    val isFavorite: Boolean = false,
    val recommendedForRisk: String = "ALL" // "ALL", "Risiko Tinggi", "Perlu Perhatian"
)

@Entity(tableName = "uks_follow_ups")
data class UksFollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val officerName: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionNote: String
)

package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class NutriMindRepository(
    private val database: AppDatabase
) {
    private val userDao = database.userDao()
    private val uksOfficerDao = database.uksOfficerDao()
    private val giziCheckDao = database.giziCheckDao()
    private val dailyCheckDao = database.dailyCheckDao()
    private val foodLogDao = database.foodLogDao()
    private val educationArticleDao = database.educationArticleDao()
    private val uksFollowUpDao = database.uksFollowUpDao()

    // Users
    suspend fun getUserById(id: String): UserEntity? = userDao.getUserById(id)
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    fun getAllStudents(): Flow<List<UserEntity>> = userDao.getAllStudents()
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    // UKS Officers
    suspend fun getOfficerByEmail(email: String): UksOfficerEntity? = uksOfficerDao.getOfficerByEmail(email)
    fun getAllOfficers(): Flow<List<UksOfficerEntity>> = uksOfficerDao.getAllOfficers()
    suspend fun registerOfficer(officer: UksOfficerEntity) = uksOfficerDao.insertOfficer(officer)
    suspend fun verifyOfficer(id: String, verified: Boolean) = uksOfficerDao.updateVerification(id, verified)

    // Gizi Checks
    fun getChecksForStudent(studentId: String): Flow<List<GiziCheckEntity>> = giziCheckDao.getChecksForStudent(studentId)
    fun getLatestCheckForStudent(studentId: String): Flow<GiziCheckEntity?> = giziCheckDao.getLatestCheckForStudent(studentId)
    fun getAllChecks(): Flow<List<GiziCheckEntity>> = giziCheckDao.getAllChecks()
    suspend fun saveGiziCheck(check: GiziCheckEntity): Long = giziCheckDao.insertCheck(check)

    // Daily Checks
    fun getDailyChecksForStudent(studentId: String): Flow<List<DailyCheckEntity>> = dailyCheckDao.getDailyChecksForStudent(studentId)
    suspend fun getDailyCheckByDate(studentId: String, date: String): DailyCheckEntity? = dailyCheckDao.getDailyCheckByDate(studentId, date)
    suspend fun saveDailyCheck(check: DailyCheckEntity): Long = dailyCheckDao.insertDailyCheck(check)

    // Food Logs
    fun getFoodLogsForStudent(studentId: String): Flow<List<FoodLogEntity>> = foodLogDao.getFoodLogsForStudent(studentId)
    suspend fun saveFoodLog(log: FoodLogEntity): Long = foodLogDao.insertFoodLog(log)

    // Education Articles
    fun getAllArticles(): Flow<List<EducationArticleEntity>> = educationArticleDao.getAllArticles()
    fun getArticlesByCategory(category: String): Flow<List<EducationArticleEntity>> = educationArticleDao.getArticlesByCategory(category)
    suspend fun toggleArticleFavorite(id: String, fav: Boolean) = educationArticleDao.toggleFavorite(id, fav)

    // UKS Follow Ups
    fun getFollowUpsForStudent(studentId: String): Flow<List<UksFollowUpEntity>> = uksFollowUpDao.getFollowUpsForStudent(studentId)
    suspend fun saveFollowUp(followUp: UksFollowUpEntity): Long = uksFollowUpDao.insertFollowUp(followUp)

    /**
     * Compute BMI, Z-score IMT/U according to Standar Kemenkes RI (Permenkes No. 2/2020),
     * risk category, factors to note, and advice
     */
    fun evaluateGizi(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String,
        breakfastHabit: String,
        vegIntake: String,
        fruitIntake: String,
        junkFoodIntake: String,
        sweetDrinkIntake: String,
        physicalActivity: String,
        sedentaryTime: String
    ): GiziEvaluationResult {
        val imtuResult = GiziCalculator.calculateImtu(
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            gender = gender
        )

        val formattedBmi = imtuResult.bmi
        val bmiCategory = imtuResult.category // "Gizi Buruk", "Gizi Kurang", "Gizi Baik (Normal)", "Gizi Lebih", "Obesitas"

        // Calculate risk score based on Kemenkes IMT/U classification & dietary/lifestyle habits
        var riskScore = 0
        val factors = mutableListOf<String>()

        when (imtuResult.category) {
            "Gizi Buruk" -> {
                riskScore += 4
                factors.add("Status IMT/U Kemenkes: Gizi Buruk (${imtuResult.zScoreFormatted} di bawah -3 SD)")
            }
            "Obesitas" -> {
                riskScore += 4
                factors.add("Status IMT/U Kemenkes: Obesitas (${imtuResult.zScoreFormatted} di atas +2 SD)")
            }
            "Gizi Kurang" -> {
                riskScore += 2
                factors.add("Status IMT/U Kemenkes: Gizi Kurang (${imtuResult.zScoreFormatted} rentang -3 s/d -2 SD)")
            }
            "Gizi Lebih" -> {
                riskScore += 2
                factors.add("Status IMT/U Kemenkes: Gizi Lebih (${imtuResult.zScoreFormatted} rentang +1 s/d +2 SD)")
            }
            else -> {
                // Normal
            }
        }

        if (breakfastHabit in listOf("Tidak pernah", "Jarang (1-2x)", "Jarang")) {
            riskScore += 2
            factors.add("Kebiasaan sering melewatkan sarapan pagi sebelum ke madrasah")
        }
        if (vegIntake in listOf("Jarang", "Tidak pernah", "Jarang/tidak pernah", "1 porsi/hari")) {
            riskScore += 1
            factors.add("Asupan serat dan mikronutrien sayuran harian masih kurang")
        }
        if (fruitIntake in listOf("Jarang", "Tidak pernah", "Jarang/tidak pernah", "1 porsi/hari")) {
            riskScore += 1
            factors.add("Konsumsi buah segar harian belum optimal")
        }
        if (junkFoodIntake in listOf(">=3x/minggu", "Sering")) {
            riskScore += 2
            factors.add("Frekuensi konsumsi gorengan/makanan cepat saji cukup tinggi")
        }
        if (sweetDrinkIntake in listOf(">=2x/hari", ">=3x/hari", "Sering")) {
            riskScore += 2
            factors.add("Tingginya asupan minuman manis kemasan/berpemanis tambahan")
        }
        if (physicalActivity in listOf("<30 menit", "Jarang", "Tidak")) {
            riskScore += 1
            factors.add("Aktivitas fisik teratur masih di bawah anjuran (minimal 30-60 menit)")
        }
        if (sedentaryTime in listOf(">6 jam", "3-6 jam")) {
            riskScore += 1
            factors.add("Waktu sedentari (duduk/layar gawai) cukup panjang")
        }

        val riskCategory = when {
            riskScore >= 5 || imtuResult.category == "Gizi Buruk" || imtuResult.category == "Obesitas" -> "Risiko Tinggi"
            riskScore >= 3 || imtuResult.category == "Gizi Kurang" || imtuResult.category == "Gizi Lebih" -> "Perlu Perhatian"
            else -> "Risiko Rendah"
        }

        val factorsString = if (factors.isEmpty()) {
            "Semua indikator kebiasaan dan antropometri sesuai standar Kemenkes RI dalam kondisi prima."
        } else {
            factors.joinToString(" • ")
        }

        val advice = when (riskCategory) {
            "Risiko Rendah" -> "Alhamdulillah, status gizi menurut IMT/U Kemenkes RI (${imtuResult.category}, Z-Score: ${imtuResult.zScoreFormatted}) dan kebiasaanmu tergolong baik. Pertahankan pola makan gizi seimbang 'Isi Piringku', rutinkan olahraga 3-5 kali seminggu, dan cukupi istirahat malam."
            "Perlu Perhatian" -> "Status gizi IMT/U berada pada kategori ${imtuResult.category} (${imtuResult.zScoreFormatted}). Terdapat kebiasaan yang perlu diperbaiki: biasakan sarapan bergizi, tingkatkan sayur/buah, dan batasi jajanan manis di kantin madrasah."
            else -> "Status gizi IMT/U berada pada kategori ${imtuResult.category} (${imtuResult.zScoreFormatted}). Kondisi ini memerlukan pendampingan dan konsultasi khusus dengan Petugas UKS Madrasah atau tenaga kesehatan gizi puskesmas."
        }

        return GiziEvaluationResult(
            bmi = formattedBmi,
            zScore = imtuResult.zScore,
            zScoreFormatted = imtuResult.zScoreFormatted,
            bmiCategory = bmiCategory,
            riskCategory = riskCategory,
            standardReference = imtuResult.standardReference,
            normalRangeText = imtuResult.normalRangeText,
            factorsToNote = factorsString,
            generalAdvice = advice
        )
    }

    /**
     * Seed initial educational articles and sample student data
     */
    suspend fun seedInitialDataIfEmpty() {
        val existingArticles = educationArticleDao.getAllArticles().firstOrNull()
        if (existingArticles.isNullOrEmpty()) {
            val articles = listOf(
                EducationArticleEntity(
                    id = "art_1",
                    title = "Mengenal Konsep Gizi Seimbang & 'Isi Piringku'",
                    category = "Gizi Seimbang",
                    summary = "Panduan porsi ideal makanan pokok, sayur, lauk protein, dan buah untuk remaja madrasah.",
                    content = "Prinsip Gizi Seimbang menggantikan konsep lama 4 Sehat 5 Sempurna. Kemenkes RI mencanangkan panduan 'Isi Piringku': 1/3 bagian makanan pokok sebagai sumber karbohidrat, 1/3 bagian sayuran kaya serat, 1/6 bagian lauk-pauk sumber protein nabati & hewani, serta 1/6 bagian buah-buahan segar. Variasi jenis makanan sangat penting karena tidak ada satu jenis makanan pun yang mengandung seluruh zat gizi lengkap.",
                    readTime = "3 mnt",
                    recommendedForRisk = "ALL"
                ),
                EducationArticleEntity(
                    id = "art_2",
                    title = "Mengapa Sarapan Itu Wajib Sebelum ke Madrasah?",
                    category = "Sarapan",
                    summary = "Hubungan sarapan pagi dengan konsentrasi belajar dan pencegahan rasa lemas di kelas.",
                    content = "Saat kita tidur malam selama 8 jam, kadar glukosa dalam darah menurun drastis. Sarapan pagi berfungsi sebagai 'bahan bakar' utama otak untuk berpikir dan mencerna pelajaran di kelas. Siswa madrasah yang rutin sarapan memiliki konsentrasi belajar lebih stabil, tidak mudah mengantuk saat jam pelajaran pertama, dan terhindar dari rasa lapar berlebih saat istirahat.",
                    readTime = "2 mnt",
                    recommendedForRisk = "Perlu Perhatian"
                ),
                EducationArticleEntity(
                    id = "art_3",
                    title = "Kekuatan Rahasia Buah dan Sayur Berwarna",
                    category = "Buah dan Sayur",
                    summary = "Manfaat vitamin, mineral, dan antioksidan untuk sistem imun siswa aktif.",
                    content = "Sayuran berdaun hijau (seperti bayam, sawi, kangkung) kaya akan zat besi yang penting untuk mencegah anemia gizi pada remaja. Sementara buah berwarna kuning/oranye (seperti jeruk, pepaya, pisang) kaya vitamin C dan kalium yang menjaga stamina tubuh saat jadwal madrasah padat sampai sore hari.",
                    readTime = "3 mnt",
                    recommendedForRisk = "Risiko Tinggi"
                ),
                EducationArticleEntity(
                    id = "art_4",
                    title = "Waspada Jebakan Gula, Garam, dan Lemak (GGL)",
                    category = "Makanan dan Minuman",
                    summary = "Cara cerdas memilih jajanan di kantin madrasah dan mengenali bahaya gula tersembunyi.",
                    content = "Batas anjuran konsumsi harian per orang dari Kemenkes RI adalah Gula maksimal 4 sendok makan (50g), Garam maksimal 1 sendok teh (5g / 2000mg natrium), dan Lemak/Minyak maksimal 5 sendok makan (67g). Satu botol minuman boba atau teh kemasan manis sering kali mengandung 30-45g gula! Pilihlah air putih mineral, jus tanpa pemanis buatan, atau susu putih murni.",
                    readTime = "4 mnt",
                    recommendedForRisk = "Risiko Tinggi"
                ),
                EducationArticleEntity(
                    id = "art_5",
                    title = "Aktivitas Fisik Ringan di Sela Jam Istirahat Madrasah",
                    category = "Aktivitas Fisik",
                    summary = "Gerakan peregangan dan jalan santai untuk mengatasi kantuk dan pegal setelah duduk lama.",
                    content = "Duduk berjam-jam mendengarkan guru dapat menurunkan sirkulasi darah dan membuat tubuh terasa lesu. Cukup dengan berdiri, meregangkan bahu dan leher, serta berjalan kaki berkeliling halaman madrasah selama 10-15 menit saat istirahat, kebugaran kardiovaskular dan mood belajar akan kembali segar.",
                    readTime = "2 mnt",
                    recommendedForRisk = "ALL"
                ),
                EducationArticleEntity(
                    id = "art_6",
                    title = "Tidur Cukup: Kunci Metabolisme Sehat & Daya Ingat Kuat",
                    category = "Tidur dan Kebiasaan Sehat",
                    summary = "Mengapa begadang merusak metabolisme tubuh dan memicu nafsu makan berlebih.",
                    content = "Remaja usia 15-18 tahun membutuhkan 7 hingga 9 jam tidur malam berkualitas. Kurang tidur mengacaukan hormon leptin (penahan nafsu makan) dan ghrelin (pemicu lapar), sehingga remaja yang sering begadang cenderung mengidam makanan tinggi kalori dan gorengan di malam hari.",
                    readTime = "3 mnt",
                    recommendedForRisk = "Perlu Perhatian"
                )
            )
            educationArticleDao.insertArticles(articles)
        }

        // Seed demo student and UKS data if empty
        val existingStudents = userDao.getUserById("SISWA-001")
        if (existingStudents == null) {
            val student1 = UserEntity(
                id = "SISWA-001",
                email = "syahrulalfiantroso@gmail.com",
                name = "Ahmad Syahrul",
                grade = "XI IPA 1",
                age = 16,
                gender = "Laki-laki",
                studentIdNumber = "0078129841",
                role = "SISWA",
                isProfileComplete = true
            )
            val student2 = UserEntity(
                id = "SISWA-002",
                email = "fatimah.zahra@madrasah.sch.id",
                name = "Fatimah Az-Zahra",
                grade = "X MA 2",
                age = 15,
                gender = "Perempuan",
                studentIdNumber = "0083214552",
                role = "SISWA",
                isProfileComplete = true
            )
            val student3 = UserEntity(
                id = "SISWA-003",
                email = "m.rizki@madrasah.sch.id",
                name = "Muhammad Rizki",
                grade = "XII IPS 1",
                age = 17,
                gender = "Laki-laki",
                studentIdNumber = "0069921477",
                role = "SISWA",
                isProfileComplete = true
            )
            userDao.insertUser(student1)
            userDao.insertUser(student2)
            userDao.insertUser(student3)

            // Seed demo UKS Officer
            val uksOfficer = UksOfficerEntity(
                id = "UKS-01",
                name = "Ibu Nurul Hidayah, S.Kep",
                email = "uks@madrasah.sch.id",
                madrasahName = "Madrasah Aliyah Negeri 1",
                officerCode = "UKS-MAN1-2026",
                passwordHash = "uks12345",
                isVerified = true
            )
            uksOfficerDao.insertOfficer(uksOfficer)

            // Seed sample Gizi checks
            val check1 = GiziCheckEntity(
                studentId = "SISWA-001",
                date = "15 Sep 2026",
                timestamp = System.currentTimeMillis() - 86400000L * 2,
                weightKg = 58.5f,
                heightCm = 168.0f,
                bmi = 20.7f,
                bmiCategory = "Gizi Baik (Normal)",
                riskCategory = "Risiko Rendah",
                breakfastHabit = "Setiap hari",
                vegetableIntake = ">= 2 porsi/hari",
                fruitIntake = "1 porsi/hari",
                junkFoodIntake = "Jarang",
                sweetDrinkIntake = "1 kali/hari",
                physicalActivity = ">= 60 menit",
                sedentaryTime = "<3 jam",
                factorsToNote = "Semua parameter antropometri dan gaya hidup terpantau stabil.",
                generalAdvice = "Pola gizi seimbang dan aktivitas fisik sangat baik. Teruskan konsistensi ini!"
            )
            val check2 = GiziCheckEntity(
                studentId = "SISWA-002",
                date = "12 Sep 2026",
                timestamp = System.currentTimeMillis() - 86400000L * 5,
                weightKg = 41.0f,
                heightCm = 158.0f,
                bmi = 16.4f,
                bmiCategory = "Gizi Kurang (Kurus)",
                riskCategory = "Perlu Perhatian",
                breakfastHabit = "Jarang",
                vegetableIntake = "Jarang",
                fruitIntake = "Jarang",
                junkFoodIntake = "1-2x/minggu",
                sweetDrinkIntake = "1 kali/hari",
                physicalActivity = "<30 menit",
                sedentaryTime = "3-6 jam",
                factorsToNote = "IMT di bawah normal (16.4 kg/m²) • Sering melewatkan sarapan • Kurang asupan sayur",
                generalAdvice = "Tingkatkan porsi makan gizi seimbang dengan makanan padat nutrisi dan jangan lewatkan sarapan sebelum ke madrasah."
            )
            val check3 = GiziCheckEntity(
                studentId = "SISWA-003",
                date = "10 Sep 2026",
                timestamp = System.currentTimeMillis() - 86400000L * 8,
                weightKg = 82.0f,
                heightCm = 169.0f,
                bmi = 28.7f,
                bmiCategory = "Gizi Lebih (Obesitas)",
                riskCategory = "Risiko Tinggi",
                breakfastHabit = "Setiap hari",
                vegetableIntake = "Jarang/tidak pernah",
                fruitIntake = "Jarang/tidak pernah",
                junkFoodIntake = ">=3x/minggu",
                sweetDrinkIntake = ">=2x/hari",
                physicalActivity = "<30 menit",
                sedentaryTime = ">6 jam",
                factorsToNote = "IMT tergolong obesitas (28.7 kg/m²) • Tingginya konsumsi minuman manis dan makanan cepat saji • Kurang aktivitas fisik",
                generalAdvice = "Perlu perhatian dan pendampingan terstruktur dari UKS. Batasi asupan minuman manis dan perbanyak bergerak aktif."
            )
            giziCheckDao.insertCheck(check1)
            giziCheckDao.insertCheck(check2)
            giziCheckDao.insertCheck(check3)

            // Seed sample daily check for student 1
            val todayDate = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())
            val daily1 = DailyCheckEntity(
                studentId = "SISWA-001",
                date = todayDate,
                timestamp = System.currentTimeMillis(),
                breakfast = "Ya",
                mealsCount = "3 kali",
                eatVeg = "Ya",
                eatFruit = "Ya",
                sweetDrinks = "1 kali",
                activityDone = "Ya",
                activityDuration = "30–60 menit",
                sedentaryHours = "3-6 jam",
                sleepHours = "6–8 jam",
                bodyCondition = "Sangat Bugar",
                summaryText = "Sarapan: Ya • Makan: 3 kali • Sayur: Ya • Buah: Ya • Minuman manis: 1 kali • Aktivitas: 30–60 menit",
                aiAdvice = "Pola hari ini sudah sangat seimbang dan terjaga. Pertahankan asupan buah dan konsumsi air putih cukup!"
            )
            dailyCheckDao.insertDailyCheck(daily1)

            // Seed sample food log
            val foodLog1 = FoodLogEntity(
                studentId = "SISWA-001",
                date = todayDate,
                timestamp = System.currentTimeMillis() - 10000000,
                foodName = "Nasi Putih, Ayam Goreng & Sayur Bening",
                detectedItems = "Nasi putih, ayam ungkep goreng, sayur bening bayam jagung, sambal",
                foodGroups = "Isi Piringku: Karbohidrat, Protein Hewani, Sayuran Berkuah",
                carbSource = "Nasi putih 1 porsi sedang",
                proteinSource = "Ayam ungkep goreng",
                vegFruitSource = "Sayur bening bayam dan jagung manis",
                estimatedCalories = "± 520 kkal",
                balanceEvaluation = "Menu ini sudah memiliki sumber karbohidrat dan protein yang baik. Akan lebih seimbang jika ditambahkan buah potong seperti pisang atau pepaya.",
                educationalNote = "Analisis foto merupakan perkiraan dan dapat berbeda dari kondisi sebenarnya."
            )
            foodLogDao.insertFoodLog(foodLog1)

            // Seed sample follow up from UKS
            val followUp = UksFollowUpEntity(
                studentId = "SISWA-002",
                officerName = "Ibu Nurul Hidayah, S.Kep",
                date = "13 Sep 2026",
                actionNote = "Telah diberikan edukasi pentingnya sarapan bekal sehat dari rumah dan tablet tambah darah (TTD) untuk pencegahan anemia gizi."
            )
            uksFollowUpDao.insertFollowUp(followUp)
        }
    }
}

data class GiziEvaluationResult(
    val bmi: Float,
    val zScore: Float,
    val zScoreFormatted: String,
    val bmiCategory: String,
    val riskCategory: String,
    val standardReference: String,
    val normalRangeText: String,
    val factorsToNote: String,
    val generalAdvice: String
)

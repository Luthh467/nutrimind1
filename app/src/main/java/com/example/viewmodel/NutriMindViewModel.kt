package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.FoodAnalysisResult
import com.example.data.ai.GeminiClient
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.GiziEvaluationResult
import com.example.data.repository.NutriMindRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    AUTH,
    COMPLETE_PROFILE,
    BERANDA,
    CEK_GIZI,
    FOTO_MAKANAN,
    CEK_HARIAN,
    EDUKASI,
    RIWAYAT,
    PROFIL,
    UKS_DASHBOARD
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "nutrimind"
    val text: String,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

class NutriMindViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = NutriMindRepository(database)

    // Navigation & Auth
    private val _currentScreen = MutableStateFlow(AppScreen.AUTH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentStudent = MutableStateFlow<UserEntity?>(null)
    val currentStudent: StateFlow<UserEntity?> = _currentStudent.asStateFlow()

    private val _currentOfficer = MutableStateFlow<UksOfficerEntity?>(null)
    val currentOfficer: StateFlow<UksOfficerEntity?> = _currentOfficer.asStateFlow()

    private val _authRole = MutableStateFlow<String?>(null) // "SISWA" or "UKS"
    val authRole: StateFlow<String?> = _authRole.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Data lists
    private val _studentGiziChecks = MutableStateFlow<List<GiziCheckEntity>>(emptyList())
    val studentGiziChecks: StateFlow<List<GiziCheckEntity>> = _studentGiziChecks.asStateFlow()

    private val _studentDailyChecks = MutableStateFlow<List<DailyCheckEntity>>(emptyList())
    val studentDailyChecks: StateFlow<List<DailyCheckEntity>> = _studentDailyChecks.asStateFlow()

    private val _studentFoodLogs = MutableStateFlow<List<FoodLogEntity>>(emptyList())
    val studentFoodLogs: StateFlow<List<FoodLogEntity>> = _studentFoodLogs.asStateFlow()

    private val _latestGiziCheck = MutableStateFlow<GiziCheckEntity?>(null)
    val latestGiziCheck: StateFlow<GiziCheckEntity?> = _latestGiziCheck.asStateFlow()

    private val _todayDailyCheck = MutableStateFlow<DailyCheckEntity?>(null)
    val todayDailyCheck: StateFlow<DailyCheckEntity?> = _todayDailyCheck.asStateFlow()

    // Education Articles
    val allArticles: StateFlow<List<EducationArticleEntity>> = repository.getAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UKS Overview Data
    val allStudents: StateFlow<List<UserEntity>> = repository.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGiziChecks: StateFlow<List<GiziCheckEntity>> = repository.getAllChecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOfficers: StateFlow<List<UksOfficerEntity>> = repository.getAllOfficers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Food Analysis state
    private val _isAnalyzingFood = MutableStateFlow(false)
    val isAnalyzingFood: StateFlow<Boolean> = _isAnalyzingFood.asStateFlow()

    private val _currentFoodAnalysis = MutableStateFlow<FoodAnalysisResult?>(null)
    val currentFoodAnalysis: StateFlow<FoodAnalysisResult?> = _currentFoodAnalysis.asStateFlow()

    // Daily Check submission state
    private val _isSubmittingDaily = MutableStateFlow(false)
    val isSubmittingDaily: StateFlow<Boolean> = _isSubmittingDaily.asStateFlow()

    // Chatbot state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            sender = "nutrimind",
            text = "Assalamu'alaikum! Saya NutriMind AI, teman belajarmu seputar gizi dan pola hidup sehat di madrasah. Ada yang ingin kamu tanyakan mengenai sarapan, bekal sekolah, atau kebutuhan gizimu hari ini?"
        )
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun navigateTo(screen: AppScreen) {
        // Enforce role-based page protection:
        // Siswa cannot open UKS dashboard
        if (screen == AppScreen.UKS_DASHBOARD && _authRole.value != "UKS") {
            _toastMessage.value = "Akses ditolak: Dashboard UKS hanya untuk petugas UKS resmi."
            return
        }
        _currentScreen.value = screen
    }

    // =========================== AUTHENTICATION ===========================

    fun loginAsGoogleStudent(email: String, name: String, isNewAccount: Boolean = false) {
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                _currentStudent.value = existing
                _authRole.value = "SISWA"
                observeStudentData(existing.id)
                if (!existing.isProfileComplete || isNewAccount) {
                    _currentScreen.value = AppScreen.COMPLETE_PROFILE
                } else {
                    _currentScreen.value = AppScreen.BERANDA
                    _toastMessage.value = "Login berhasil. Selamat datang, ${existing.name}!"
                }
            } else {
                // Create new student
                val newId = "SISWA-" + String.format(Locale.US, "%03d", (100..999).random())
                val newStudent = UserEntity(
                    id = newId,
                    email = email,
                    name = name,
                    grade = "",
                    age = 16,
                    gender = "Laki-laki",
                    studentIdNumber = "",
                    role = "SISWA",
                    isProfileComplete = false
                )
                repository.saveUser(newStudent)
                _currentStudent.value = newStudent
                _authRole.value = "SISWA"
                observeStudentData(newId)
                _currentScreen.value = AppScreen.COMPLETE_PROFILE
                _toastMessage.value = "Akun Google terhubung. Silakan lengkapi profilmu."
            }
        }
    }

    fun saveStudentProfile(
        name: String,
        grade: String,
        age: Int,
        gender: String,
        nisn: String
    ) {
        viewModelScope.launch {
            val current = _currentStudent.value ?: return@launch
            val updated = current.copy(
                name = name.ifBlank { current.name },
                grade = grade,
                age = age,
                gender = gender,
                studentIdNumber = nisn,
                isProfileComplete = true
            )
            repository.updateUser(updated)
            _currentStudent.value = updated
            _currentScreen.value = AppScreen.BERANDA
            _toastMessage.value = "Profil berhasil disimpan."
        }
    }

    fun loginAsUksOfficer(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val officer = repository.getOfficerByEmail(email.trim())
            if (officer == null) {
                onResult(false, "Akun petugas UKS tidak ditemukan.")
                return@launch
            }
            if (officer.passwordHash != password.trim()) {
                onResult(false, "Kata sandi salah.")
                return@launch
            }
            if (!officer.isVerified) {
                onResult(false, "Akun UKS sedang menunggu verifikasi administrator madrasah.")
                return@launch
            }

            _currentOfficer.value = officer
            _authRole.value = "UKS"
            _currentScreen.value = AppScreen.UKS_DASHBOARD
            _toastMessage.value = "Login berhasil. Selamat datang, ${officer.name}!"
            onResult(true, "Berhasil masuk")
        }
    }

    fun registerUksOfficer(
        name: String,
        email: String,
        madrasahName: String,
        officerCode: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.getOfficerByEmail(email.trim())
            if (existing != null) {
                onResult(false, "Email petugas UKS sudah terdaftar.")
                return@launch
            }
            val newOfficer = UksOfficerEntity(
                id = "UKS-" + UUID.randomUUID().toString().take(6).uppercase(),
                name = name,
                email = email.trim(),
                madrasahName = madrasahName,
                officerCode = officerCode,
                passwordHash = password.trim(),
                isVerified = true, // Auto-verified for seamless testing prototype
                registeredAt = System.currentTimeMillis()
            )
            repository.registerOfficer(newOfficer)
            _currentOfficer.value = newOfficer
            _authRole.value = "UKS"
            _currentScreen.value = AppScreen.UKS_DASHBOARD
            _toastMessage.value = "Pendaftaran berhasil. Akun Petugas UKS aktif."
            onResult(true, "Akun berhasil dibuat")
        }
    }

    fun logout() {
        _currentStudent.value = null
        _currentOfficer.value = null
        _authRole.value = null
        _currentScreen.value = AppScreen.AUTH
        _toastMessage.value = "Sesi login telah berakhir. Silakan masuk kembali."
    }

    private fun observeStudentData(studentId: String) {
        viewModelScope.launch {
            repository.getChecksForStudent(studentId).collect {
                _studentGiziChecks.value = it
            }
        }
        viewModelScope.launch {
            repository.getLatestCheckForStudent(studentId).collect {
                _latestGiziCheck.value = it
            }
        }
        viewModelScope.launch {
            repository.getDailyChecksForStudent(studentId).collect {
                _studentDailyChecks.value = it
            }
        }
        viewModelScope.launch {
            repository.getFoodLogsForStudent(studentId).collect {
                _studentFoodLogs.value = it
            }
        }
        // Check today's check
        val todayStr = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())
        viewModelScope.launch {
            val check = repository.getDailyCheckByDate(studentId, todayStr)
            _todayDailyCheck.value = check
        }
    }

    // =========================== GIZI CHECK ===========================

    fun calculateAndSaveGiziCheck(
        weightKg: Float,
        heightCm: Float,
        breakfast: String,
        vegetable: String,
        fruit: String,
        junkFood: String,
        sweetDrink: String,
        physicalAct: String,
        sedentary: String,
        onComplete: (GiziCheckEntity) -> Unit
    ) {
        val student = _currentStudent.value ?: return
        val eval = repository.evaluateGizi(
            weightKg = weightKg,
            heightCm = heightCm,
            age = student.age,
            gender = student.gender,
            breakfastHabit = breakfast,
            vegIntake = vegetable,
            fruitIntake = fruit,
            junkFoodIntake = junkFood,
            sweetDrinkIntake = sweetDrink,
            physicalActivity = physicalAct,
            sedentaryTime = sedentary
        )

        val todayDate = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())
        val check = GiziCheckEntity(
            studentId = student.id,
            date = todayDate,
            timestamp = System.currentTimeMillis(),
            weightKg = weightKg,
            heightCm = heightCm,
            bmi = eval.bmi,
            bmiCategory = eval.bmiCategory,
            riskCategory = eval.riskCategory,
            breakfastHabit = breakfast,
            vegetableIntake = vegetable,
            fruitIntake = fruit,
            junkFoodIntake = junkFood,
            sweetDrinkIntake = sweetDrink,
            physicalActivity = physicalAct,
            sedentaryTime = sedentary,
            factorsToNote = eval.factorsToNote,
            generalAdvice = eval.generalAdvice,
            zScore = eval.zScore,
            standardReference = eval.standardReference
        )

        viewModelScope.launch {
            repository.saveGiziCheck(check)
            _latestGiziCheck.value = check
            _toastMessage.value = "Data cek gizi berhasil disimpan."
            onComplete(check)
        }
    }

    // =========================== FOOD PHOTO AI ===========================

    fun analyzeFoodImage(bitmap: Bitmap?, samplePreset: String? = null) {
        viewModelScope.launch {
            _isAnalyzingFood.value = true
            if (samplePreset != null && bitmap == null) {
                // Instant realistic preset for madrasah meal
                val result = GeminiClient.getFallbackFoodAnalysis(samplePreset)
                _currentFoodAnalysis.value = result
            } else {
                val result = GeminiClient.analyzeFoodImage(bitmap)
                _currentFoodAnalysis.value = result
            }
            _isAnalyzingFood.value = false
        }
    }

    fun saveFoodAnalysisToHistory() {
        val student = _currentStudent.value ?: return
        val analysis = _currentFoodAnalysis.value ?: return
        val todayDate = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

        val foodLog = FoodLogEntity(
            studentId = student.id,
            date = todayDate,
            foodName = analysis.foodName,
            detectedItems = analysis.detectedItems,
            foodGroups = analysis.foodGroups,
            carbSource = analysis.carbSource,
            proteinSource = analysis.proteinSource,
            vegFruitSource = analysis.vegFruitSource,
            estimatedCalories = analysis.estimatedCalories,
            balanceEvaluation = analysis.balanceEvaluation,
            educationalNote = analysis.educationalNote
        )

        viewModelScope.launch {
            repository.saveFoodLog(foodLog)
            _toastMessage.value = "Foto makanan dan analisis AI berhasil disimpan ke riwayat."
        }
    }

    // =========================== DAILY CHECK ===========================

    fun submitDailyCheck(
        sarapan: String,
        makanBerapaKali: String,
        sayur: String,
        buah: String,
        minumanManis: String,
        aktivitasFisik: String,
        durasiAktivitas: String,
        durasiDuduk: String,
        tidur: String,
        kondisiTubuh: String,
        bmr: Float = 0f,
        tdee: Float = 0f,
        activityLevel: String = "",
        onComplete: (DailyCheckEntity) -> Unit
    ) {
        val student = _currentStudent.value ?: return
        viewModelScope.launch {
            _isSubmittingDaily.value = true
            val aiAdvice = GeminiClient.generateDailyAdvice(
                sarapan = sarapan,
                sayur = sayur,
                buah = buah,
                minumanManis = minumanManis,
                aktivitasFisik = aktivitasFisik,
                durasiAktivitas = durasiAktivitas,
                durasiDuduk = durasiDuduk,
                tidur = tidur,
                kondisiTubuh = kondisiTubuh,
                bmr = bmr,
                tdee = tdee,
                activityLevel = activityLevel
            )

            val bmrPrefix = if (bmr > 0f) "BMR: ${bmr.toInt()} kkal • TDEE: ${tdee.toInt()} kkal • " else ""
            val summary = "$bmrPrefix Sarapan: $sarapan • Sayur: $sayur • Buah: $buah • Minuman manis: $minumanManis • Aktivitas: $durasiAktivitas • Tidur: $tidur"
            val todayDate = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())

            val daily = DailyCheckEntity(
                studentId = student.id,
                date = todayDate,
                timestamp = System.currentTimeMillis(),
                breakfast = sarapan,
                mealsCount = makanBerapaKali,
                eatVeg = sayur,
                eatFruit = buah,
                sweetDrinks = minumanManis,
                activityDone = aktivitasFisik,
                activityDuration = durasiAktivitas,
                sedentaryHours = durasiDuduk,
                sleepHours = tidur,
                bodyCondition = kondisiTubuh,
                summaryText = summary,
                aiAdvice = aiAdvice,
                bmr = bmr,
                tdee = tdee,
                activityLevel = activityLevel
            )

            repository.saveDailyCheck(daily)
            _todayDailyCheck.value = daily
            _isSubmittingDaily.value = false
            _toastMessage.value = "Cek kebutuhan energi & kesehatan hari ini berhasil disimpan."
            onComplete(daily)
        }
    }

    // =========================== AI CHATBOT ===========================

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = message.trim())
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatLoading.value = true
            val history = _chatMessages.value.map {
                (if (it.sender == "user") "user" else "model") to it.text
            }
            val reply = GeminiClient.chatWithNutriMind(history, message.trim())
            val aiMsg = ChatMessage(sender = "nutrimind", text = reply)
            _chatMessages.value = _chatMessages.value + aiMsg
            _isChatLoading.value = false
        }
    }

    // =========================== UKS ACTIONS ===========================

    fun addUksFollowUp(studentId: String, note: String, onDone: () -> Unit) {
        val officer = _currentOfficer.value ?: return
        val todayDate = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())
        viewModelScope.launch {
            val followUp = UksFollowUpEntity(
                studentId = studentId,
                officerName = officer.name,
                date = todayDate,
                actionNote = note
            )
            repository.saveFollowUp(followUp)
            _toastMessage.value = "Catatan tindak lanjut UKS berhasil disimpan."
            onDone()
        }
    }

    fun toggleArticleFavorite(id: String, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleArticleFavorite(id, isFav)
        }
    }
}

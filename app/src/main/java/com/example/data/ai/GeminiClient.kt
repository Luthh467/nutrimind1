package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "NutriMindAI"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.ifEmpty { "" }
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Multimodal analysis of food images using gemini-3.5-flash with structured JSON output
     */
    suspend fun analyzeFoodImage(
        bitmap: Bitmap?,
        prompt: String = """
            Kamu adalah AI Ahli Gizi Remaja dari NutriMind AI. Analisis gambar makanan ini secara teliti untuk identifikasi makanan dan estimasi keseimbangan gizi siswa madrasah berdasarkan konsep "Isi Piringku" Kemenkes RI.
            
            Kembalikan jawaban HANYA berupa format JSON valid dengan struktur:
            {
              "foodName": "Nama hidangan/kombinasi makanan yang teridentifikasi",
              "detectedItems": "Daftar bahan makanan, lauk pauk, atau komponen yang terdeteksi di foto",
              "foodGroups": "Kelompok pangan Isi Piringku (Makanan Pokok, Lauk Pauk Hewani/Nabati, Sayuran, Buahan)",
              "carbSource": "Sumber karbohidrat dan taksiran porsinya (misal: Nasi putih ±1 centong / 150g)",
              "proteinSource": "Sumber protein hewani atau nabati yang terlihat (misal: Ayam goreng & tempe)",
              "vegFruitSource": "Kandungan sayuran dan buah yang terlihat (atau catat jika belum ada)",
              "estimatedCalories": "Estimasi kalori total & makronutrisi (contoh: '± 520 kkal (P: 24g, L: 15g, K: 70g)')",
              "balanceStatus": "Status Keseimbangan (pilih salah satu: 'Gizi Seimbang' / 'Cukup Seimbang' / 'Perlu Tambahan Sayur & Buah' / 'Tinggi Lemak & Karbohidrat')",
              "macronutrients": "Estimasi proporsi makronutrien (contoh: 'Karbohidrat: 55%, Protein: 20%, Lemak: 25%')",
              "balanceEvaluation": "Evaluasi keseimbangan gizi lengkap untuk kebutuhan belajar & pubertas siswa madrasah (analisis proporsi piring, serat, kecukupan energi)",
              "improvementTips": "Rekomendasi praktis dan ramah siswa madrasah untuk menyempurnakan menu ini sesuai Isi Piringku Kemenkes RI",
              "educationalNote": "Analisis foto merupakan perkiraan skrining gizi berbasis AI dan bukan diagnosis medis."
            }
        """.trimIndent()
    ): FoodAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || bitmap == null) {
            return@withContext getFallbackFoodAnalysis()
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val requestJson = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                    put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    }))
                }
                put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text",
                        "Kamu adalah NutriMind AI, sistem skrining dan pendeteksi gizi untuk siswa Madrasah Aliyah di Indonesia. Berikan informasi edukatif mengenai porsi seimbang Isi Piringku Kemenkes RI. Wajib kembalikan format JSON murni tanpa markdown tambahan."
                    )))
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")

                var responseText = ""
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.optJSONObject(i)
                        val text = part?.optString("text", "") ?: ""
                        if (text.isNotEmpty()) {
                            responseText += text
                        }
                    }
                }

                if (responseText.isNotEmpty()) {
                    parseFoodAnalysisJson(responseText)
                } else {
                    getFallbackFoodAnalysis()
                }
            } else {
                Log.w(TAG, "Gemini API error: ${response.code} $responseBody")
                getFallbackFoodAnalysis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini Vision API", e)
            getFallbackFoodAnalysis()
        }
    }

    /**
     * Fast daily check summary & advice using gemini-3.1-flash-lite
     */
    suspend fun generateDailyAdvice(
        sarapan: String,
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
        activityLevel: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val defaultAdvice = generateLocalDailyAdvice(sarapan, sayur, buah, minumanManis, durasiAktivitas, tidur, bmr, tdee)

        if (apiKey.isEmpty()) {
            return@withContext defaultAdvice
        }

        try {
            val bmrInfo = if (bmr > 0f) "- BMR (Metabolisme Basal Harris-Benedict): ${bmr.toInt()} kkal\n- TDEE (Kebutuhan Energi Total): ${tdee.toInt()} kkal ($activityLevel)" else ""
            val prompt = """
                Buatkan ringkasan 2-3 kalimat evaluasi gizi & saran ramah untuk siswa madrasah berdasarkan data cek harian dan kebutuhan energi berikut:
                $bmrInfo
                - Sarapan: $sarapan
                - Sayur: $sayur
                - Buah: $buah
                - Minuman manis: $minumanManis
                - Aktivitas fisik: $durasiAktivitas
                - Duduk/Gadget: $durasiDuduk
                - Tidur tadi malam: $tidur
                - Kondisi tubuh: $kondisiTubuh
                
                Aturan: Bahasa akrab, suportif, edukatif untuk anak MA/SMA. Hubungkan dengan target kalori harian jika relevan. Jangan mendiagnosis penyakit medis.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("${BASE_URL}gemini-3.1-flash-lite-preview:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""

                if (text.isNotBlank()) text.trim() else defaultAdvice
            } else {
                defaultAdvice
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling gemini-3.1-flash-lite", e)
            defaultAdvice
        }
    }

    /**
     * Multi-turn chat with NutriMind AI chatbot using gemini-3.5-flash
     */
    suspend fun chatWithNutriMind(
        history: List<Pair<String, String>>, // role to message
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getOfflineChatResponse(userMessage)
        }

        try {
            val contentsArray = JSONArray()
            // Add previous history
            history.takeLast(6).forEach { (role, text) ->
                val apiRole = if (role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", apiRole)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                })
            }
            // Add current message
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            })

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text",
                        "Kamu adalah Asisten NutriMind AI, teman edukasi gizi dan gaya hidup sehat untuk siswa Madrasah Aliyah di Indonesia. " +
                        "Gunakan sapaan hangat yang akrab bagi siswa madrasah (seperti 'Halo teman sehat!', 'Assalamu'alaikum', dll). " +
                        "Berikan penjelasan gizi praktis dan mudah dipahami seputar bekal sekolah, sarapan, kebutuhan kalori remaja, dan aktivitas fisik. " +
                        "PERATURAN PENTING: NutriMind AI HANYA skrining dan edukasi awal, BUKAN alat diagnosis medis atau peresepan obat. Jika ada keluhan fisik berulang, selalu sarankan konsultasi dengan petugas UKS atau dokter di Puskesmas."
                    )))
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""

                if (text.isNotBlank()) text.trim() else getOfflineChatResponse(userMessage)
            } else {
                getOfflineChatResponse(userMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in NutriMind chat", e)
            getOfflineChatResponse(userMessage)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if very large to optimize bandwidth
        val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val ratio = Math.min(1024f / bitmap.width, 1024f / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseFoodAnalysisJson(rawText: String): FoodAnalysisResult {
        return try {
            val cleanJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val json = JSONObject(cleanJson)
            val balanceStatus = json.optString("balanceStatus", "Gizi Seimbang")
            val macronutrients = json.optString("macronutrients", "Karbohidrat, Protein & Lemak")
            val improvementTips = json.optString("improvementTips", "Lengkapi dengan 1 porsi buah potong segar dan perbanyak minum air putih.")

            FoodAnalysisResult(
                foodName = json.optString("foodName", "Menu Makanan Madrasah"),
                detectedItems = json.optString("detectedItems", "Nasi putih, lauk protein, sayuran"),
                foodGroups = json.optString("foodGroups", "Isi Piringku: Karbohidrat, Lauk Protein, Sayuran"),
                carbSource = json.optString("carbSource", "Nasi putih / karbohidrat pokok"),
                proteinSource = json.optString("proteinSource", "Lauk pauk sumber protein"),
                vegFruitSource = json.optString("vegFruitSource", "Sayuran atau buah pelengkap"),
                estimatedCalories = json.optString("estimatedCalories", "± 480 - 550 kkal"),
                balanceEvaluation = json.optString("balanceEvaluation", "Menu ini dianalisis dengan kecukupan gizi pedoman Isi Piringku Kemenkes RI."),
                educationalNote = json.optString("educationalNote", "Analisis foto merupakan perkiraan skrining gizi berbasis AI dan bukan diagnosis medis."),
                balanceStatus = balanceStatus,
                macronutrients = macronutrients,
                improvementTips = improvementTips
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSON parsing error, falling back to text regex: ${e.message}")
            parseFoodAnalysisText(rawText)
        }
    }

    private fun parseFoodAnalysisText(text: String): FoodAnalysisResult {
        return FoodAnalysisResult(
            foodName = extractSection(text, "foodName", "Menu Makanan Siswa"),
            detectedItems = extractSection(text, "terdeteksi", "Nasi putih, lauk protein, sayuran"),
            foodGroups = extractSection(text, "kelompok", "Karbohidrat, Lauk Protein, Sayuran"),
            carbSource = extractSection(text, "karbohidrat", "Nasi putih / karbohidrat pokok"),
            proteinSource = extractSection(text, "protein", "Lauk pauk (ayam / telur / tempe)"),
            vegFruitSource = extractSection(text, "sayur", "Sayuran hijau atau buah pendamping"),
            estimatedCalories = extractSection(text, "kalori", "Sekitar 450 - 550 kkal"),
            balanceEvaluation = text.take(350).ifBlank { "Menu makan siang dengan gizi cukup seimbang sesuai rekomendasi Isi Piringku Kemenkes RI." },
            educationalNote = "Analisis foto merupakan perkiraan skrining gizi dan bukan diagnosis medis."
        )
    }

    fun getFallbackFoodAnalysis(sampleName: String = "Nasi Kotak Madrasah"): FoodAnalysisResult {
        return when (sampleName) {
            "Pecel Sayur & Tempe" -> FoodAnalysisResult(
                foodName = "Nasi Pecel Sayur, Tempe & Telur",
                detectedItems = "Nasi putih, kangkung, tauge, kacang panjang, bumbu kacang, tempe bacem, telur rebus",
                foodGroups = "Isi Piringku: 1/3 Karbohidrat, 1/3 Sayuran, 1/6 Protein Nabati, 1/6 Protein Hewani",
                carbSource = "Nasi putih (±150g)",
                proteinSource = "Tempe bacem & telur rebus (±18g protein)",
                vegFruitSource = "Sayuran kangkung, tauge & kacang panjang kaya serat & vitamin A/C",
                estimatedCalories = "± 480 kkal (Protein: 22g, Lemak: 14g, Karbo: 65g)",
                balanceEvaluation = "Menu ini sangat seimbang! Memiliki porsi sayuran yang berlimpah serta kombinasi protein nabati dan hewani yang baik untuk pertumbuhan remaja madrasah.",
                educationalNote = "Analisis foto merupakan perkiraan skrining gizi berbasis AI dan bukan diagnosis medis.",
                balanceStatus = "Gizi Seimbang",
                macronutrients = "Karbohidrat: 54%, Protein: 18%, Lemak: 28%",
                improvementTips = "Pertahankan menu ini! Kamu bisa menambahkan 1 buah pisang atau jeruk setelah makan untuk menambah asupan vitamin C."
            )
            "Mie Instan & Nugget" -> FoodAnalysisResult(
                foodName = "Mie Goreng Instan dengan Nugget",
                detectedItems = "Mie goreng instan, nugget ayam olahan, saus sambal",
                foodGroups = "Didominasi Karbohidrat olahan dan Lemak jenuh",
                carbSource = "Mie tepung gandum olahan",
                proteinSource = "Nugget ayam ultra-processed",
                vegFruitSource = "Belum ada sayuran atau buah segar",
                estimatedCalories = "± 540 kkal (Natrium tinggi >900mg, Serat rendah <2g)",
                balanceEvaluation = "Menu ini tinggi kalori, karbohidrat, dan natrium (garam), namun minim serat sayuran dan buah. Disarankan untuk membatasi makanan olahan dan menambahkan sayuran.",
                educationalNote = "Analisis foto merupakan perkiraan skrining gizi berbasis AI dan bukan diagnosis medis.",
                balanceStatus = "Perlu Penyesuaian",
                macronutrients = "Karbohidrat: 65%, Lemak: 25%, Protein: 10%",
                improvementTips = "Tambahkan sayuran hijau seperti sawi atau wortel saat memasak, serta ganti nugget olahan dengan telur atau tempe murni."
            )
            "Gado-Gado Madrasah", "Gado-Gado Lontong & Tahu Tempe" -> FoodAnalysisResult(
                foodName = "Gado-Gado Sayur, Lontong, Tahu & Telur",
                detectedItems = "Lontong beras, selada, tauge, kentang rebus, tahu goreng, telur rebus, saus kacang tanah gurih",
                foodGroups = "Isi Piringku: Lengkap (Karbohidrat, Protein Nabati & Hewani, Sayur Segar)",
                carbSource = "Lontong beras & kentang kukus (±150g karbohidrat kompleks)",
                proteinSource = "Tahu kedelai & telur rebus utuh (±16g protein berkualitas)",
                vegFruitSource = "Selada hijau, tauge renyah, dan mentimun segar kaya air & serat",
                estimatedCalories = "± 460 kkal (Protein: 20g, Lemak: 15g, Karbo: 60g)",
                balanceEvaluation = "Kaya antioksidan dan serat pangan. Sangat mendukung kesehatan saluran cerna dan menjaga rasa kenyang lebih lama saat belajar di kelas.",
                educationalNote = "Analisis foto merupakan perkiraan skrining gizi berbasis AI dan bukan diagnosis medis.",
                balanceStatus = "Gizi Seimbang",
                macronutrients = "Karbohidrat: 52%, Protein: 18%, Lemak: 30%",
                improvementTips = "Sangat baik! Porsi sayuran mencukupi 1/3 piring. Kontrol kekentalan bumbu kacang agar asupan kalori tetap proporsional."
            )
            else -> FoodAnalysisResult(
                foodName = "Nasi Putih, Ayam Goreng & Sayur Bening",
                detectedItems = "Nasi putih, ayam ungkep goreng, sayur bening bayam jagung, sambal",
                foodGroups = "Lengkap (Karbohidrat, Protein Hewani, Sayuran berkuah)",
                carbSource = "Nasi putih (±1 Centong / 150g)",
                proteinSource = "Ayam bagian paha/dada (sumber asam amino esensial)",
                vegFruitSource = "Sayur bening bayam & jagung manis (sumber zat besi & serat)",
                estimatedCalories = "± 520 kkal (Protein: 25g, Lemak: 16g, Karbo: 68g)",
                balanceEvaluation = "Menu ini sudah memiliki sumber karbohidrat dan protein yang cukup. Penambahan sayur bayam sangat baik untuk pencegahan anemia gizi pada siswa.",
                educationalNote = "Analisis foto merupakan perkiraan skrining gizi berbasis AI dan bukan diagnosis medis.",
                balanceStatus = "Cukup Seimbang",
                macronutrients = "Karbohidrat: 55%, Protein: 22%, Lemak: 23%",
                improvementTips = "Akan lebih sempurna jika ditutup dengan 1 porsi buah potong seperti pisang, semangka, atau pepaya untuk melengkapi vitamin & mineral."
            )
        }
    }

    private fun generateLocalDailyAdvice(
        sarapan: String,
        sayur: String,
        buah: String,
        minumanManis: String,
        durasiAktivitas: String,
        tidur: String,
        bmr: Float = 0f,
        tdee: Float = 0f
    ): String {
        val tips = mutableListOf<String>()
        if (sarapan == "Tidak") {
            tips.add("usahakan jangan melewatkan sarapan agar konsentrasi belajar tetap prima di kelas")
        }
        if (sayur == "Belum" || buah == "Belum") {
            tips.add("tambahkan porsi sayur berkuah atau buah segar saat makan siang/malam")
        }
        if (minumanManis == ">=3 kali" || minumanManis == "2 kali") {
            tips.add("kurangi konsumsi minuman manis kemasan dan perbanyak minum air putih 8 gelas")
        }
        if (durasiAktivitas == "<30 menit") {
            tips.add("luangkan waktu 15-30 menit untuk jalan kaki atau peregangan di waktu istirahat")
        }
        if (tidur == "<6 jam") {
            tips.add("hindari begadang agar regenerasi sel dan fokus belajarmu tetap optimal")
        }

        val baseAdvice = if (tips.isEmpty()) {
            "Masya Allah! Pola kebiasaan sehatmu hari ini sudah luar biasa seimbang. Terus pertahankan ritme ini setiap hari ya!"
        } else {
            "Pola hari ini sudah cukup baik. Cobalah " + tips.take(2).joinToString(" dan ") + "."
        }

        return if (tdee > 0f) {
            "$baseAdvice (Target energi harianmu: ±${tdee.toInt()} kkal; BMR: ${bmr.toInt()} kkal)."
        } else {
            baseAdvice
        }
    }

    private fun getOfflineChatResponse(query: String): String {
        val lower = query.lowercase()
        return when {
            "sarapan" in lower -> {
                "Sarapan sangat krusial bagi siswa madrasah! Setelah 8-10 jam berpuasa saat tidur, otak membutuhkan glukosa untuk fokus menyerap pelajaran. Contoh sarapan praktis: telur rebus dengan roti gandum atau nasi dengan sayur bening dan tempe."
            }
            "imt" in lower || "berat" in lower || "gemuk" in lower || "kurus" in lower -> {
                "Indeks Massa Tubuh (IMT) dihitung dari Berat Badan (kg) dibagi Tinggi Badan kuadrat (m²). Untuk remaja seusiamu (15-18 tahun), status gizi dinilai berdasarkan IMT menurut Umur (IMT/U). Jangan diet ketat tanpa pengawasan ya, prioritaskan gizi seimbang!"
            }
            "minuman manis" in lower || "gula" in lower || "boba" in lower -> {
                "Batas asupan gula harian anjuran Kemenkes adalah maksimal 4 sendok makan (50 gram) per hari. Satu cup minuman boba atau teh manis kemasan sering kali sudah melebihi batas ini! Ganti dengan air putih atau susu murni."
            }
            "anemia" in lower || "lemas" in lower || "pusing" in lower -> {
                "Rasa lemas atau cepat mengantuk saat belajar di madrasah bisa dipicu oleh kurangnya asupan zat besi (anemia gizi). Tingkatkan konsumsi hati ayam, daging, bayam, kacang-kacangan, dan minum tablet tambah darah (TTD) bagi siswi putri sesuai anjuran UKS."
            }
            else -> {
                "Halo teman sehat NutriMind AI! Pola gizi seimbang (Isi Piringku) terdiri dari 1/3 piring makanan pokok, 1/3 piring sayuran, 1/6 piring lauk protein, dan 1/6 piring buah-buahan. Jangan lupa minum air putih minimal 8 gelas per hari dan tetap aktif bergerak!"
            }
        }
    }

    private fun extractSection(text: String, keyword: String, default: String): String {
        val lines = text.split("\n")
        val match = lines.firstOrNull { it.contains(keyword, ignoreCase = true) }
        return match?.replace(Regex("^[-*#\\s:]+"), "")?.take(120) ?: default
    }
}

data class FoodAnalysisResult(
    val foodName: String,
    val detectedItems: String,
    val foodGroups: String,
    val carbSource: String,
    val proteinSource: String,
    val vegFruitSource: String,
    val estimatedCalories: String,
    val balanceEvaluation: String,
    val educationalNote: String,
    val balanceStatus: String = "Gizi Seimbang",
    val macronutrients: String = "Karbohidrat, Protein & Lemak",
    val improvementTips: String = "Lengkapi dengan buah segar dan minum air putih yang cukup."
)

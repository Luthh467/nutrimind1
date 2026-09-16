package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'SISWA' ORDER BY name ASC")
    fun getAllStudents(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface UksOfficerDao {
    @Query("SELECT * FROM uks_officers WHERE email = :email LIMIT 1")
    suspend fun getOfficerByEmail(email: String): UksOfficerEntity?

    @Query("SELECT * FROM uks_officers ORDER BY registeredAt DESC")
    fun getAllOfficers(): Flow<List<UksOfficerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfficer(officer: UksOfficerEntity)

    @Query("UPDATE uks_officers SET isVerified = :verified WHERE id = :id")
    suspend fun updateVerification(id: String, verified: Boolean)
}

@Dao
interface GiziCheckDao {
    @Query("SELECT * FROM gizi_checks WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getChecksForStudent(studentId: String): Flow<List<GiziCheckEntity>>

    @Query("SELECT * FROM gizi_checks WHERE studentId = :studentId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestCheckForStudent(studentId: String): Flow<GiziCheckEntity?>

    @Query("SELECT * FROM gizi_checks ORDER BY timestamp DESC")
    fun getAllChecks(): Flow<List<GiziCheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: GiziCheckEntity): Long

    @Query("DELETE FROM gizi_checks WHERE id = :id")
    suspend fun deleteCheck(id: Long)
}

@Dao
interface DailyCheckDao {
    @Query("SELECT * FROM daily_checks WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getDailyChecksForStudent(studentId: String): Flow<List<DailyCheckEntity>>

    @Query("SELECT * FROM daily_checks WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getDailyCheckByDate(studentId: String, date: String): DailyCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyCheck(check: DailyCheckEntity): Long

    @Update
    suspend fun updateDailyCheck(check: DailyCheckEntity)
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getFoodLogsForStudent(studentId: String): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(foodLog: FoodLogEntity): Long
}

@Dao
interface EducationArticleDao {
    @Query("SELECT * FROM education_articles")
    fun getAllArticles(): Flow<List<EducationArticleEntity>>

    @Query("SELECT * FROM education_articles WHERE category = :category")
    fun getArticlesByCategory(category: String): Flow<List<EducationArticleEntity>>

    @Query("SELECT * FROM education_articles WHERE isFavorite = 1")
    fun getFavoriteArticles(): Flow<List<EducationArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<EducationArticleEntity>)

    @Query("UPDATE education_articles SET isFavorite = :fav WHERE id = :id")
    suspend fun toggleFavorite(id: String, fav: Boolean)
}

@Dao
interface UksFollowUpDao {
    @Query("SELECT * FROM uks_follow_ups WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getFollowUpsForStudent(studentId: String): Flow<List<UksFollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: UksFollowUpEntity): Long
}

@Database(
    entities = [
        UserEntity::class,
        UksOfficerEntity::class,
        GiziCheckEntity::class,
        DailyCheckEntity::class,
        FoodLogEntity::class,
        EducationArticleEntity::class,
        UksFollowUpEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun uksOfficerDao(): UksOfficerDao
    abstract fun giziCheckDao(): GiziCheckDao
    abstract fun dailyCheckDao(): DailyCheckDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun educationArticleDao(): EducationArticleDao
    abstract fun uksFollowUpDao(): UksFollowUpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutrimind_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

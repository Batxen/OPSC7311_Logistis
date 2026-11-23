package za.ac.iie.opsc7311_introduction

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import android.content.Context
import androidx.room.Room

/*
 * Local Room database: entities, DAOs and the database singleton.
 * Reference: Android Developers, 2024. Save data in a local database using Room.
 * Available at: https://developer.android.com/training/data-storage/room [Accessed 23 November 2026].
 */
@Entity(tableName = "users")
data class dcUser(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dbusername: String,
    val dbpassword: String
)//user class

@Entity(tableName = "categories")
data class dcCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cat: String,
    val catmax: String,
    val catmin: String,
    val tag: String = "Essential"
)//category class

@Entity(tableName = "expenses")
data class dcExpenses(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val date: String,
    val expense: String,
    val item: String,
    val photo: String,
    val startTime: String = "",
    val endTime: String = ""
)//expenses class

@Entity(tableName = "monthly_goals")
data class dcMonthlyGoal(
    @PrimaryKey val id: Int = 1,
    val minGoal: String,
    val maxGoal: String
)//monthly spending goal for the current month

//Data Access Objects - one for each table/entity
@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: dcUser)

    @Update
    suspend fun update(user: dcUser)

    @Delete
    suspend fun delete(user: dcUser)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("UPDATE users SET dbpassword = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: Int, newPassword: String)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Int): dcUser?

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<dcUser>

    @Query("SELECT * FROM users WHERE dbusername = :user")
    suspend fun getByName(user: String): dcUser?
}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: dcCategory): Long

    @Update
    suspend fun update(category: dcCategory)

    @Delete
    suspend fun delete(dccategory: dcCategory)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("DELETE FROM categories WHERE id = :itemId")
    suspend fun deleteItem(itemId: Int)

    @Query("DELETE FROM categories WHERE cat = :catitem")
    suspend fun deleteCategory(catitem: String)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Int): dcCategory?

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<dcCategory>
}

@Dao
interface ExpensesDao {
    @Insert
    suspend fun insert(expense: dcExpenses): Long

    @Update
    suspend fun update(expense: dcExpenses)

    @Delete
    suspend fun delete(expense: dcExpenses)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<dcExpenses>
}

@Dao
interface MonthlyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: dcMonthlyGoal)

    @Query("SELECT * FROM monthly_goals WHERE id = 1")
    suspend fun getGoal(): dcMonthlyGoal?
}

@Database(
    entities = [dcUser::class, dcCategory::class, dcExpenses::class, dcMonthlyGoal::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expensesDao(): ExpensesDao
    abstract fun monthlyGoalDao(): MonthlyGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // One shared database for the whole app (singleton)
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "mydb"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
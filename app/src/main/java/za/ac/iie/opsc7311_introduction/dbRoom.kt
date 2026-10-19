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

class dbRoom {
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
        val catmin: String
    )//category class

    @Entity(tableName = "expenses")
    data class dcExpenses(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val category: String,
        val date: String,
        val expense: String,
        val item: String,
        val photo: String
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
        //Define DAO  data access object interface

        @Insert
        suspend fun insert(user: dcUser)

        @Update
        suspend fun update(user: dcUser)

        @Delete
        suspend fun delete(user: dcUser)

        @Query("DELETE FROM users")
        suspend fun deleteAll()

        // Example: update a specific field with a query
        @Query("UPDATE users SET dbpassword = :newPassword WHERE id = :userId")
        suspend fun updatePassword(userId: Int, newPassword: String)

        @Query("SELECT * FROM users WHERE id = :id")
        suspend fun getById(id: Int): dcUser?

        @Query("SELECT * FROM users")
        suspend fun getAll(): List<dcUser>

        @Query("SELECT * FROM users WHERE dbusername = :user")
        suspend fun getByName(user: String): dcUser?
    }//interface userDao

    @Dao
    interface CategoryDao {
        //Define DAO  data access object interface
        @Insert
        suspend fun insert(category: dcCategory)

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
    }//interface CategoryDao

    @Dao
    interface ExpensesDao {
        //Define DAO  data access object interface
        // accessing external class
        @Insert
        suspend fun insert(expense: dcExpenses)

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
        version = 2
    )
    abstract class AppDatabase : RoomDatabase() {
        abstract fun userDao(): UserDao
        abstract fun categoryDao(): CategoryDao
        abstract fun expensesDao(): ExpensesDao
        abstract fun monthlyGoalDao(): MonthlyGoalDao
    }


}//class dbRoom
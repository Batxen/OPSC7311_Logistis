package za.ac.iie.opsc7311_introduction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Outcome of a register / login attempt against Firebase.
enum class AuthResult { OK, TAKEN, NOT_FOUND, WRONG_PASSWORD, OFFLINE }

// Remembers which user is logged in (survives the app being restarted).
object Session {
    private const val PREFS = "logistis_session"
    private const val KEY_USER = "username"

    fun login(context: Context, username: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER, username).apply()
    }

    fun username(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER, "") ?: ""
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_USER).apply()
    }
}

// Lets a coroutine wait for a Firebase Task to finish.
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}


// All Firebase Realtime Database access lives here.
// Data layout:  users / {username} / password   (salted SHA-256 hash, never the password)
// Based on the Firebase documentation "Read and write data on Android":
// https://firebase.google.com/docs/database/android/read-and-write

object FirebaseSync {
    private const val TAG = "BXR"
    private const val TIMEOUT_MS = 8000L

    // persistence must be switched on before the first use: writes made while the phone
    // is offline are queued and sent automatically when it reconnects
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance().apply { setPersistenceEnabled(true) }
    }

    fun userRef(username: String): DatabaseReference =
        database.getReference("users").child(username)

    // Usernames become database keys, so only letters, numbers, _ and - are allowed.
    fun isValidUsername(username: String): Boolean =
        Regex("^[a-z0-9_-]{3,20}\$").matches(username)

    // The username is mixed in as a salt so equal passwords do not give equal hashes.
    private fun hashPassword(username: String, password: String): String =
        clsTools().hashPassword("$username:$password")

    suspend fun registerUser(username: String, password: String): AuthResult {
        return try {
            val existing = withTimeoutOrNull(TIMEOUT_MS) {
                userRef(username).child("password").get().await()
            }
            if (existing == null) {
                AuthResult.OFFLINE
            } else if (existing.exists()) {
                Log.w(TAG, "Register failed: $username already exists online")
                AuthResult.TAKEN
            } else {
                val written = withTimeoutOrNull(TIMEOUT_MS) {
                    userRef(username).child("password")
                        .setValue(hashPassword(username, password)).await()
                    true
                }
                if (written == true) {
                    Log.i(TAG, "User registered online: $username")
                    AuthResult.OK
                } else {
                    AuthResult.OFFLINE
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Register failed", e)
            AuthResult.OFFLINE
        }
    }

    suspend fun loginUser(username: String, password: String): AuthResult {
        return try {
            val snapshot = withTimeoutOrNull(TIMEOUT_MS) {
                userRef(username).child("password").get().await()
            }
            when {
                snapshot == null -> AuthResult.OFFLINE
                !snapshot.exists() -> {
                    Log.w(TAG, "Login failed: no online account for $username")
                    AuthResult.NOT_FOUND
                }
                snapshot.getValue(String::class.java) == hashPassword(username, password) -> {
                    Log.i(TAG, "Login success (online check): $username")
                    AuthResult.OK
                }
                else -> {
                    Log.w(TAG, "Login failed: wrong password for $username")
                    AuthResult.WRONG_PASSWORD
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            AuthResult.OFFLINE
        }
    }

    // Saving data online. These are fire-and-forget: the app keeps working at once, and
    // Firebase (with persistence on) sends the data as soon as the phone is online.
    fun pushCategory(username: String, category: dcCategory) {
        if (username.isBlank()) {
            Log.w(TAG, "Firebase: no logged in user, category not saved online")
            return
        }
        val values = mapOf(
            "cat" to category.cat,
            "catmax" to category.catmax,
            "catmin" to category.catmin,
            "tag" to category.tag
        )
        userRef(username).child("categories").child(category.id.toString())
            .setValue(values)
            .addOnSuccessListener { Log.i(TAG, "Firebase: category '${category.cat}' saved online") }
            .addOnFailureListener { Log.e(TAG, "Firebase: category save failed", it) }
    }

    fun pushExpense(username: String, expense: dcExpenses) {
        if (username.isBlank()) {
            Log.w(TAG, "Firebase: no logged in user, expense not saved online")
            return
        }
        val values = mapOf(
            "category" to expense.category,
            "date" to expense.date,
            "expense" to expense.expense,
            "item" to expense.item,
            "photoPath" to expense.photo,
            "startTime" to expense.startTime,
            "endTime" to expense.endTime
        )
        userRef(username).child("expenses").child(expense.id.toString())
            .setValue(values)
            .addOnSuccessListener { Log.i(TAG, "Firebase: expense '${expense.item}' saved online") }
            .addOnFailureListener { Log.e(TAG, "Firebase: expense save failed", it) }
    }

    fun pushGoal(username: String, minGoal: String, maxGoal: String) {
        if (username.isBlank()) {
            Log.w(TAG, "Firebase: no logged in user, goal not saved online")
            return
        }
        val values = mapOf("min" to minGoal, "max" to maxGoal)
        userRef(username).child("goal")
            .setValue(values)
            .addOnSuccessListener { Log.i(TAG, "Firebase: monthly goal saved online") }
            .addOnFailureListener { Log.e(TAG, "Firebase: goal save failed", it) }
    }

    // Photos: a shrunken copy is stored as text under users/{username}/photos/{id}
    suspend fun pushPhoto(username: String, expenseId: Int, photoPath: String) {
        if (username.isBlank() || photoPath.isBlank()) return

        // shrink and encode off the main thread
        val encoded: String? = withContext(Dispatchers.IO) {
            try {
                if (!File(photoPath).exists()) return@withContext null

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(photoPath, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1000 || bounds.outHeight / sample > 1000) sample *= 2

                val bitmap = BitmapFactory.decodeFile(
                    photoPath,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                ) ?: return@withContext null

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(TAG, "Firebase: could not prepare photo", e)
                null
            }
        }

        if (encoded == null) {
            Log.w(TAG, "Firebase: photo for expense $expenseId not saved online")
            return
        }
        userRef(username).child("photos").child(expenseId.toString())
            .setValue(encoded)
            .addOnSuccessListener {
                Log.i(TAG, "Firebase: photo for expense $expenseId saved online (${encoded.length / 1024} KB)")
            }
            .addOnFailureListener { Log.e(TAG, "Firebase: photo save failed", it) }
    }

    // Downloads one photo and saves it in the app's storage. Returns the new file path.
    private suspend fun restorePhoto(context: Context, username: String, expenseId: Int): String? {
        return try {
            val snapshot = withTimeoutOrNull(TIMEOUT_MS * 2) {
                userRef(username).child("photos").child(expenseId.toString()).get().await()
            }
            val encoded = snapshot?.getValue(String::class.java) ?: return null

            withContext(Dispatchers.IO) {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                val dir = File(context.filesDir, "photos").apply { mkdirs() }
                val file = File(dir, "restored_$expenseId.jpg")
                file.writeBytes(bytes)
                file.absolutePath
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase: could not restore photo for expense $expenseId", e)
            null
        }
    }

    // Loading data from Firebase. Called at login: the local Room database is cleared and
    // refilled with this user's online data, so every account only sees its own data.
    // Returns false if the data could not be loaded.
    suspend fun pullAll(context: Context, username: String): Boolean {
        return try {
            // the parts are read separately so the large photos are not downloaded here
            val categoriesSnap = withTimeoutOrNull(TIMEOUT_MS) {
                userRef(username).child("categories").get().await()
            }
            val expensesSnap = withTimeoutOrNull(TIMEOUT_MS) {
                userRef(username).child("expenses").get().await()
            }
            val goalSnap = withTimeoutOrNull(TIMEOUT_MS) {
                userRef(username).child("goal").get().await()
            }

            if (categoriesSnap == null || expensesSnap == null || goalSnap == null) {
                Log.w(TAG, "Firebase pull failed: server not reachable")
                false
            } else {
                val categories = categoriesSnap.children.mapNotNull { node ->
                    val id = node.key?.toIntOrNull()
                    val name = node.child("cat").getValue(String::class.java)
                    if (id == null || name == null) null else dcCategory(
                        id = id,
                        cat = name,
                        catmax = node.child("catmax").getValue(String::class.java) ?: "0",
                        catmin = node.child("catmin").getValue(String::class.java) ?: "0",
                        tag = node.child("tag").getValue(String::class.java) ?: "Essential"
                    )
                }

                val expenses = expensesSnap.children.mapNotNull { node ->
                    val id = node.key?.toIntOrNull()
                    val amount = node.child("expense").getValue(String::class.java)
                    val date = node.child("date").getValue(String::class.java)
                    if (id == null || amount == null || date == null) null else dcExpenses(
                        id = id,
                        category = node.child("category").getValue(String::class.java) ?: "",
                        date = date,
                        expense = amount,
                        item = node.child("item").getValue(String::class.java) ?: "",
                        photo = node.child("photoPath").getValue(String::class.java) ?: "",
                        startTime = node.child("startTime").getValue(String::class.java) ?: "",
                        endTime = node.child("endTime").getValue(String::class.java) ?: ""
                    )
                }

                val goalMin = goalSnap.child("min").getValue(String::class.java)
                val goalMax = goalSnap.child("max").getValue(String::class.java)

                // replace the local copy with this user's online data
                val localDb = AppDatabase.getInstance(context.applicationContext)
                withContext(Dispatchers.IO) { localDb.clearAllTables() }
                for (category in categories) localDb.categoryDao().insert(category)
                for (expense in expenses) localDb.expensesDao().insert(expense)
                if (goalMin != null && goalMax != null) {
                    localDb.monthlyGoalDao().upsert(dcMonthlyGoal(minGoal = goalMin, maxGoal = goalMax))
                }

                // bring back photos that are not on this phone (new install / another phone)
                var restored = 0
                for (expense in expenses) {
                    if (expense.photo.isBlank()) continue
                    if (File(expense.photo).exists()) continue      // still stored on this phone
                    val newPath = restorePhoto(context, username, expense.id)
                    localDb.expensesDao().update(expense.copy(photo = newPath ?: ""))
                    if (newPath != null) restored++
                }

                Log.i(
                    TAG,
                    "Firebase pull done for $username: ${categories.size} categories, " +
                            "${expenses.size} expenses, $restored photos restored, " +
                            "goal ${if (goalMin != null) "set" else "not set"}"
                )
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase pull failed", e)
            false
        }
    }
}
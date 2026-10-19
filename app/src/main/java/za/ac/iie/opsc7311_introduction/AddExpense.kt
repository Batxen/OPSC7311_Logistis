package za.ac.iie.opsc7311_introduction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Screen for capturing a new expense: amount, description, category,
 * date, start/end time and an optional photo. Saves to the Room "expenses" table.
 */
class AddExpense : AppCompatActivity() {
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var spCategory: Spinner
    private lateinit var btnDate: Button
    private lateinit var btnStart: Button
    private lateinit var btnEnd: Button
    private lateinit var btnPhoto: Button
    private lateinit var tvPhotoStatus: TextView
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    private var dateValue = ""          // yyyy-MM-dd
    private var startText = ""          // HH:mm
    private var endText = ""            // HH:mm
    private var startMinutes = -1
    private var endMinutes = -1
    private var photoPath = ""          // absolute path of the copy kept in app storage

    // Gallery picker - no storage permission needed
    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                lifecycleScope.launch {
                    val saved = withContext(Dispatchers.IO) { copyPhoto(uri) }
                    if (saved != null) {
                        photoPath = saved
                        tvPhotoStatus.text = "Photo attached"
                    } else {
                        Toast.makeText(this@AddExpense, "Could not use that photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_expense)

        etAmount = findViewById(R.id.etExpenseAmount)
        etDescription = findViewById(R.id.etExpenseDescription)
        spCategory = findViewById(R.id.spExpenseCategory)
        btnDate = findViewById(R.id.btnExpenseDate)
        btnStart = findViewById(R.id.btnStartTime)
        btnEnd = findViewById(R.id.btnEndTime)
        btnPhoto = findViewById(R.id.btnPickPhoto)
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus)
        btnSave = findViewById(R.id.btnSaveExpense)
        btnBack = findViewById(R.id.btnExpenseBack)

        val tools = clsTools()
        val db = Room.databaseBuilder(
            applicationContext,
            dbRoom.AppDatabase::class.java, "mydb"
        ).fallbackToDestructiveMigration(true)
            .build()
        val categoryDao = db.categoryDao()
        val expensesDao = db.expensesDao()

        // default the date to today
        val now = Calendar.getInstance()
        dateValue = String.format(
            Locale.US, "%04d-%02d-%02d",
            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)
        )
        btnDate.text = "Date: $dateValue"

        // load categories into the spinner
        lifecycleScope.launch {
            val names = categoryDao.getAll().map { it.cat }
            if (names.isEmpty()) {
                Toast.makeText(
                    this@AddExpense,
                    "Create a category first (Menu > Categories)",
                    Toast.LENGTH_LONG
                ).show()
            }
            val adapter = object : ArrayAdapter<String>(
                this@AddExpense, android.R.layout.simple_spinner_item, names
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.BLACK)
                    }
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.BLACK)
                        setBackgroundColor(Color.parseColor("#EBE19D"))
                        setPadding(24, 24, 24, 24)
                    }
                }
            }
            spCategory.adapter = adapter
        }

        btnDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                dateValue = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                btnDate.text = "Date: $dateValue"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnStart.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                startMinutes = h * 60 + min
                startText = String.format(Locale.US, "%02d:%02d", h, min)
                btnStart.text = "Start: $startText"
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        btnEnd.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                endMinutes = h * 60 + min
                endText = String.format(Locale.US, "%02d:%02d", h, min)
                btnEnd.text = "End: $endText"
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        btnPhoto.setOnClickListener { pickPhoto.launch("image/*") }

        btnSave.setOnClickListener {
            // 'and' (not &&) so every field shows its own error
            val fieldsOk = tools.validateAmount(etAmount) and tools.validateField(etDescription)
            if (!fieldsOk) {
                Toast.makeText(this, "Please fill in the amount and description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = etAmount.text.toString().replace(",", "").toDouble()
            if (amount <= 0) {
                etAmount.error = "Amount must be greater than 0"
                return@setOnClickListener
            }

            val category = spCategory.selectedItem as? String
            if (category == null) {
                Toast.makeText(this, "Create a category first (Menu > Categories)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (startMinutes < 0 || endMinutes < 0) {
                Toast.makeText(this, "Please choose a start and end time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (endMinutes <= startMinutes) {
                Toast.makeText(this, "End time must be after the start time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val expense = dbRoom.dcExpenses(
                category = category,
                date = dateValue,
                expense = amount.toString(),
                item = etDescription.text.toString().trim(),
                photo = photoPath
            )


            lifecycleScope.launch {
                expensesDao.insert(expense)
                Log.i("BXR", "Expense saved: ${expense.item} R${expense.expense} ($category)")
                Toast.makeText(this@AddExpense, "Expense saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    /** Copies the chosen image into app storage (scaled down) so it stays available later. */
    private fun copyPhoto(uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2

            val bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val dir = File(filesDir, "photos").apply { mkdirs() }
            val file = File(dir, "expense_${UUID.randomUUID()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("BXR", "Photo copy failed", e)
            null
        }
    }
}
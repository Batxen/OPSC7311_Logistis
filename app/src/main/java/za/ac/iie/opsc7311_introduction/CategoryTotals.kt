package za.ac.iie.opsc7311_introduction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CategoryTotals : AppCompatActivity() {
    private lateinit var lvCategoryTotals: ListView
    private lateinit var tvNoCategoryTotals: TextView
    private lateinit var tvPeriodTotal: TextView
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnCategoryTotalsBack: Button

    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val startDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        startOfDay()
    }
    private val endDate: Calendar = Calendar.getInstance().apply { endOfDay() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_totals)

        lvCategoryTotals = findViewById(R.id.lvCategoryTotals)
        tvNoCategoryTotals = findViewById(R.id.tvNoCategoryTotals)
        tvPeriodTotal = findViewById(R.id.tvPeriodTotal)
        btnStartDate = findViewById(R.id.btnStartDate)
        btnEndDate = findViewById(R.id.btnEndDate)
        btnCategoryTotalsBack = findViewById(R.id.btnCategoryTotalsBack)

        updateDateButtons()

        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }
        btnCategoryTotalsBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadCategoryTotals()
    }

    private fun showDatePicker(pickingStart: Boolean) {
        val current = if (pickingStart) startDate else endDate
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                    if (pickingStart) startOfDay() else endOfDay()
                }
                if (pickingStart) {
                    if (selected.after(endDate)) {
                        Toast.makeText(this, "Start date cannot be after the end date", Toast.LENGTH_SHORT)
                            .show()
                        return@DatePickerDialog
                    }
                    startDate.timeInMillis = selected.timeInMillis
                    startDate.startOfDay()
                } else {
                    if (selected.before(startDate)) {
                        Toast.makeText(this, "End date cannot be before the start date", Toast.LENGTH_SHORT)
                            .show()
                        return@DatePickerDialog
                    }
                    endDate.timeInMillis = selected.timeInMillis
                    endDate.endOfDay()
                }
                updateDateButtons()
                loadCategoryTotals()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtons() {
        btnStartDate.text = "From ${displayFormat.format(startDate.time)}"
        btnEndDate.text = "To ${displayFormat.format(endDate.time)}"
    }

    private fun loadCategoryTotals() {
        val db = Room.databaseBuilder(
            applicationContext,
            dbRoom.AppDatabase::class.java, "mydb"
        ).fallbackToDestructiveMigration(true)
            .build()

        val periodStart = startDate.timeInMillis
        val periodEnd = endDate.timeInMillis

        lifecycleScope.launch {
            val expenses = db.expensesDao().getAll()
            val totals = linkedMapOf<String, Double>()
            var periodTotal = 0.0

            for (expense in expenses) {
                val expenseDate = parseExpenseDate(expense.date) ?: continue
                if (expenseDate.time < periodStart || expenseDate.time > periodEnd) {
                    continue
                }
                val categoryName = expense.category.ifBlank { "Uncategorised" }
                val amount = parseAmount(expense.expense)
                totals[categoryName] = (totals[categoryName] ?: 0.0) + amount
                periodTotal += amount
            }

            val rows = totals.entries
                .sortedByDescending { it.value }
                .map { "${it.key}   R${"%.2f".format(it.value)}" }
            val totalLabel = "Total for period: R${"%.2f".format(periodTotal)}"

            runOnUiThread {
                tvPeriodTotal.text = totalLabel
                if (rows.isEmpty()) {
                    tvNoCategoryTotals.visibility = View.VISIBLE
                    lvCategoryTotals.adapter = null
                } else {
                    tvNoCategoryTotals.visibility = View.GONE
                    lvCategoryTotals.adapter = ArrayAdapter(
                        this@CategoryTotals,
                        android.R.layout.simple_list_item_1,
                        rows
                    )
                }
            }
        }
    }

    private fun parseAmount(raw: String): Double {
        return raw.replace("R", "", ignoreCase = true)
            .replace(",", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun parseExpenseDate(dateStr: String): Date? {
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd")
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                parser.isLenient = false
                return parser.parse(dateStr)
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun Calendar.startOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.endOfDay() {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
}

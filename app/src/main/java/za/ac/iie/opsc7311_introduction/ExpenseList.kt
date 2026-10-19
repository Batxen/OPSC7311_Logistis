package za.ac.iie.opsc7311_introduction

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class ExpenseList : AppCompatActivity() {
    private lateinit var lvExpenses: ListView
    private lateinit var tvNoExpenses: TextView
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnExpenseListBack: Button

    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val startDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        startOfDay()
    }
    private val endDate: Calendar = Calendar.getInstance().apply { endOfDay() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense_list)

        lvExpenses = findViewById(R.id.lvExpenses)
        tvNoExpenses = findViewById(R.id.tvNoExpenses)
        btnStartDate = findViewById(R.id.btnExpenseStartDate)
        btnEndDate = findViewById(R.id.btnExpenseEndDate)
        btnExpenseListBack = findViewById(R.id.btnExpenseListBack)

        updateDateButtons()
        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }
        btnExpenseListBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
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
                loadExpenses()
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

    private fun loadExpenses() {
        val db = Room.databaseBuilder(
            applicationContext,
            dbRoom.AppDatabase::class.java, "mydb"
        ).fallbackToDestructiveMigration(true)
            .build()

        val periodStart = startDate.timeInMillis
        val periodEnd = endDate.timeInMillis

        lifecycleScope.launch {
            val filtered = db.expensesDao().getAll()
                .mapNotNull { expense ->
                    val expenseDate = parseExpenseDate(expense.date) ?: return@mapNotNull null
                    if (expenseDate.time < periodStart || expenseDate.time > periodEnd) {
                        null
                    } else {
                        expense to expenseDate
                    }
                }
                .sortedByDescending { it.second.time }
                .map { it.first }

            runOnUiThread {
                if (filtered.isEmpty()) {
                    tvNoExpenses.visibility = View.VISIBLE
                    lvExpenses.adapter = null
                } else {
                    tvNoExpenses.visibility = View.GONE
                    lvExpenses.adapter = ExpenseAdapter(this@ExpenseList, filtered)
                }
            }
        }
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

    private class ExpenseAdapter(
        context: Context,
        private val expenses: List<dbRoom.dcExpenses>
    ) : ArrayAdapter<dbRoom.dcExpenses>(context, 0, expenses) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_expense, parent, false)
            val expense = expenses[position]
            val itemName = expense.item.ifBlank { "Untitled item" }
            val categoryName = expense.category.ifBlank { "Uncategorised" }
            val amount = expense.expense.ifBlank { "0" }

            view.findViewById<TextView>(R.id.tvExpenseTitle).text = "$itemName  •  R$amount"
            view.findViewById<TextView>(R.id.tvExpenseSubtitle).text =
                "${expense.date}  •  $categoryName"

            val btnViewPhoto = view.findViewById<Button>(R.id.btnViewPhoto)
            val hasPhoto = expense.photo.isNotBlank()
            btnViewPhoto.visibility = if (hasPhoto) View.VISIBLE else View.GONE
            btnViewPhoto.setOnClickListener {
                val intent = Intent(context, ExpensePhoto::class.java)
                intent.putExtra(ExpensePhoto.EXTRA_PHOTO, expense.photo)
                intent.putExtra(ExpensePhoto.EXTRA_ITEM, itemName)
                context.startActivity(intent)
            }
            return view
        }
    }
}

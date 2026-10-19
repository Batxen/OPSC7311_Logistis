package za.ac.iie.opsc7311_introduction

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyGoals : AppCompatActivity() {
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var btnSaveGoal: Button
    private lateinit var btnGoalsBack: Button
    private lateinit var tvCurrentSpend: TextView
    private lateinit var tvGoalStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_monthly_goals)

        val tools = clsTools()
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        btnGoalsBack = findViewById(R.id.btnGoalsBack)
        tvCurrentSpend = findViewById(R.id.tvCurrentSpend)
        tvGoalStatus = findViewById(R.id.tvGoalStatus)

        val db = Room.databaseBuilder(
            applicationContext,
            dbRoom.AppDatabase::class.java, "mydb"
        ).fallbackToDestructiveMigration(true)
            .build()
        val goalDao = db.monthlyGoalDao()
        val expensesDao = db.expensesDao()

        loadGoalsAndSpend(goalDao, expensesDao)

        btnSaveGoal.setOnClickListener {
            val minValid = tools.validateAmount(etMinGoal)
            val maxValid = tools.validateAmount(etMaxGoal)
            if (!minValid || !maxValid) {
                Toast.makeText(this, "Enter valid minimum and maximum amounts", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val minGoal = etMinGoal.text.toString().replace(",", "").toDouble()
            val maxGoal = etMaxGoal.text.toString().replace(",", "").toDouble()
            if (minGoal > maxGoal) {
                etMinGoal.error = "Minimum cannot be greater than maximum"
                Toast.makeText(
                    this,
                    "Minimum goal cannot be greater than maximum goal",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                goalDao.upsert(
                    dbRoom.dcMonthlyGoal(
                        minGoal = minGoal.toString(),
                        maxGoal = maxGoal.toString()
                    )
                )
                runOnUiThread {
                    Toast.makeText(this@MonthlyGoals, "Monthly goals saved", Toast.LENGTH_SHORT)
                        .show()
                }
                loadGoalsAndSpend(goalDao, expensesDao)
            }
        }

        btnGoalsBack.setOnClickListener { finish() }
    }

    private fun loadGoalsAndSpend(
        goalDao: dbRoom.MonthlyGoalDao,
        expensesDao: dbRoom.ExpensesDao
    ) {
        lifecycleScope.launch {
            val goal = goalDao.getGoal()
            val spent = currentMonthSpend(expensesDao.getAll())
            runOnUiThread {
                if (goal != null) {
                    etMinGoal.setText(goal.minGoal)
                    etMaxGoal.setText(goal.maxGoal)
                }
                tvCurrentSpend.text = "Spent this month: R${"%.2f".format(spent)}"
                tvGoalStatus.text = goalStatus(goal, spent)
            }
        }
    }

    private fun currentMonthSpend(expenses: List<dbRoom.dcExpenses>): Double {
        return expenses.filter { isInCurrentMonth(it.date) }
            .sumOf { parseAmount(it.expense) }
    }

    private fun parseAmount(raw: String): Double {
        return raw.replace("R", "", ignoreCase = true)
            .replace(",", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun isInCurrentMonth(dateStr: String): Boolean {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd")
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                parser.isLenient = false
                val parsed = parser.parse(dateStr) ?: continue
                val cal = Calendar.getInstance()
                cal.time = parsed
                if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun goalStatus(goal: dbRoom.dcMonthlyGoal?, spent: Double): String {
        if (goal == null) {
            return "No monthly goal set yet"
        }
        val minGoal = goal.minGoal.toDoubleOrNull() ?: 0.0
        val maxGoal = goal.maxGoal.toDoubleOrNull() ?: 0.0
        return when {
            spent < minGoal -> "Below the minimum goal of R${"%.2f".format(minGoal)}"
            spent > maxGoal -> "Over the maximum goal of R${"%.2f".format(maxGoal)}"
            else -> "On track between R${"%.2f".format(minGoal)} and R${"%.2f".format(maxGoal)}"
        }
    }
}

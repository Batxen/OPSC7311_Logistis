package za.ac.iie.opsc7311_introduction

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MonthlyGoals : AppCompatActivity() {
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var btnSaveGoal: Button
    private lateinit var btnGoalsBack: Button
    private lateinit var tvCurrentSpend: TextView
    private lateinit var tvGoalStatus: TextView
    private lateinit var chartGoals: LineChart

    // length of the "past month" window shown on the graph
    private val periodDays = 30

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
        chartGoals = findViewById(R.id.chartGoals)

        val db = AppDatabase.getInstance(applicationContext)
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
                    dcMonthlyGoal(
                        minGoal = minGoal.toString(),
                        maxGoal = maxGoal.toString()
                    )
                )
                Log.i("BXR", "Monthly goals saved: min R$minGoal, max R$maxGoal")
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
        goalDao: MonthlyGoalDao,
        expensesDao: ExpensesDao
    ) {
        lifecycleScope.launch {
            val goal = goalDao.getGoal()
            val daily = dailySpend(expensesDao.getAll())
            val spent = daily.sum()
            Log.i("BXR", "Last $periodDays days spend: R$spent (goal: ${goal?.minGoal} - ${goal?.maxGoal})")

            runOnUiThread {
                if (goal != null) {
                    etMinGoal.setText(goal.minGoal)
                    etMaxGoal.setText(goal.maxGoal)
                }
                tvCurrentSpend.text = "Spent in the last $periodDays days: R${"%.2f".format(spent)}"
                showGoalStatus(goal, spent)
                showGoalChart(goal, daily)
            }
        }
    }

    private fun dailySpend(expenses: List<dcExpenses>): DoubleArray {
        val daily = DoubleArray(periodDays)
        val today = Calendar.getInstance().apply { startOfDay() }

        for (expense in expenses) {
            val date = parseExpenseDate(expense.date) ?: continue
            val day = Calendar.getInstance().apply {
                time = date
                startOfDay()
            }
            val daysAgo = Math.round(
                (today.timeInMillis - day.timeInMillis) / 86_400_000.0
            ).toInt()
            if (daysAgo in 0 until periodDays) {
                daily[periodDays - 1 - daysAgo] += parseAmount(expense.expense)
            }
        }
        return daily
    }

    private fun showGoalStatus(goal: dcMonthlyGoal?, spent: Double) {
        if (goal == null) {
            tvGoalStatus.text = "No monthly goal set yet"
            tvGoalStatus.setBackgroundColor(Color.parseColor("#841919"))
            return
        }
        val minGoal = goal.minGoal.toDoubleOrNull() ?: 0.0
        val maxGoal = goal.maxGoal.toDoubleOrNull() ?: 0.0
        val percentOfMax = if (maxGoal > 0) (spent / maxGoal * 100).toInt() else 0

        val (message, colour) = when {
            spent < minGoal ->
                "Below the minimum goal of R${"%.2f".format(minGoal)} (${percentOfMax}% of your max)" to "#FFC107"
            spent > maxGoal ->
                "Over the maximum goal of R${"%.2f".format(maxGoal)} (${percentOfMax}% of your max)" to "#FF5252"
            else ->
                "On track between R${"%.2f".format(minGoal)} and R${"%.2f".format(maxGoal)} (${percentOfMax}% of your max)" to "#4CAF50"
        }
        tvGoalStatus.text = message
        tvGoalStatus.setBackgroundColor(Color.parseColor(colour))
    }

    /**
     * Line graph of the running total of spending over the last 30 days, with the min and
     * max goals as dashed lines. Based on the lecturer's MPAndroidChart multi-line sample
     * and the MPAndroidChart library by PhilJay (https://github.com/PhilJay/MPAndroidChart).
     */
    private fun showGoalChart(goal: dcMonthlyGoal?, daily: DoubleArray) {
        val textColor = Color.parseColor("#EBE19D")

        // x-axis labels: the date of each day in the window
        val labelFormat = SimpleDateFormat("d MMM", Locale.US)
        val labels = List(periodDays) { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -(periodDays - 1 - i))
            labelFormat.format(cal.time)
        }

        // running total of what has been spent so far
        val actual = ArrayList<Entry>()
        var running = 0.0
        daily.forEachIndexed { i, amount ->
            running += amount
            actual.add(Entry(i.toFloat(), running.toFloat()))
        }
        val actualSet = LineDataSet(actual, "Spent (running total)").apply {
            color = textColor
            lineWidth = 3f
            setDrawCircles(false)
            setDrawValues(false)
        }
        val lineData = LineData(actualSet)

        if (goal != null) {
            val minGoal = goal.minGoal.toDoubleOrNull() ?: 0.0
            val maxGoal = goal.maxGoal.toDoubleOrNull() ?: 0.0
            lineData.addDataSet(goalLine(minGoal, "Min goal", Color.parseColor("#4CAF50")))
            lineData.addDataSet(goalLine(maxGoal, "Max goal", Color.parseColor("#FF5252")))
        }

        chartGoals.data = lineData
        chartGoals.description.isEnabled = false
        chartGoals.axisRight.isEnabled = false
        chartGoals.axisLeft.apply {
            axisMinimum = 0f
            this.textColor = textColor
            gridColor = Color.parseColor("#555555")
        }
        chartGoals.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setLabelCount(6, false)
            setDrawGridLines(false)
            this.textColor = textColor
        }
        chartGoals.legend.textColor = textColor
        chartGoals.animateX(800)
        chartGoals.invalidate()
    }

    private fun goalLine(value: Double, label: String, lineColor: Int): LineDataSet {
        val entries = listOf(
            Entry(0f, value.toFloat()),
            Entry((periodDays - 1).toFloat(), value.toFloat())
        )
        return LineDataSet(entries, label).apply {
            color = lineColor
            lineWidth = 2f
            enableDashedLine(10f, 8f, 0f)
            setDrawCircles(false)
            setDrawValues(false)
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
}
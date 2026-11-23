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
import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/** Bar graph of spending per category between two dates, with each category's min and max goal. */
class CategoryTotals : AppCompatActivity() {
    private lateinit var lvCategoryTotals: ListView
    private lateinit var tvNoCategoryTotals: TextView
    private lateinit var tvPeriodTotal: TextView
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnCategoryTotalsBack: Button
    private lateinit var chart: CombinedChart

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
        chart = findViewById(R.id.chartCategories)

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
        val db = AppDatabase.getInstance(applicationContext)

        val periodStart = startDate.timeInMillis
        val periodEnd = endDate.timeInMillis

        lifecycleScope.launch {
            val expenses = db.expensesDao().getAll()
            val categories = db.categoryDao().getAll()
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
            Log.i("BXR", "Category totals for period: $totals (total R$periodTotal)")

            val rows = totals.entries
                .sortedByDescending { it.value }
                .map { "${it.key}   R${"%.2f".format(it.value)}" }
            val totalLabel = "Total for period: R${"%.2f".format(periodTotal)}"

            // every defined category is shown on the graph, even with R0 spent,
            // plus any spent category that has no goals (e.g. "Uncategorised")
            val labels = (categories.map { it.cat } + totals.keys).distinct()

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
                showChart(labels, totals, categories)
            }
        }
    }
    /**
     * Draws bars for the amount spent per category and lines for each category's
     * min and max goal. Based on the lecturer's MPAndroidChart multi-line sample and
     * the MPAndroidChart library by PhilJay (https://github.com/PhilJay/MPAndroidChart).
     */
    private fun showChart(
        labels: List<String>,
        totals: Map<String, Double>,
        categories: List<dcCategory>
    ) {
        val textColor = Color.parseColor("#EBE19D")

        chart.setNoDataText("Create a category to see the graph")
        chart.setNoDataTextColor(textColor)

        if (labels.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val barEntries = ArrayList<BarEntry>()
        val minEntries = ArrayList<Entry>()
        val maxEntries = ArrayList<Entry>()

        labels.forEachIndexed { index, name ->
            barEntries.add(BarEntry(index.toFloat(), (totals[name] ?: 0.0).toFloat()))
            // categories without goals (e.g. "Uncategorised") get no goal point
            val category = categories.firstOrNull { it.cat == name }
            if (category != null) {
                minEntries.add(Entry(index.toFloat(), parseAmount(category.catmin).toFloat()))
                maxEntries.add(Entry(index.toFloat(), parseAmount(category.catmax).toFloat()))
            }
        }

        val barSet = BarDataSet(barEntries, "Spent").apply {
            color = textColor
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val combined = CombinedData()
        combined.setData(BarData(barSet).apply { barWidth = 0.5f })

        if (minEntries.isNotEmpty()) {
            val minSet = LineDataSet(minEntries, "Min goal").apply {
                color = Color.parseColor("#4CAF50")
                lineWidth = 2f
                setCircleColor(Color.parseColor("#4CAF50"))
                circleRadius = 4f
                setDrawValues(false)
            }
            val maxSet = LineDataSet(maxEntries, "Max goal").apply {
                color = Color.parseColor("#FF5252")
                lineWidth = 2f
                setCircleColor(Color.parseColor("#FF5252"))
                circleRadius = 4f
                setDrawValues(false)
            }
            combined.setData(LineData(minSet, maxSet))
        }

        chart.data = combined
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false

        chart.axisLeft.apply {
            axisMinimum = 0f
            this.textColor = textColor
            gridColor = Color.parseColor("#555555")
        }
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setLabelCount(labels.size, false)
            // room on both sides so the first and last bars are not cut off
            axisMinimum = -0.5f
            axisMaximum = labels.size - 0.5f
            setDrawGridLines(false)
            this.textColor = textColor
        }
        chart.legend.textColor = textColor

        chart.animateY(800)
        chart.invalidate()
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

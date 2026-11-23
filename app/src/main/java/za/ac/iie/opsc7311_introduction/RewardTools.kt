package za.ac.iie.opsc7311_introduction

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Result of the budget streak calculation.
data class StreakResult(
    val days: Int,
    val dailySafe: Double,
    val todaySpent: Double,
    val hasGoal: Boolean
)

// Reward points earned by one category.
data class CategoryPoints(
    val category: String,
    val tag: String,
    val spent: Double,
    val max: Double,
    val points: Int,
    val note: String
)

data class RewardSummary(
    val streak: StreakResult,
    val categoryPoints: List<CategoryPoints>,
    val streakBonus: Int,
    val total: Int
)

data class TierInfo(
    val name: String,
    val nextName: String?,
    val pointsToNext: Int,
    val progress: Int
)


// Logic for the two own features: the budget streak and reward points.
// Pure Kotlin (no Android views) so it can be unit tested.

object RewardTools {
    const val WINDOW_DAYS = 30
    const val POINTS_UNDER_MAX = 50
    const val RECREATION_MAX_BONUS = 100
    const val STREAK_POINTS_PER_DAY = 5
    private const val MILLIS_PER_DAY = 86_400_000.0

    private val tiers = listOf("Bronze" to 0, "Silver" to 100, "Gold" to 250, "Platinum" to 500)

    fun parseAmount(raw: String): Double {
        return raw.replace("R", "", ignoreCase = true)
            .replace(",", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    fun parseDate(dateStr: String): Date? {
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

    private fun startOfDayMillis(date: Date): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // How many days before today a date falls
    fun daysAgo(date: Date, today: Calendar = Calendar.getInstance()): Int {
        val todayStart = startOfDayMillis(today.time)
        return Math.round((todayStart - startOfDayMillis(date)) / MILLIS_PER_DAY).toInt()
    }


    // Budget streak: consecutive days, counting back from today, where spending stayed
    // within the daily safe amount (monthly max goal / 30). Counting starts on the day of
    // the first recorded expense. Going over on a day breaks the streak.

    fun calculateStreak(
        expenses: List<dcExpenses>,
        maxGoal: Double?,
        today: Calendar = Calendar.getInstance()
    ): StreakResult {
        if (maxGoal == null || maxGoal <= 0.0) {
            return StreakResult(0, 0.0, 0.0, false)
        }
        val dailySafe = maxGoal / WINDOW_DAYS

        // total spent on each day
        val spentByDaysAgo = HashMap<Int, Double>()
        var oldest = -1
        for (expense in expenses) {
            val date = parseDate(expense.date) ?: continue
            val ago = daysAgo(date, today)
            if (ago < 0) continue                       // ignore future-dated expenses
            spentByDaysAgo[ago] = (spentByDaysAgo[ago] ?: 0.0) + parseAmount(expense.expense)
            if (ago > oldest) oldest = ago
        }

        var streak = 0
        var day = 0
        while (day <= oldest) {
            if ((spentByDaysAgo[day] ?: 0.0) > dailySafe + 0.005) break
            streak++
            day++
        }
        return StreakResult(streak, dailySafe, spentByDaysAgo[0] ?: 0.0, true)
    }

    // Reward points for the last 30 days:
    // - +50 for every category at or under its maximum
    // - Recreation categories: up to +100 more, the less is spent the more points
    // - +5 for every day in the budget streak
    // No points are given until at least one expense has been logged in the window.

    fun calculateRewards(
        expenses: List<dcExpenses>,
        categories: List<dcCategory>,
        maxGoal: Double?,
        today: Calendar = Calendar.getInstance()
    ): RewardSummary {
        val streak = calculateStreak(expenses, maxGoal, today)

        // spend per category over the last 30 days
        val spentByCategory = HashMap<String, Double>()
        var anyInWindow = false
        for (expense in expenses) {
            val date = parseDate(expense.date) ?: continue
            val ago = daysAgo(date, today)
            if (ago !in 0 until WINDOW_DAYS) continue
            anyInWindow = true
            spentByCategory[expense.category] =
                (spentByCategory[expense.category] ?: 0.0) + parseAmount(expense.expense)
        }

        val results = categories.map { category ->
            val spent = spentByCategory[category.cat] ?: 0.0
            val max = parseAmount(category.catmax)
            when {
                !anyInWindow ->
                    CategoryPoints(category.cat, category.tag, spent, max, 0, "No expenses logged yet")
                max <= 0.0 ->
                    CategoryPoints(category.cat, category.tag, spent, max, 0, "No maximum set")
                spent > max ->
                    CategoryPoints(category.cat, category.tag, spent, max, 0, "Over the maximum")
                else -> {
                    var points = POINTS_UNDER_MAX
                    var note = "Within maximum (+$POINTS_UNDER_MAX)"
                    if (category.tag == "Recreation") {
                        val bonus = ((1.0 - spent / max) * RECREATION_MAX_BONUS)
                            .toInt()
                            .coerceIn(0, RECREATION_MAX_BONUS)
                        points += bonus
                        note += ", Recreation saving bonus +$bonus"
                    }
                    CategoryPoints(category.cat, category.tag, spent, max, points, note)
                }
            }
        }

        val streakBonus = streak.days * STREAK_POINTS_PER_DAY
        val total = results.sumOf { it.points } + streakBonus
        return RewardSummary(streak, results, streakBonus, total)
    }

    // Which reward tier a points total is in, and how far it is to the next one.
    fun tierInfo(points: Int): TierInfo {
        val index = tiers.indexOfLast { points >= it.second }.coerceAtLeast(0)
        val current = tiers[index]
        val next = tiers.getOrNull(index + 1) ?: return TierInfo(current.first, null, 0, 100)
        val span = next.second - current.second
        val progress = ((points - current.second) * 100 / span).coerceIn(0, 100)
        return TierInfo(current.first, next.first, next.second - points, progress)
    }
}
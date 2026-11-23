package za.ac.iie.opsc7311_introduction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Unit tests for the streak, reward points and tier logic in RewardTools (own features). */
class RewardToolsTest {

    // fixed "today" so the tests give the same result on any day
    private val today: Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 23, 12, 0, 0)
    }

    private fun dateAgo(days: Int): String {
        val cal = today.clone() as Calendar
        cal.add(Calendar.DAY_OF_MONTH, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun expense(daysAgo: Int, amount: String, category: String = "Food") =
        dcExpenses(
            category = category, date = dateAgo(daysAgo), expense = amount,
            item = "test", photo = "", startTime = "08:00", endTime = "09:00"
        )

    private fun category(name: String, max: String, tag: String = "Essential") =
        dcCategory(cat = name, catmax = max, catmin = "0", tag = tag)

    // ---------- parsing ----------
    @Test fun parseAmount_handlesCurrencyAndCommas() =
        assertEquals(1200.5, RewardTools.parseAmount("R1,200.50"), 0.001)

    @Test fun parseAmount_invalidGivesZero() =
        assertEquals(0.0, RewardTools.parseAmount("abc"), 0.001)

    @Test fun parseDate_validAndInvalid() {
        assertNotNull(RewardTools.parseDate("2026-11-23"))
        assertNull(RewardTools.parseDate("not a date"))
    }

    // ---------- streak (goal max 3000 -> R100 per day) ----------
    @Test fun streak_noGoal() {
        val r = RewardTools.calculateStreak(listOf(expense(0, "50")), null, today)
        assertFalse(r.hasGoal)
        assertEquals(0, r.days)
    }

    @Test fun streak_dailySafeIsMaxDividedBy30() =
        assertEquals(100.0, RewardTools.calculateStreak(emptyList(), 3000.0, today).dailySafe, 0.001)

    @Test fun streak_underLimitToday() =
        assertEquals(1, RewardTools.calculateStreak(listOf(expense(0, "50")), 3000.0, today).days)

    @Test fun streak_overLimitTodayResets() {
        val list = listOf(expense(0, "50"), expense(0, "200"))
        assertEquals(0, RewardTools.calculateStreak(list, 3000.0, today).days)
    }

    @Test fun streak_countsConsecutiveDays() {
        val list = listOf(expense(0, "50"), expense(1, "60"), expense(2, "60"))
        assertEquals(3, RewardTools.calculateStreak(list, 3000.0, today).days)
    }

    @Test fun streak_stopsAtDayThatWentOver() {
        val list = listOf(expense(0, "50"), expense(1, "60"), expense(2, "500"))
        assertEquals(2, RewardTools.calculateStreak(list, 3000.0, today).days)
    }

    @Test fun streak_ignoresFutureExpenses() {
        val list = listOf(expense(0, "50"), expense(-3, "9999"))
        assertEquals(1, RewardTools.calculateStreak(list, 3000.0, today).days)
    }

    // ---------- reward points ----------
    @Test fun rewards_noExpensesNoPoints() {
        val r = RewardTools.calculateRewards(emptyList(), listOf(category("Food", "1500")), 3000.0, today)
        assertEquals(0, r.total)
    }

    @Test fun rewards_underMaxGives50() {
        val r = RewardTools.calculateRewards(
            listOf(expense(5, "900")), listOf(category("Food", "1500")), 3000.0, today
        )
        assertEquals(50, r.categoryPoints[0].points)
    }

    @Test fun rewards_recreationBonus() {
        // spent 150 of 600 = 25% -> 50 + 75 = 125
        val r = RewardTools.calculateRewards(
            listOf(expense(5, "150", "Fun")),
            listOf(category("Fun", "600", "Recreation")), 3000.0, today
        )
        assertEquals(125, r.categoryPoints[0].points)
    }

    @Test fun rewards_recreationZeroSpentGivesFullBonus() {
        val r = RewardTools.calculateRewards(
            listOf(expense(5, "10", "Food")),
            listOf(category("Food", "1500"), category("Fun", "600", "Recreation")),
            3000.0, today
        )
        assertEquals(150, r.categoryPoints[1].points)   // 50 + 100
    }

    @Test fun rewards_overMaxGivesZero() {
        val r = RewardTools.calculateRewards(
            listOf(expense(5, "1600")), listOf(category("Food", "1500")), 3000.0, today
        )
        assertEquals(0, r.categoryPoints[0].points)
        assertEquals("Over the maximum", r.categoryPoints[0].note)
    }

    @Test fun rewards_totalIncludesStreakBonus() {
        val r = RewardTools.calculateRewards(
            listOf(expense(0, "50")), listOf(category("Food", "1500")), 3000.0, today
        )
        assertEquals(50 + r.streak.days * 5, r.total)
        assertTrue(r.streakBonus > 0)
    }

    // ---------- tiers ----------
    @Test fun tier_bronzeAtZero() {
        val t = RewardTools.tierInfo(0)
        assertEquals("Bronze", t.name)
        assertEquals("Silver", t.nextName)
        assertEquals(0, t.progress)
    }

    @Test fun tier_silverHalfwayToGold() {
        val t = RewardTools.tierInfo(175)
        assertEquals("Silver", t.name)
        assertEquals("Gold", t.nextName)
        assertEquals(75, t.pointsToNext)
        assertEquals(50, t.progress)
    }

    @Test fun tier_boundaries() {
        assertEquals("Gold", RewardTools.tierInfo(250).name)
        assertEquals("Gold", RewardTools.tierInfo(499).name)
        assertEquals("Platinum", RewardTools.tierInfo(500).name)
    }

    @Test fun tier_platinumHasNoNext() {
        val t = RewardTools.tierInfo(900)
        assertNull(t.nextName)
        assertEquals(100, t.progress)
    }
}
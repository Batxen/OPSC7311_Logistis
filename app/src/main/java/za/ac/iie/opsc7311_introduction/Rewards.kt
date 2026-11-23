package za.ac.iie.opsc7311_introduction

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Shows the budget streak and the reward points.
// All the calculations live in RewardTools.
class Rewards : AppCompatActivity() {
    private lateinit var tvStreak: TextView
    private lateinit var tvStreakInfo: TextView
    private lateinit var tvPoints: TextView
    private lateinit var pbTier: ProgressBar
    private lateinit var tvTierInfo: TextView
    private lateinit var lvBreakdown: ListView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rewards)

        tvStreak = findViewById(R.id.tvStreak)
        tvStreakInfo = findViewById(R.id.tvStreakInfo)
        tvPoints = findViewById(R.id.tvPoints)
        pbTier = findViewById(R.id.pbTier)
        tvTierInfo = findViewById(R.id.tvTierInfo)
        lvBreakdown = findViewById(R.id.lvRewardBreakdown)
        btnBack = findViewById(R.id.btnRewardsBack)

        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadRewards()
    }

    private fun loadRewards() {
        val db = AppDatabase.getInstance(applicationContext)

        lifecycleScope.launch {
            val expenses = db.expensesDao().getAll()
            val categories = db.categoryDao().getAll()
            val goal = db.monthlyGoalDao().getGoal()
            val maxGoal = goal?.maxGoal?.toDoubleOrNull()

            val summary = RewardTools.calculateRewards(expenses, categories, maxGoal)
            Log.i(
                "BXR",
                "Rewards: streak ${summary.streak.days} days, total ${summary.total} points"
            )

            // Streak
            val streak = summary.streak
            if (!streak.hasGoal) {
                tvStreak.text = "No streak yet"
                tvStreakInfo.text = "Save a monthly spending goal to start your streak"
            } else {
                tvStreak.text = "\uD83D\uDD25 ${streak.days} day streak"
                tvStreakInfo.text =
                    "Safe to spend per day: R${"%.2f".format(streak.dailySafe)}\n" +
                            "Spent today: R${"%.2f".format(streak.todaySpent)}"
            }

            // Points + Tier
            val tier = RewardTools.tierInfo(summary.total)
            tvPoints.text = "${summary.total} points  -  ${tier.name}"
            pbTier.progress = tier.progress
            tvTierInfo.text = if (tier.nextName == null) {
                "Top tier reached!"
            } else {
                "${tier.pointsToNext} points to ${tier.nextName}"
            }

            // Breakdown list
            val rows = mutableListOf<String>()
            if (summary.categoryPoints.isEmpty()) {
                rows.add("Create a category to start earning points")
            }
            for (c in summary.categoryPoints) {
                rows.add(
                    "${c.category} (${c.tag}): +${c.points} pts\n" +
                            "${c.note}  |  R${"%.2f".format(c.spent)} of R${"%.2f".format(c.max)}"
                )
            }
            rows.add(
                "Streak bonus: +${summary.streakBonus} pts " +
                        "(${RewardTools.STREAK_POINTS_PER_DAY} per streak day)"
            )
            lvBreakdown.adapter = ArrayAdapter(
                this@Rewards,
                android.R.layout.simple_list_item_1,
                rows
            )
        }
    }
}
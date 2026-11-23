package za.ac.iie.opsc7311_introduction

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class Menu : AppCompatActivity() {
    private lateinit var btnMonthlyGoals: Button
    private lateinit var btnCategoryTotals: Button
    private lateinit var btnExpenseList: Button
    private lateinit var btnAddExpense: Button
    private lateinit var btnMenuCategories: Button
    private lateinit var btnMenuSettings: Button
    private lateinit var btnLogout: Button
    private lateinit var tvMenuStreak: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_menu)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_login -> {
                    Intent(this, MainActivity::class.java).also { startActivity(it) }
                }

                R.id.nav_monthly_goals -> {
                    Intent(this, MonthlyGoals::class.java).also { startActivity(it) }
                }

                R.id.nav_rewards -> {
                    Intent(this, Rewards::class.java).also { startActivity(it) }
                }

                R.id.nav_category_totals -> {
                    Intent(this, CategoryTotals::class.java).also { startActivity(it) }
                }

                R.id.nav_expense_list -> {
                    Intent(this, ExpenseList::class.java).also { startActivity(it) }
                }

                R.id.nav_add_expense -> {
                    Intent(this, AddExpense::class.java).also { startActivity(it) }
                }

                R.id.nav_categories -> {
                    Intent(this, Categories::class.java).also { startActivity(it) }
                }

                R.id.nav_settings -> {
                    Intent(this, Settings::class.java).also { startActivity(it) }
                }

                R.id.nav_logout -> {
                    Session.logout(this)
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }.also { startActivity(it) }
                }
            }

            drawerLayout.closeDrawers()
            true
        }

        btnMonthlyGoals = findViewById(R.id.btnMonthlyGoals)
        btnCategoryTotals = findViewById(R.id.btnCategoryTotals)
        btnExpenseList = findViewById(R.id.btnExpenseList)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnMenuCategories = findViewById(R.id.btnMenuCategories)
        btnMenuSettings = findViewById(R.id.btnMenuSettings)
        btnLogout = findViewById(R.id.btnLogout)
        tvMenuStreak = findViewById(R.id.tvMenuStreak)

        // tapping the streak counter opens the Rewards screen
        tvMenuStreak.setOnClickListener {
            Intent(this, Rewards::class.java).also { startActivity(it) }
        }
        btnMonthlyGoals.setOnClickListener {
            Intent(this, MonthlyGoals::class.java).also { startActivity(it) }
        }
        btnCategoryTotals.setOnClickListener {
            Intent(this, CategoryTotals::class.java).also { startActivity(it) }
        }
        btnExpenseList.setOnClickListener {
            Intent(this, ExpenseList::class.java).also { startActivity(it) }
        }
        btnAddExpense.setOnClickListener {
            Intent(this, AddExpense::class.java).also { startActivity(it) }
        }
        btnMenuCategories.setOnClickListener {
            Intent(this, Categories::class.java).also { startActivity(it) }
        }
        btnMenuSettings.setOnClickListener {
            Intent(this, Settings::class.java).also { startActivity(it) }
        }
        btnLogout.setOnClickListener {
            Session.logout(this)
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }.also { startActivity(it) }
        }
    }

    // refresh the streak counter every time the home screen is shown
    override fun onResume() {
        super.onResume()
        refreshStreak()
    }

    private fun refreshStreak() {
        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            val goal = db.monthlyGoalDao().getGoal()
            val streak = RewardTools.calculateStreak(
                db.expensesDao().getAll(),
                goal?.maxGoal?.toDoubleOrNull()
            )
            tvMenuStreak.text = if (!streak.hasGoal) {
                "Set a monthly goal to start your streak"
            } else {
                "\uD83D\uDD25 ${streak.days} day streak"
            }
        }
    }
}
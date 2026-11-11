package za.ac.iie.opsc7311_introduction

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

class Menu : AppCompatActivity() {
    private lateinit var btnMonthlyGoals: Button
    private lateinit var btnCategoryTotals: Button
    private lateinit var btnExpenseList: Button
    private lateinit var btnAddExpense: Button
    private lateinit var btnMenuCategories: Button
    private lateinit var btnMenuSettings: Button
    private lateinit var btnLogout: Button

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
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }.also { startActivity(it) }
        }
    }
}

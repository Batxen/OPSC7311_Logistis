package za.ac.iie.opsc7311_introduction

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Variables declared here
    private lateinit var btnnext: Button
    private lateinit var etusername: EditText
    private lateinit var etpassword: EditText

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        btnnext = findViewById(R.id.btnNext)
        etusername = findViewById(R.id.etUsername)
        etpassword = findViewById(R.id.etPassword)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        // Fixed ActionBarDrawerToggle constructor call
        toggle = ActionBarDrawerToggle(
            this@MainActivity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // The back arrow

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_login -> {
                    Toast.makeText(applicationContext, "Clicked Login", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_settings -> {
                    Toast.makeText(applicationContext, "Clicked Settings", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawers()
            true
        } // <-- Fixed: Closed the navigation listener block properly here

        val tools = clsTools()
        val dbName = "mydb"

        // Fixed database builder call with correct reference to AppDatabase
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, dbName
        ).fallbackToDestructiveMigration()
            .build()
        val userDao = db.userDao()

        btnnext.setOnClickListener {
            val opt = tools.validateField(etusername) && tools.validateField(etpassword)

            if (!opt) {
                Toast.makeText(
                    this,
                    "Cannot use empty strings or please register!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                lifecycleScope.launch {
                    val dbuser = userDao.getByName(etusername.text.toString())

                    if (dbuser == null) {
                        val newuser = dcUser(
                            dbusername = etusername.text.toString(),
                            dbpassword = tools.hashPassword(etpassword.text.toString())
                        )
                        userDao.insert(newuser)
                        Toast.makeText(
                            this@MainActivity,
                            "User ${etusername.text.toString()} registered in database. Please log in again",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (tools.hashPassword(etpassword.text.toString()) == dbuser.dbpassword) {
                            Toast.makeText(
                                this@MainActivity,
                                "User ${etusername.text.toString()} logged in",
                                Toast.LENGTH_SHORT
                            ).show()
                            Intent(this@MainActivity, Menu::class.java).also { startActivity(it) }
                            etusername.text.clear()
                            etpassword.text.clear()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Incorrect password",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
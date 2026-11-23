package za.ac.iie.opsc7311_introduction

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private lateinit var btnnext: Button
    private lateinit var btnregister: Button
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
        btnregister = findViewById(R.id.btnRegister)
        etusername = findViewById(R.id.etUsername)
        etpassword = findViewById(R.id.etPassword)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        toggle = ActionBarDrawerToggle(
            this@MainActivity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Nobody is logged in yet, so the drawer only reminds the user to log in
        navView.setNavigationItemSelectedListener { item ->
            if (item.itemId != R.id.nav_login) {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawers()
            true
        }

        val tools = clsTools()
        val db = AppDatabase.getInstance(applicationContext)
        val userDao = db.userDao()

        // Log In
        btnnext.setOnClickListener {
            val fieldsOk = tools.validateField(etusername) and tools.validateField(etpassword)
            if (!fieldsOk) {
                Toast.makeText(this, "Please enter your username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = etusername.text.toString().trim()
            lifecycleScope.launch {
                val dbuser = userDao.getByName(username)
                when {
                    dbuser == null -> {
                        Log.w("BXR", "Login failed: no account for $username")
                        Toast.makeText(
                            this@MainActivity,
                            "No account found - please register first",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    tools.hashPassword(etpassword.text.toString()) != dbuser.dbpassword -> {
                        Log.w("BXR", "Login failed: wrong password for $username")
                        Toast.makeText(this@MainActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.i("BXR", "Login success: $username")
                        Toast.makeText(this@MainActivity, "Welcome, $username", Toast.LENGTH_SHORT).show()
                        Intent(this@MainActivity, Menu::class.java).also { startActivity(it) }
                        etusername.text.clear()
                        etpassword.text.clear()
                    }
                }
            }
        }

        // Register
        btnregister.setOnClickListener {
            val fieldsOk = tools.validateField(etusername) and tools.validateField(etpassword)
            if (!fieldsOk) {
                Toast.makeText(this, "Please choose a username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = etusername.text.toString().trim()
            lifecycleScope.launch {
                if (userDao.getByName(username) != null) {
                    Log.w("BXR", "Register failed: $username already exists")
                    etusername.error = "That username is already taken"
                    return@launch
                }
                userDao.insert(
                    dcUser(
                        dbusername = username,
                        dbpassword = tools.hashPassword(etpassword.text.toString())
                    )
                )
                Log.i("BXR", "User registered: $username")
                Toast.makeText(
                    this@MainActivity,
                    "Registered! You can now log in",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
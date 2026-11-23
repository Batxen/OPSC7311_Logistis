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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

/** Login and register screen. Accounts live in Firebase, and logging in downloads the user's data into Room. */
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

        // LOG IN
        btnnext.setOnClickListener {
            val fieldsOk = tools.validateField(etusername) and tools.validateField(etpassword)
            if (!fieldsOk) {
                Toast.makeText(this, "Please enter your username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = etusername.text.toString().trim().lowercase()
            val password = etpassword.text.toString()

            setBusy(true)
            Log.i("BXR", "Login button pressed for user: $username")
            lifecycleScope.launch {
                val result = FirebaseSync.loginUser(username, password)
                Log.i("BXR", "Login result for $username: $result")
                when (result) {
                    AuthResult.OK -> {
                        // download this user's categories, goals and expenses into the phone
                        val loaded = FirebaseSync.pullAll(applicationContext, username)
                        if (loaded) {
                            Log.i("BXR", "Session started for $username, opening menu")
                            Session.login(this@MainActivity, username)
                            Toast.makeText(this@MainActivity, "Welcome, $username", Toast.LENGTH_SHORT).show()
                            Intent(this@MainActivity, Menu::class.java).also { startActivity(it) }
                            etusername.text.clear()
                            etpassword.text.clear()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Could not load your data - please try again",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    AuthResult.NOT_FOUND -> Toast.makeText(
                        this@MainActivity, "No account found - please register first", Toast.LENGTH_SHORT
                    ).show()
                    AuthResult.WRONG_PASSWORD -> Toast.makeText(
                        this@MainActivity, "Incorrect password", Toast.LENGTH_SHORT
                    ).show()
                    else -> Toast.makeText(
                        this@MainActivity,
                        "Could not reach the server - check your internet connection",
                        Toast.LENGTH_LONG
                    ).show()
                }
                setBusy(false)
            }
        }

        // REGISTER
        btnregister.setOnClickListener {
            val fieldsOk = tools.validateField(etusername) and tools.validateField(etpassword)
            if (!fieldsOk) {
                Toast.makeText(this, "Please choose a username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = etusername.text.toString().trim().lowercase()
            val password = etpassword.text.toString()

            if (!FirebaseSync.isValidUsername(username)) {
                Log.w("BXR", "Register rejected: invalid username '$username'")
                etusername.error = "3-20 characters: letters, numbers, _ or -"
                return@setOnClickListener
            }
            if (password.length < 4) {
                etpassword.error = "Use at least 4 characters"
                return@setOnClickListener
            }

            setBusy(true)
            Log.i("BXR", "Register button pressed for user: $username")
            lifecycleScope.launch {
                val result = FirebaseSync.registerUser(username, password)
                Log.i("BXR", "Register result for $username: $result")
                setBusy(false)
                when (result) {
                    AuthResult.OK -> Toast.makeText(
                        this@MainActivity, "Registered! You can now log in", Toast.LENGTH_SHORT
                    ).show()
                    AuthResult.TAKEN -> etusername.error = "That username is already taken"
                    else -> Toast.makeText(
                        this@MainActivity,
                        "Could not reach the server - check your internet connection",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Disable the buttons while we wait for the server so it cannot be tapped twice.
    private fun setBusy(busy: Boolean) {
        btnnext.isEnabled = !busy
        btnregister.isEnabled = !busy
    }
}
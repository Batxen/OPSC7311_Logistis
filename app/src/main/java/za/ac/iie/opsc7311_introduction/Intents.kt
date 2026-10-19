package za.ac.iie.opsc7311_introduction

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Settings : AppCompatActivity() {
    //Declare local variables and defer initialisation
    private lateinit var goback: Button
    private lateinit var categories: Button
    private lateinit var monthlyGoals: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        goback = findViewById(R.id.btnSettingsBack)
        categories = findViewById(R.id.btnCategory)
        monthlyGoals = findViewById(R.id.btnSettingsGoals)

        categories.setOnClickListener() {
            Intent(this, Categories::class.java).also { startActivity(it) }
        }//categories click
        monthlyGoals.setOnClickListener() {
            Intent(this, MonthlyGoals::class.java).also { startActivity(it) }
        }
        goback.setOnClickListener() { finish() }
    }
}
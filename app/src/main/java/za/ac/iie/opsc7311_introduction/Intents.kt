package za.ac.iie.opsc7311_introduction

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java

class Settings : AppCompatActivity() {
    //Declare local variables and defer initialisation
    private lateinit var goback: Button
    private lateinit var categories: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        goback = findViewById(R.id.btnSettingsBack)
        categories = findViewById(R.id.btnCategory)

        categories.setOnClickListener() {
            Intent(this, Categories::class.java).also { startActivity(it) }
        }//categories click
        goback.setOnClickListener() { finish() }
    }
}
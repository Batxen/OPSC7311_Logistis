package za.ac.iie.opsc7311_introduction

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch
import za.ac.iie.opsc7311_introduction.databinding.ActivityCategoriesBinding

class Categories : AppCompatActivity() {
    private lateinit var binding: ActivityCategoriesBinding

    //declare local variables
    private lateinit var etcategory: EditText
    private lateinit var etmax: EditText
    private lateinit var etmin: EditText
    private lateinit var lvCategories: ListView
    private lateinit var btnsubmit: Button
    private lateinit var btnback: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tools = clsTools()
        val room = dbRoom()

        //initialise local variables by linking it to the canvas variables
        //using viewbinding instead of etCategory =  findViewById(R.id.etCategory)
        etcategory = binding.etCategory
        etmin = binding.etMin
        etmax = binding.etMax
        btnback = binding.btnBack
        btnsubmit = binding.btnSave
        lvCategories = binding.lvCategories
        //build the SQLite database
        val dbName = "mydb"
        val db = Room.databaseBuilder(
            applicationContext,
            dbRoom.AppDatabase::class.java, dbName
        ).fallbackToDestructiveMigration(true)
            .build()  //build the database =

        val catlist: MutableList<String> = mutableListOf()
        val categoryDao = db.categoryDao()
        //  val categories = dbRoom.dcCategory(cat = "cat", catmax = "catmax", catmin = "catmin")

        //load captured categories from the database, and display in listview, if it exists
        lifecycleScope.launch {

            val lstcat = categoryDao.getAll()

            for (item in lstcat) {
                val xx = item.cat + "   R" + item.catmax + "   R" + item.catmin
                catlist.add(xx)
                Log.i("BXR", "Category list: $catlist")
            }
            val categoryAdapter = ArrayAdapter(
                this@Categories,
                android.R.layout.simple_list_item_1, catlist
            )//ArrayAdapter
            lvCategories.adapter = categoryAdapter
        }//coroutine


        btnsubmit.setOnClickListener {
            // check every field (and = all errors show); min/max must be valid amounts
            val valid = tools.validateField(etcategory) and
                    tools.validateAmount(etmin) and
                    tools.validateAmount(etmax)

            if (!valid) {
                Toast.makeText(this, "Please fill in all fields with valid values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = etcategory.text.toString().trim()
            val newcategory = dbRoom.dcCategory(
                cat = name,
                catmin = etmin.text.toString(),
                catmax = etmax.text.toString()
            )

            lifecycleScope.launch {
                // reject duplicate category names
                if (categoryDao.getAll().any { it.cat.equals(name, ignoreCase = true) }) {
                    etcategory.error = "That category already exists"
                    return@launch
                }

                categoryDao.insert(newcategory)
                Log.i("BXR", "Category saved: $name")

                // reload the list only after the insert has finished
                catlist.clear()
                for (item in categoryDao.getAll()) {
                    catlist.add(item.cat + "   R" + item.catmax + "   R" + item.catmin)
                }
                (lvCategories.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

                Toast.makeText(this@Categories, "Category saved", Toast.LENGTH_SHORT).show()
                etcategory.text.clear()
                etmin.text.clear()
                etmax.text.clear()
            }
        }//btnsubmit

        btnback.setOnClickListener { finish() }


    }//oncreate
// 1. Clear the backing list
    //   myArrayList.clear()

// 2. Notify the adapter so the UI updates
    //   myAdapter.notifyDataSetChanged()

}//class
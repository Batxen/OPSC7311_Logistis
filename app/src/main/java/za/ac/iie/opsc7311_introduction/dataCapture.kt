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
            //test if category is empty
            var opt = true
            opt = tools.validateField(etcategory)
            opt = tools.validateField(etmin)
            opt = tools.validateField(etmax)


            if (!opt) {
                Toast.makeText(this, "None of the fields may be empty", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Category saved", Toast.LENGTH_SHORT).show()
                //save category, min, max to database

                val newcategory = dbRoom.dcCategory(
                    cat = etcategory.text.toString(),
                    catmin = etmin.text.toString(),
                    catmax = etmax.text.toString()
                )
                //add category to listview
                //clear edittext
                //if etcategory exist in database, then reject entry with waning
                lifecycleScope.launch {
                    val categoryDao = db.categoryDao()
                    categoryDao.insert(newcategory)
                }//coroutine
                etcategory.text.clear()
                etmin.text.clear()
                etmax.text.clear()
            }//else if

            val categoryAdapter = ArrayAdapter(
                this@Categories,
                android.R.layout.simple_list_item_1, catlist
            )//ArrayAdapter

            //refresh listview
            categoryAdapter.clear()

            lifecycleScope.launch {

                val lstcat = categoryDao.getAll()

                for (item in lstcat) {
                    val xx = item.cat + "   R" + item.catmax + "   R" + item.catmin
                    catlist.add(xx)
                }

                lvCategories.adapter = categoryAdapter
            }//coroutine

        }//btnsubmit

        btnback.setOnClickListener { finish() }


    }//oncreate
// 1. Clear the backing list
    //   myArrayList.clear()

// 2. Notify the adapter so the UI updates
    //   myAdapter.notifyDataSetChanged()

}//class
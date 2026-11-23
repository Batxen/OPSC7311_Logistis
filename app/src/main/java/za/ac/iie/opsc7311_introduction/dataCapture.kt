package za.ac.iie.opsc7311_introduction

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.opsc7311_introduction.databinding.ActivityCategoriesBinding

class Categories : AppCompatActivity() {
    private lateinit var binding: ActivityCategoriesBinding

    //declare local variables
    private lateinit var etcategory: EditText
    private lateinit var etmax: EditText
    private lateinit var etmin: EditText
    private lateinit var spTag: Spinner
    private lateinit var lvCategories: ListView
    private lateinit var btnsubmit: Button
    private lateinit var btnback: Button

    // tags a category can have; "Recreation" is used by the reward points feature
    private val tags = listOf("Essential", "Recreation", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tools = clsTools()

        etcategory = binding.etCategory
        etmin = binding.etMin
        etmax = binding.etMax
        spTag = binding.spCategoryTag
        btnback = binding.btnBack
        btnsubmit = binding.btnSave
        lvCategories = binding.lvCategories

        spTag.adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, tags
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getView(position, convertView, parent) as TextView).apply {
                    setTextColor(Color.BLACK)
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setTextColor(Color.BLACK)
                    setBackgroundColor(Color.parseColor("#EBE19D"))
                    setPadding(24, 24, 24, 24)
                }
            }
        }

        val db = AppDatabase.getInstance(applicationContext)
        val categoryDao = db.categoryDao()

        val catlist: MutableList<String> = mutableListOf()
        val categoryAdapter = ArrayAdapter(
            this@Categories,
            android.R.layout.simple_list_item_1, catlist
        )
        lvCategories.adapter = categoryAdapter

        // load captured categories from the database and display them in the listview
        lifecycleScope.launch {
            for (item in categoryDao.getAll()) {
                catlist.add(describe(item))
            }
            Log.i("BXR", "Category list: $catlist")
            categoryAdapter.notifyDataSetChanged()
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
            val newcategory = dcCategory(
                cat = name,
                catmin = etmin.text.toString(),
                catmax = etmax.text.toString(),
                tag = spTag.selectedItem as String
            )

            lifecycleScope.launch {
                // reject duplicate category names
                if (categoryDao.getAll().any { it.cat.equals(name, ignoreCase = true) }) {
                    etcategory.error = "That category already exists"
                    return@launch
                }

                val newId = categoryDao.insert(newcategory)
                FirebaseSync.pushCategory(
                    Session.username(applicationContext),
                    newcategory.copy(id = newId.toInt())
                )
                Log.i("BXR", "Category saved: $name (${newcategory.tag})")

                // reload the list only after the insert has finished
                catlist.clear()
                for (item in categoryDao.getAll()) {
                    catlist.add(describe(item))
                }
                categoryAdapter.notifyDataSetChanged()

                Toast.makeText(this@Categories, "Category saved", Toast.LENGTH_SHORT).show()
                etcategory.text.clear()
                etmin.text.clear()
                etmax.text.clear()
            }
        }//btnsubmit

        btnback.setOnClickListener { finish() }
    }//oncreate

    private fun describe(item: dcCategory): String {
        return "${item.cat} (${item.tag})   Min R${item.catmin}   Max R${item.catmax}"
    }
}//class
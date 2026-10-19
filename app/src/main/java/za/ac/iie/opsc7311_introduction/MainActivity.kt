package za.ac.iie.opsc7311_introduction
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch


    class MainActivity : AppCompatActivity() {
        //variables declared here
//late initialisation of variables
        private lateinit var btnnext: Button
        private lateinit var etusername: EditText
        private lateinit var etpassword: EditText

        override fun onCreate(savedInstanceState: Bundle?) {

            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)

            btnnext = findViewById(R.id.btnNext)
            etusername = findViewById(R.id.etUsername)
            etpassword = findViewById(R.id.etPassword)

            var opt: Boolean

            //create instance of clsTools
            val tools =
                clsTools()  //use class and functions declared outside main body - see clsTools class
            val dbName = "mydb"

            val db = Room.databaseBuilder(
                applicationContext,
                dbRoom.AppDatabase::class.java, dbName
            ).fallbackToDestructiveMigration(true)
                .build()  //build the database
            val userDao = db.userDao()

            btnnext.setOnClickListener {

                opt = tools.validateField(etusername) and tools.validateField(etpassword)

                if (!opt) {
                    Toast.makeText(
                        this,
                        "Cannot use empty strings or please register!",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    //opt true, ie username and password not empty
                    //check if user exist in database

                    lifecycleScope.launch {
                        //lifecycleScope is not part of KSP -
                        val dbuser =
                            userDao.getByName(etusername.text.toString()) // search for specific name in database

                        if (dbuser?.dbusername == null) {
                            //  Log.i("BXR","User ${etusername.text.toString()} does not exist in database")
                            //register new user
                            val newuser = dbRoom.dcUser(
                                dbusername = etusername.text.toString(),
                                dbpassword = tools.hashPassword(etpassword.text.toString())
                            )
                            userDao.insert(newuser)
                            //  Log.i("BXR", "User ${etusername.text.toString()} registered in database")
                            Toast.makeText(
                                this@MainActivity,
                                "User ${etusername.text.toString()} registered in database. Please log in again",
                                Toast.LENGTH_SHORT
                            ).show()

                        }//if
                        else {
                            //if name is found in database then user cannot be registered but password can be updated
                            //  Log.i("BXR", "User ${dbuser.dbusername} exists in database")
                            //verify password for this user and give access to rest of system if verification positive
                            if(tools.hashPassword(etpassword.text.toString()) == dbuser.dbpassword){

                                Toast.makeText(
                                    this@MainActivity,
                                    "User ${etusername.text.toString()} logged in",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Intent(this@MainActivity, Menu::class.java).also { startActivity(it) }
                                etusername.text.clear()
                                etpassword.text.clear()
                            }else{
                                Toast.makeText(
                                    this@MainActivity,
                                    "Incorrect password",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }//coroutine
                }//if opt true
            }//btnnext listener

        }//fun onCreate
    }//class main
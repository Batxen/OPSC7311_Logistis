package za.ac.iie.opsc7311_introduction

import android.widget.EditText
import java.security.MessageDigest

class clsTools {

    public fun validateField(varText: EditText): Boolean {
        if (varText.text.length == 0) {
            varText.error = "This field cannot be empty!"
            return false
        } else {
            return true
        }
    }

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") {str, it -> str + "%02x".format(it)}
    }

}
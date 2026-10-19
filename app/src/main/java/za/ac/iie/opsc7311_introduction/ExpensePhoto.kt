package za.ac.iie.opsc7311_introduction

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ExpensePhoto : AppCompatActivity() {
    companion object {
        const val EXTRA_PHOTO = "photo"
        const val EXTRA_ITEM = "item"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense_photo)

        val imgExpensePhoto = findViewById<ImageView>(R.id.imgExpensePhoto)
        val tvPhotoTitle = findViewById<TextView>(R.id.tvPhotoTitle)
        val btnPhotoBack = findViewById<Button>(R.id.btnPhotoBack)

        val itemName = intent.getStringExtra(EXTRA_ITEM).orEmpty()
        val photo = intent.getStringExtra(EXTRA_PHOTO).orEmpty()
        tvPhotoTitle.text = if (itemName.isBlank()) "Expense photo" else itemName

        btnPhotoBack.setOnClickListener { finish() }

        if (!showPhoto(imgExpensePhoto, photo)) {
            Toast.makeText(this, "Photo could not be opened", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showPhoto(imageView: ImageView, photo: String): Boolean {
        if (photo.isBlank()) {
            return false
        }

        try {
            val uri = Uri.parse(photo)
            if (uri.scheme == "content" || uri.scheme == "file") {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        return true
                    }
                }
            }
        } catch (_: Exception) {
        }

        val file = resolvePhotoFile(photo)
        if (file != null) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return true
            }
        }
        return false
    }

    private fun resolvePhotoFile(photo: String): File? {
        val candidates = listOfNotNull(
            File(photo),
            File(filesDir, photo),
            File(File(filesDir, "photos"), photo),
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { File(it, photo) },
            getExternalFilesDir(null)?.let { File(it, photo) }
        )
        return candidates.firstOrNull { it.exists() }
    }
}

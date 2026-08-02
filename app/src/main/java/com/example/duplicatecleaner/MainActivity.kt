package com.example.duplicatecleaner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private val duplicateFiles = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
        }

        val title = TextView(this).apply {
            text = "Duplicate Image Cleaner\n(MD5 Fingerprint Scan)"
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }

        val btnScan = Button(this).apply {
            text = "🔍 Scan Storage"
        }

        val btnDelete = Button(this).apply {
            text = "🗑️ Delete Duplicates"
        }

        layout.addView(title)
        layout.addView(btnScan)
        layout.addView(btnDelete)
        setContentView(layout)

        btnScan.setOnClickListener {
            if (checkPermissions()) {
                scanDuplicateImages()
            } else {
                requestPermissions()
            }
        }

        btnDelete.setOnClickListener {
            deleteSelectedDuplicates()
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            100
        )
    }

    private fun scanDuplicateImages() {
        duplicateFiles.clear()
        val hashMap = HashMap<String, String>()

        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )

        cursor?.use {
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val filePath = it.getString(dataColumn)
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0) {
                        val hash = getFileHash(file)
                        if (hash.isNotEmpty()) {
                            if (hashMap.containsKey(hash)) {
                                duplicateFiles.add(file)
                            } else {
                                hashMap[hash] = filePath
                            }
                        }
                    }
                }
            }
        }
        Toast.makeText(this, "Scan Finished: Found ${duplicateFiles.size} Duplicates", Toast.LENGTH_LONG).show()
    }

    private fun getFileHash(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val inputStream = file.inputStream()
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            inputStream.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun deleteSelectedDuplicates() {
        if (duplicateFiles.isEmpty()) {
            Toast.makeText(this, "No duplicates found to delete!", Toast.LENGTH_SHORT).show()
            return
        }

        var deletedCount = 0
        for (file in duplicateFiles) {
            if (file.exists() && file.delete()) {
                deletedCount++
            }
        }
        Toast.makeText(this, "$deletedCount Duplicates Deleted Successfully!", Toast.LENGTH_LONG).show()
        duplicateFiles.clear()
    }
}
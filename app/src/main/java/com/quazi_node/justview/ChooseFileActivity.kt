package com.quazi_node.justview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class ChooseFileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_file)

        requestChooseFile()
    }

    private fun requestChooseFile() {
        Log.i("action", "requestChooseFile")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_REQUEST_PERMISSIONS)
        } else {
            chooseFile()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        Log.i("action", "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_REQUEST_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseFile()
                } else {
                    Log.w("Permission", "READ permission not granted")
                }
                return
            }
        }
    }

    private fun chooseFile() {
        Log.i("action", "chooseFile")

        val uri = getStartingPath()

        val chooseFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT, uri).apply {
            data = uri
            type = "video/*"

            // Only pick openable and local files. Theoretically we could pull files from google drive
            // or other applications that have networked files, but that's unnecessary for this case.
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }

        chooseFileActivity.launch(Intent.createChooser(chooseFileIntent, "Select a file"))
    }

    private fun getStartingPath(): Uri {
        var uriRaw = intent.getStringExtra("uri")
        if (uriRaw.isNullOrEmpty())
        {
            uriRaw = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path
        }

        return Uri.parse(uriRaw)
    }

    private val chooseFileActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.i("action", "chooseFileActivity result")
        setResult(RESULT_OK, result?.data)
        Log.i("action", "chooseFileActivity result finished")
        finish()
    }

    companion object {
        const val READ_EXTERNAL_REQUEST_PERMISSIONS : Int = 1
    }
}
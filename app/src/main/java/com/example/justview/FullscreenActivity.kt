package com.example.justview

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File


class FullscreenActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private var currentTrack: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.hide();

        videoView = findViewById<VideoView>(R.id.videoView)

        videoView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                nextTrack(currentTrack!!)
            }
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onSwipeRight() {
                super.onSwipeRight()
                prevTrack(currentTrack!!)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_READ_EXTERNAL_REQUEST)
        } else {
            showChooseFileDialog()
        }
    }

    private val MY_READ_EXTERNAL_REQUEST : Int = 1

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_READ_EXTERNAL_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showChooseFileDialog()
                } else {
                    Log.w("Permission", "READ permission not granted")
                }
                return
            }
        }
    }

    val REQUEST_GET_FILE = 111

    private fun showChooseFileDialog() {
        val intent = Intent()
            .setType("video/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_GET_FILE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_GET_FILE && resultCode == RESULT_OK) {
            val pathHelper = URIPathHelper()
            val selectedFile = pathHelper.getPath(this, data?.data!!)
            playPath(selectedFile!!)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun playPath(path: String) {
        try {
            Log.d("Play", path)
            currentTrack = path
            val uri = Uri.fromFile(File(path))
            videoView.setVideoURI(uri)

            videoView.requestFocus()
            videoView.start()

            videoView.setOnCompletionListener { nextTrack(path) }

            videoView.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
                nextTrack(path)
                false
            })
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun nextTrack(path: String) {
        val currentFile = File(path)
        val currentDir = currentFile.parent
        val files = getFiles(currentDir!!)
        var index = files?.indexOf(currentFile.name)!!
        index++
        if (index >= files.count()) {
            index = 0
        } else if (index < 0) {
            index = files.count() - 1
        }

        playPath(currentDir + "/" + files[index]!!)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun prevTrack(path: String) {
        val currentFile = File(path)
        val currentDir = currentFile.parent
        val files = getFiles(currentDir!!)
        var index = files?.indexOf(currentFile.name)!!
        index--
        if (index >= files.count()) {
            index = 0
        } else if (index < 0) {
            index = files.count() - 1
        }

        playPath(currentDir + "/" + files[index]!!)
    }

    private fun getFiles(directoryPath: String): Array<String?>? {
        val directory = File(directoryPath)
        val files: Array<File> = directory.listFiles()
        val result = arrayOfNulls<String>(files.size)
        for (i in files.indices) {
            result[i] = files[i].getName()
        }
        return result
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                videoView.stopPlayback()
                showChooseFileDialog()
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_SPACE) {
                if (videoView.isPlaying) {
                    videoView.pause()
                } else {
                    videoView.resume()
                }
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        if (currentTrack != null) {
            videoView.start()
        }
    }

    override fun onStop() {
        if (videoView.isPlaying) {
            videoView.pause()
        }

        super.onStop()
    }
}
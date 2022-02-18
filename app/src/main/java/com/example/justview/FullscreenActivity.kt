package com.example.justview

import android.Manifest
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
    private var currentPosition: Int = 0

    @RequiresApi(Build.VERSION_CODES.M)
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
                nextTrack(currentTrack)
            }
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onSwipeRight() {
                super.onSwipeRight()
                prevTrack(currentTrack)
            }
        })

        videoView.setOnCompletionListener {
            nextTrack(currentTrack)
        }

        videoView.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
            nextTrack(currentTrack)
            false
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
        Log.d("Play", path)
        currentTrack = path
        currentPosition = 0
        startPlayVideo()
    }

    private fun startPlayVideo() {
        if (!currentTrack.isNullOrBlank()) {
            try {
                videoView.requestFocus()
                val uri = Uri.fromFile(File(currentTrack!!))
                videoView.setVideoURI(uri)
                videoView.seekTo(currentPosition)
                videoView.start()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun nextTrack(path: String?) {
        if (path.isNullOrEmpty()) return

        val currentFile = File(path)
        val currentDir = currentFile.parent
        val files = getFiles(currentDir!!)
        var index = files.indexOf(currentFile.name)
        index++
        if (index >= files.count()) {
            index = 0
        } else if (index < 0) {
            index = files.count() - 1
        }

        playPath(currentDir + "/" + files[index]!!)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun prevTrack(path: String?) {
        if (path.isNullOrEmpty()) return

        val currentFile = File(path)
        val currentDir = currentFile.parent
        val files = getFiles(currentDir!!)
        var index = files.indexOf(currentFile.name)
        index--
        if (index >= files.count()) {
            index = 0
        } else if (index < 0) {
            index = files.count() - 1
        }

        playPath(currentDir + "/" + files[index]!!)
    }

    private fun getFiles(directoryPath: String): Array<String?> {
        val directory = File(directoryPath)
        val files: Array<File> = directory.listFiles()
        val result = arrayOfNulls<String>(files.size)
        for (i in files.indices) {
            result[i] = files[i].name
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
//            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
//                videoView.stopPlayback()
//                showChooseFileDialog()
//                return true
//            }
            if (event.keyCode == KeyEvent.KEYCODE_SPACE) {
                if (videoView.isPlaying) {
                    pauseVideo()
                } else {
                    startPlayVideo()
                }
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                event.keyCode == KeyEvent.KEYCODE_NAVIGATE_NEXT ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                nextTrack(currentTrack)
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
                event.keyCode == KeyEvent.KEYCODE_NAVIGATE_PREVIOUS ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                prevTrack(currentTrack)
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onRestart() {
        super.onRestart()
        startPlayVideo()
    }

    override fun onBackPressed() {
        pauseVideo()
        showChooseFileDialog()
    }

    override fun onStop() {
        pauseVideo()
        super.onStop()
    }

    private fun pauseVideo() {
        if (videoView.isPlaying) {
            currentPosition = videoView.currentPosition
            videoView.pause()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        pauseVideo()
        showChooseFileDialog()
    }
}
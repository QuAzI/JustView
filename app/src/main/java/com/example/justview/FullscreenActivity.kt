package com.example.justview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileFilter


class FullscreenActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private var currentTrack: String? = null
    private var currentPosition: Int = 0
    private var prevTrack: String? = null

    private var flippingDirection: Int = 1

    private val STATE_STOP : Int = 0
    private val STATE_PLAY : Int = 1

    private var lastState: Int = STATE_STOP
        set(value) {
            Log.i("action", "lastState = $value")
            field = value
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.hide()

        videoView = findViewById<VideoView>(R.id.videoView)

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)

        videoView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                if (!nextTrack(currentTrack)) {
                    chooseFile()
                }
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onSwipeRight() {
                super.onSwipeRight()
                if (!prevTrack(currentTrack)) {
                    chooseFile()
                }
            }

            override fun onClick() {
                Log.i("action", "onClick")
                if (!videoView.isPlaying) {
                    videoView.start()
                }

                Log.i("action", "onClick: showing ${mediaController.isShowing}")
                videoView.setMediaController(null)
            }

            override fun onLongClick() {
                Log.i("action", "onLongClick")
                videoView.setMediaController(mediaController)
            }
        })

        videoView.setOnPreparedListener(MediaPlayer.OnPreparedListener { mediaPlayer ->
            Log.i("action", "setOnPreparedListener")
            prevTrack = currentTrack
        })

        videoView.setOnCompletionListener {
            switchToNextTrackInTheDirection()
        }

        videoView.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
            Log.i("action", "File playback error: ${this.currentTrack}")
            if (!switchToNextTrackInTheDirection()) {
                chooseFile()
            }
            true
        })
    }

    private fun switchToNextTrackInTheDirection(): Boolean {
        if (flippingDirection >= 0) {
            return nextTrack(currentTrack)
        } else {
            return prevTrack(currentTrack)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_READ_EXTERNAL_REQUEST)
        } else {
            chooseFile()
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
                    chooseFile()
                } else {
                    Log.w("Permission", "READ permission not granted")
                }
                return
            }
        }
    }

    val REQUEST_GET_FILE = 111

    private var chooseFileIntent: Intent? = null

    private fun chooseFile() {
        Log.i("action", "showChooseFileDialog")

        if (chooseFileIntent != null) {
            Log.i("action", "showChooseFileDialog already opened")
            return
        }

        pauseVideo()

        val uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path)

        chooseFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT, uri).apply {
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

        startActivityForResult(Intent.createChooser(chooseFileIntent, "Select a file"), REQUEST_GET_FILE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_GET_FILE) {
            chooseFileIntent = null

            Log.i("action", "showChooseFileDialog finished with $resultCode")
            if (resultCode == RESULT_OK) {
                val pathHelper = URIPathHelper()
                val selectedFile = pathHelper.getPath(this, data?.data!!)
                prevTrack = selectedFile
                playPath(selectedFile!!)
            } else {
                    startPlayVideo()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun playPath(path: String) {
        Log.i("action", "Play: $path")
        stopPlayVideo()
        currentTrack = path
        currentPosition = 0
        startPlayVideo()
    }

    private fun startPlayVideo() {
        Log.i("action", "startPlayVideo: from $currentPosition play '$currentTrack'")
        if (chooseFileIntent != null) {
            Log.i("action", "startPlayVideo disabled as ChooseFile opened")
            return
        }

        if (!currentTrack.isNullOrBlank()) {
            try {
                videoView.requestFocus()
                val uri = Uri.fromFile(File(currentTrack!!))
                videoView.setVideoURI(uri)
                if (videoView.currentPosition != currentPosition) {
                    videoView.seekTo(currentPosition)
                }
                videoView.start()
                lastState = STATE_PLAY
                videoView.setMediaController(null)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        supportActionBar?.hide()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun nextTrack(path: String?): Boolean {
        Log.i("action", "nextTrack $path")
        return playNextTrackInTheDirection(path, 1)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun prevTrack(path: String?): Boolean {
        Log.i("action", "prevTrack $path")
        return playNextTrackInTheDirection(path, -1)
    }

    private fun playNextTrackInTheDirection(path: String?, direction: Int): Boolean {
        if (path.isNullOrEmpty()) return false

        this.flippingDirection = direction
        val currentFile = File(path)
        val currentDir = currentFile.parent
        val files = getFiles(currentDir!!)
        if (files.size > 1) {
            var index = files.indexOf(currentFile.name)
            index += flippingDirection
            if (index >= files.count()) {
                index = 0
            } else if (index < 0) {
                index = files.count() - 1
            }

            val newFilePath = currentDir + "/" + files[index]!!
            if (prevTrack != newFilePath) {
                playPath(newFilePath)
                return true
            }
        }

        return false
    }

    private fun getFiles(directoryPath: String): Array<String?> {
        val directory = File(directoryPath)
        val files = directory.listFiles(FileFilter { file ->
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            if (mimeType != null) {
                return@FileFilter mimeType.startsWith("video/")
            }

            return@FileFilter false;
        })
        if (files != null) {
            val result = arrayOfNulls<String>(files.size)
            for (i in files.indices) {
                result[i] = files[i].name
            }

            return result
        }
        return arrayOfNulls(0)
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
                    stopPlayVideo()
                } else {
                    startPlayVideo()
                }
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                event.keyCode == KeyEvent.KEYCODE_NAVIGATE_NEXT ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!nextTrack(currentTrack)) {
                    chooseFile()
                }
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
                event.keyCode == KeyEvent.KEYCODE_NAVIGATE_PREVIOUS ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!prevTrack(currentTrack)) {
                    chooseFile()
                }
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("action", "onRestart: $lastState")
        resumeActivity()
    }

    override fun onBackPressed() {
        Log.i("action", "onBackPressed")
        lastState = if (lastState == STATE_STOP) STATE_PLAY else STATE_STOP
        resumeActivity()
    }

    private fun resumeActivity() {
        Log.i("action", "resumeActivity: $lastState, $currentTrack")
        if (lastState == STATE_PLAY && !currentTrack.isNullOrEmpty()) {
            startPlayVideo()
        } else {
            chooseFile()
        }
    }

    private fun stopPlayVideo() {
        Log.i("action", "stopPlayVideo")
        lastState = STATE_STOP
//        pauseVideo()
        videoView.stopPlayback()
    }

    override fun onStop() {
        Log.i("action", "onStop")
        pauseVideo()
//        stopPlayVideo()
        super.onStop()
    }

    private fun pauseVideo() {
        if (videoView.isPlaying) {
            Log.i("action", "pauseVideo: at ${videoView.currentPosition}")
            currentPosition = videoView.currentPosition
            videoView.pause()
        } else {
            Log.i("action", "pauseVideo: already paused")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i("action", "onNewIntent: $lastState")
//        lastState = STATE_STOP
//        resumeActivity()
    }
}
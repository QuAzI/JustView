package com.quazi_node.justview

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileFilter


class FullscreenActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private var currentTrack: String? = null
    private var currentPosition: Int = 0
    private var prevTrack: String? = null

    private var flippingDirection: Int = 1

    private var lastState: Int = PLAYER_STATE_STOP
        set(value) {
            Log.i("action", "lastState = $value")
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullScreen()

        videoView = findViewById(R.id.videoView)

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

        videoView.setOnPreparedListener {
            Log.i("action", "setOnPreparedListener")
            prevTrack = currentTrack
        }

        videoView.setOnCompletionListener {
            switchToNextTrackInTheDirection()
        }

        videoView.setOnErrorListener { _, _, _ ->
            Log.i("action", "File playback error: ${this.currentTrack}")
            if (!switchToNextTrackInTheDirection()) {
                chooseFile()
            }
            true
        }

        playSelectedFileIfPresent(intent)
    }

    private fun playSelectedFileIfPresent(intent: Intent?) {
        if (intent != null && Intent.ACTION_VIEW == intent.action) {
            Log.i("action", "action view!!!")
            Log.i("action", intent.dataString!!)
            val pathHelper = URIPathHelper()
            val selectedFile = pathHelper.getPath(this, intent.data!!)
            prevTrack = selectedFile
            playPath(selectedFile!!)
        }
    }

    private fun setFullScreen() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_fullscreen)

        //Set full screen after setting layout content
        @Suppress("DEPRECATION")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController

            if(controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        supportActionBar?.hide()
    }

    private fun switchToNextTrackInTheDirection(): Boolean {
        if (flippingDirection >= 0) {
            return nextTrack(currentTrack)
        }

        return prevTrack(currentTrack)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (currentTrack == null) {
            chooseFile()
        }
    }

    private var chooseFileIntent: Intent? = null

    private fun chooseFile() {
        Log.i("action", "chooseFile")
        pauseVideo()

        if (chooseFileIntent != null) {
            Log.i("action", "chooseFile dialog active yet")
            return
        }

         chooseFileIntent = Intent(this, ChooseFileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(
                "uri",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path
            )
        }

        chooseFileActivity.launch(chooseFileIntent)
    }

    private val chooseFileActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        chooseFileIntent = null
        onChooseFileResult(result)
    }

    private fun onChooseFileResult(result: ActivityResult) {
        Log.i("action", "chooseFile finished with $result.resultCode")
        if (result.resultCode == RESULT_OK) {
            val pathHelper = URIPathHelper()
            val intentData: Intent? = result.data
            if (intentData != null && intentData.data != null) {
                val selectedFile = pathHelper.getPath(this, intentData.data!!)
                prevTrack = selectedFile
                playPath(selectedFile!!)
                return
            } else {
                // Restricted mode
                // For example, Samsung Kids doesn't allow to use ACTION_OPEN_DOCUMENT or ACTION_GET_CONTENT intents
                // ACTION_PICK also useless in restricted mode

                val videos = getVideoFiles(this)
                if (videos.any()) {
                    val selectedFile = pathHelper.getPath(this, videos[0].second)
                    prevTrack = selectedFile
                    playPath(selectedFile!!)
                    return
                }
            }
        }

        startPlayVideo()
    }

    private fun getVideoFiles(context: Context): List<Pair<String, Uri>> {
        val videoList = mutableListOf<Pair<String, Uri>>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Movies/%")

        context.contentResolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val uri = ContentUris.withAppendedId(contentUri, id)

                videoList.add(name to uri)
            }
        }

        return videoList
    }

    private fun playPath(path: String) {
        Log.i("action", "Play: $path")
        stopPlayVideo()
        currentTrack = path
        currentPosition = 0
        startPlayVideo()
    }

    private fun startPlayVideo() {
        Log.i("action", "startPlayVideo: from $currentPosition play '$currentTrack'")
        if (!currentTrack.isNullOrBlank()) {
            try {
                videoView.requestFocus()
                val uri = Uri.fromFile(File(currentTrack!!))
                videoView.setVideoURI(uri)
                if (videoView.currentPosition != currentPosition) {
                    videoView.seekTo(currentPosition)
                }
                videoView.start()
                lastState = PLAYER_STATE_START
                videoView.setMediaController(null)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        supportActionBar?.hide()
    }

    private fun nextTrack(path: String?): Boolean {
        Log.i("action", "nextTrack $path")
        return playNextTrackInTheDirection(path, 1)
    }

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

            return@FileFilter false
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        Log.i("action", "onBackPressed")
        lastState = if (lastState == PLAYER_STATE_STOP) PLAYER_STATE_START else PLAYER_STATE_STOP
        resumeActivity()
    }

    private fun resumeActivity() {
        Log.i("action", "resumeActivity: $lastState, $currentTrack")
        if (lastState == PLAYER_STATE_START && !currentTrack.isNullOrEmpty()) {
            startPlayVideo()
        } else {
            chooseFile()
        }
    }

    private fun stopPlayVideo() {
        Log.i("action", "stopPlayVideo")
        lastState = PLAYER_STATE_STOP
        videoView.stopPlayback()
    }

    override fun onStop() {
        Log.i("action", "onStop")
        pauseVideo()
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
        playSelectedFileIfPresent(intent)
    }

    companion object {
        const val PLAYER_STATE_STOP : Int = 0
        const val PLAYER_STATE_START : Int = 1
    }
}
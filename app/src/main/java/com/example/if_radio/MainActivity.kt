package com.example.if_radio

import android.Manifest
import kotlinx.coroutines.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var job: Job? = null
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var songInfoTextView: TextView
    private val radioStationUrls = arrayOf(
        "https://mediaserv38.live-streams.nl:8107/stream",
        "https://n12.radiojar.com/fqt2y0ds97zuv?rj-ttl=5&rj-tok=AAABjwesAq8A9PMhn7HQFNU3XA",
        "https://dc1.serverse.com/proxy/wiupfvnu?mp=/TradCan",
        "https://cast02.siamsa.ie:8000/radio.mp3"
    )

    private val radioStationNames = arrayOf(
        "Svensk Folk",
        "Radio Folk",
        "Le Canard Folk",
        "Radio Siamsa"
    )

    private val recordAudioRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songInfoTextView = findViewById(R.id.songInfoTextView)
        val detectSongButton: Button = findViewById(R.id.detectSongButton)

        setupMediaPlayer()  // Set up media player with proper error handling

        // Check for permissions and initialize media recorder
        if (isRecordAudioPermissionGranted()) {
            setupMediaRecorder()
        } else {
            requestRecordAudioPermission()
        }

        detectSongButton.setOnClickListener {
            // Start recording and detecting the song
            recordAndDetectSong()
        }

        // Initialize buttons for playing radio stations
        radioStationUrls.forEachIndexed { index, url ->
            val buttonId = R.id::class.java.getField("radioButton$index").getInt(null)
            findViewById<Button>(buttonId)?.setOnClickListener {
                playRadioStream(url, radioStationNames[index])
            }
        }
    }

    private fun resolveRedirects(url: String): String {
        var connection: HttpURLConnection? = null
        try {
            var currentUrl = url
            var redirected: Boolean
            do {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.connect()
                val code = connection.responseCode
                redirected = code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_SEE_OTHER
                if (redirected) {
                    currentUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                }
            } while (redirected)
            return currentUrl
        } finally {
            connection?.disconnect()
        }
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnErrorListener { _, what, extra ->
                Log.e("MediaPlayer", "Error occurred: What: $what, Extra: $extra")
                true  // Indicate that the error was handled
            }
            setOnPreparedListener {
                Log.d("MediaPlayer", "MediaPlayer is prepared, starting playback")
                start()
            }
            setOnCompletionListener {
                Log.d("MediaPlayer", "Playback completed")
            }
        }
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), recordAudioRequestCode)
    }

    private fun setupMediaRecorder() {
        // Initialize your MediaRecorder and configure it
        Log.d("MediaRecorder", "MediaRecorder setup complete")
    }

    private fun recordAndDetectSong() {
        // Start the MediaRecorder to record a snippet of the audio
        // Then make a call to the Shazam API to detect the song
        // Parse the response and update the song info text view
        Log.d("MediaRecorder", "Recording and detecting song")
    }

    private fun playRadioStream(url: String, stationName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mediaPlayer.reset()
                Log.d("MediaPlayer", "MediaPlayer reset")

                val finalUrl = resolveRedirects(url)
                Log.d("MediaPlayer", "Resolved URL: $finalUrl")

                withContext(Dispatchers.Main) {
                    mediaPlayer.setDataSource(finalUrl)
                    mediaPlayer.prepareAsync() // Prepare asynchronously
                    mediaPlayer.setOnPreparedListener {
                        Log.d("MediaPlayer", "MediaPlayer is prepared, starting playback")
                        mediaPlayer.start()
                        songInfoTextView.text = stationName // Update the TextView to show the current station
                    }
                    mediaPlayer.setOnErrorListener { _, what, extra ->
                        Log.e("MediaPlayer", "Playback error: What: $what, Extra: $extra")
                        true // Handle the error
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Error setting data source", e)
            }
        }
    }

    // Handle permission request response
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == recordAudioRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMediaRecorder()
        } else {
            Log.e("Permissions", "Record audio permission was denied")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()  // Cancel any ongoing coroutine job

        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()  // Stop playback if currently playing
        }
        mediaPlayer.release()  // Release MediaPlayer resources

        if (::mediaRecorder.isInitialized) {
            mediaRecorder.release()  // Release MediaRecorder resources if initialized
        }
    }
}
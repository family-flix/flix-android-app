package com.familyflix.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyflix.app.databinding.ActivityPlayerBinding
import com.familyflix.app.model.MediaTypes
import com.familyflix.app.model.SourceItem
import com.familyflix.app.network.RetrofitClient
import com.familyflix.app.ui.adapter.EpisodeAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.math.abs

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    
    private var mediaId: String? = null
    private var mediaType: Int = MediaTypes.MOVIE
    
    private var sources: List<SourceItem> = emptyList()
    private var currentSourceId: String? = null

    // Gesture control properties
    private lateinit var gestureDetector: GestureDetector
    private var audioManager: AudioManager? = null
    private var isSpeedingUp = false
    private var isSeeking = false
    private var seekPosition: Long = 0
    private var currentVolume: Int = 0
    private var gestureOrientation: Int = 0 // 0: none, 1: horizontal (seek), 2: vertical (volume)

    companion object {
        private const val EXTRA_MEDIA_ID = "media_id"
        private const val EXTRA_MEDIA_TYPE = "media_type"

        fun start(context: Context, mediaId: String, mediaType: Int) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_ID, mediaId)
                putExtra(EXTRA_MEDIA_TYPE, mediaType)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
        mediaType = intent.getIntExtra(EXTRA_MEDIA_TYPE, MediaTypes.MOVIE)

        setupPlayerControls()
        setupGestures()

        if (mediaId != null) {
            fetchPlayingInfo()
        } else {
            Toast.makeText(this, "Invalid Media ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupPlayerControls() {
        binding.playerView.setControllerVisibilityListener(object : androidx.media3.ui.PlayerView.ControllerVisibilityListener {
             override fun onVisibilityChanged(visibility: Int) {
                 if (visibility == View.VISIBLE) {
                     hideSystemUI()
                 }
             }
        })
        
        binding.playerView.post {
            val btnEpisodes = binding.playerView.findViewById<View>(R.id.btn_episodes)
            if (btnEpisodes != null) {
                btnEpisodes.setOnClickListener {
                    showEpisodeListDialog()
                }
                
                if (mediaType == MediaTypes.MOVIE) {
                    btnEpisodes.visibility = View.GONE
                } else {
                    btnEpisodes.visibility = View.VISIBLE
                }
            }

            val btnFullscreen = binding.playerView.findViewById<ImageButton>(R.id.btn_fullscreen)
            if (btnFullscreen != null) {
                btnFullscreen.setOnClickListener {
                    toggleFullscreen()
                }
                updateFullscreenButton(btnFullscreen)
            }
        }
    }

    private fun toggleFullscreen() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun updateFullscreenButton(btn: ImageButton?) {
        val button = btn ?: binding.playerView.findViewById(R.id.btn_fullscreen) ?: return
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            button.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            button.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateFullscreenButton(null)
        hideSystemUI()
    }

    private fun showEpisodeListDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_episode_list, null)
        dialog.setContentView(view)
        
        val rvEpisodes = view.findViewById<RecyclerView>(R.id.rv_episode_list)
        rvEpisodes.layoutManager = LinearLayoutManager(this)
        
        val adapter = EpisodeAdapter { item ->
            fetchVideoUrl(item.id)
            currentSourceId = item.id
            dialog.dismiss()
        }
        rvEpisodes.adapter = adapter
        adapter.submitList(sources)
        if (currentSourceId != null) {
            adapter.setCurrentPlaying(currentSourceId!!)
        }
        
        dialog.show()
    }

    private fun setupGestures() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        gestureDetector = GestureDetector(this, PlayerGestureListener())

        binding.playerView.setOnTouchListener { _, event ->
            if (gestureDetector.onTouchEvent(event)) return@setOnTouchListener true
            
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (isSpeedingUp) {
                    isSpeedingUp = false
                    player?.playbackParameters = PlaybackParameters(1.0f)
                    binding.tvSpeedOverlay.visibility = View.GONE
                }
                if (isSeeking || gestureOrientation != 0) {
                    binding.layoutGestureFeedback.visibility = View.GONE
                    isSeeking = false
                    gestureOrientation = 0
                }
            }
            return@setOnTouchListener false
        }
    }

    private inner class PlayerGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            seekPosition = player?.currentPosition ?: 0
            currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            gestureOrientation = 0
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (!isSeeking && player?.isPlaying == true) {
                isSpeedingUp = true
                player?.playbackParameters = PlaybackParameters(2.0f)
                binding.tvSpeedOverlay.visibility = View.VISIBLE
                binding.tvSpeedOverlay.text = "2.0x Speed"
            }
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (e1 == null) return false
            
            val absX = abs(e2.x - e1.x)
            val absY = abs(e2.y - e1.y)
            
            if (gestureOrientation == 0) {
                if (absX > absY && absX > 50) { 
                    gestureOrientation = 1 // Seek
                } else if (absY > absX && absY > 50) {
                    gestureOrientation = 2 // Volume
                }
            }

            if (gestureOrientation == 1) {
                val width = binding.playerView.width
                val totalDeltaX = e2.x - e1.x
                val seekChange = (totalDeltaX / width * 120000).toLong() // 2 mins for full width
                var newPos = seekPosition + seekChange
                newPos = newPos.coerceIn(0, player?.duration ?: 0)
                
                player?.seekTo(newPos)
                showSeekOverlay(newPos, seekChange)
                isSeeking = true
                return true
            } else if (gestureOrientation == 2) {
                val height = binding.playerView.height
                val totalDeltaY = e1.y - e2.y // Up is positive for volume increase
                
                val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
                val deltaVol = (totalDeltaY / height * maxVol * 1.5).toInt()
                val newVol = (currentVolume + deltaVol).coerceIn(0, maxVol)
                
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                showVolumeOverlay(newVol, maxVol)
                return true
            }
            return false
        }
        
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
             if (binding.playerView.isControllerFullyVisible) {
                 binding.playerView.hideController()
             } else {
                 binding.playerView.showController()
             }
             return true
        }
    }

    private fun showSeekOverlay(currentPos: Long, change: Long) {
        binding.layoutGestureFeedback.visibility = View.VISIBLE
        binding.ivGestureIcon.setImageResource(if (change > 0) android.R.drawable.ic_media_next else android.R.drawable.ic_media_previous)
        val sign = if (change > 0) "+" else ""
        binding.tvGestureText.text = "${formatTime(currentPos)} ($sign${change/1000}s)"
    }

    private fun showVolumeOverlay(volume: Int, maxVolume: Int) {
        binding.layoutGestureFeedback.visibility = View.VISIBLE
        binding.ivGestureIcon.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
        val percent = (volume * 100 / maxVolume)
        binding.tvGestureText.text = "Volume: $percent%"
    }
    
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun fetchPlayingInfo() {
        lifecycleScope.launch {
            try {
                val request = com.familyflix.app.model.MediaPlayingRequest(
                    mediaId = mediaId!!,
                    type = mediaType
                )
                val playingResponse = RetrofitClient.apiService.getMediaPlaying(request)
                
                if (playingResponse.code == 0) {
                    val curSource = playingResponse.data.curSource
                    if (curSource == null) {
                        Toast.makeText(this@PlayerActivity, "No playable source found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    val curSourceId = curSource.id
                    sources = playingResponse.data.sources
                    currentSourceId = curSourceId
                    
                    fetchVideoUrl(curSourceId)
                } else {
                    Toast.makeText(this@PlayerActivity, "Error: ${playingResponse.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Failed to load media info: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun fetchVideoUrl(sourceId: String) {
        lifecycleScope.launch {
            try {
                val request = com.familyflix.app.model.SourcePlayingRequest(
                    id = sourceId,
                    type = "SD"
                )
                val sourceResponse = RetrofitClient.apiService.getSourcePlaying(request)
                
                if (sourceResponse.code == 0) {
                    val videoUrl = RetrofitClient.BASE_URL + sourceResponse.data.url
                    initializePlayer(videoUrl)
                } else {
                    Toast.makeText(this@PlayerActivity, "Error: ${sourceResponse.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Failed to load video URL: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun initializePlayer(url: String) {
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
            binding.playerView.player = player
        }

        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23 && player != null) {
            player?.play()
        }
    }

    override fun onResume() {
        super.onResume()
        if ((Build.VERSION.SDK_INT <= 23 || player == null)) {
            // Simplified logic
        }
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer()
        }
    }
}
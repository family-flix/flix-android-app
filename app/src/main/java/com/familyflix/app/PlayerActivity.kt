package com.familyflix.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyflix.app.databinding.ActivityPlayerBinding
import com.familyflix.app.model.MediaTypes
import com.familyflix.app.network.RetrofitClient
import com.familyflix.app.ui.adapter.EpisodeAdapter
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    
    private var mediaId: String? = null
    private var mediaType: Int = MediaTypes.MOVIE
    
    private lateinit var episodeAdapter: EpisodeAdapter

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

        setupRecyclerView()

        mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
        mediaType = intent.getIntExtra(EXTRA_MEDIA_TYPE, MediaTypes.MOVIE)

        if (mediaType == MediaTypes.MOVIE) {
            binding.rvEpisodes.visibility = View.GONE
        }

        if (mediaId != null) {
            fetchPlayingInfo()
        } else {
            Toast.makeText(this, "Invalid Media ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        episodeAdapter = EpisodeAdapter { item ->
            // Switch episode
            // We need to find the file source id from the item
            val sources = item.sources
            if (!sources.isNullOrEmpty()) {
                val fileId = sources[0].id
                fetchVideoUrl(fileId)
                episodeAdapter.setCurrentPlaying(item.id)
            } else {
                Toast.makeText(this, "No video source for this episode", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvEpisodes.layoutManager = LinearLayoutManager(this)
        binding.rvEpisodes.adapter = episodeAdapter
    }

    private fun fetchPlayingInfo() {
        lifecycleScope.launch {
            try {
                // 1. Get playing info (episodes, current source)
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
                    
                    val curSourceFileId = curSource.curSourceFileId
                    val sources = playingResponse.data.sources
                    
                    if (mediaType != MediaTypes.MOVIE) {
                        // Update episodes list
                        episodeAdapter.submitList(sources)
                        
                        // Highlight the current episode
                        val currentEpisode = sources.find { it.order == curSource.order }
                        if (currentEpisode != null) {
                            episodeAdapter.setCurrentPlaying(currentEpisode.id)
                        }
                    }

                    fetchVideoUrl(curSourceFileId)
                } else {
                    Toast.makeText(this@PlayerActivity, "Error: ${playingResponse.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Failed to load media info: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun fetchVideoUrl(sourceFileId: String) {
        lifecycleScope.launch {
            try {
                // 2. Get actual video URL
                val request = com.familyflix.app.model.SourcePlayingRequest(
                    id = sourceFileId,
                    type = "SD" // Default resolution
                )
                val sourceResponse = RetrofitClient.apiService.getSourcePlaying(request)
                
                if (sourceResponse.code == 0) {
                    val videoUrl = sourceResponse.data.url
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
            // If we have URL we could re-init, but we need to store it.
            // Simplified: do nothing if player was released. ideally restore state.
        }
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

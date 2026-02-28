package com.familyflix.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyflix.app.databinding.ActivityMainBinding
import com.familyflix.app.network.RetrofitClient
import com.familyflix.app.network.TokenManager
import com.familyflix.app.ui.main.MainPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            TokenManager.init(applicationContext)

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupViewPager()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "电影"
                1 -> "电视剧"
                2 -> "视频"
                else -> "随机"
            }
        }.attach()
    }
}

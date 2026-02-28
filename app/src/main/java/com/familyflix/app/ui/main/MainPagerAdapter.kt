package com.familyflix.app.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.familyflix.app.model.MediaTypes

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MediaListFragment.newInstance(MediaTypes.MOVIE)
            1 -> MediaListFragment.newInstance(MediaTypes.SEASON)
            2 -> MediaListFragment.newInstance(MediaTypes.VIDEO)
            else -> MediaListFragment.newInstance(MediaTypes.VIDEO, isRandom = true)
        }
    }
}

package com.familyflix.app.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.familyflix.app.model.MediaTypes

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MediaListFragment.newInstance(MediaTypes.MOVIE)
            1 -> MediaListFragment.newInstance(MediaTypes.SEASON)
            else -> MediaListFragment.newInstance(MediaTypes.VIDEO)
        }
    }
}

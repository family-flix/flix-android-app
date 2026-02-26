package com.familyflix.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.familyflix.app.databinding.ItemMediaBinding
import com.familyflix.app.model.MediaItem

class MediaAdapter(
    private val onItemClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private val items = mutableListOf<MediaItem>()

    fun submitList(newItems: List<MediaItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    fun addAll(newItems: List<MediaItem>) {
        val start = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }
    
    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            binding.tvTitle.text = item.name
            binding.tvSubtitle.text = item.airDate
            
            val vote = item.voteAverage
            if (vote != null && vote > 0) {
                binding.tvRating.text = String.format("%.1f", vote)
            } else {
                binding.tvRating.text = "N/A"
            }
            
            binding.tvOverview.text = item.overview ?: ""

            val posterUrl = if (item.posterPath != null) {
                val url = if (item.posterPath.startsWith("http")) item.posterPath
                else "https://media-t.funzm.com${item.posterPath}"
                "http://192.168.1.118:3200/api/proxy/javbus?url=$url"
            } else null

            Glide.with(binding.root.context)
                .load(posterUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivPoster)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

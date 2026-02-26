package com.familyflix.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.familyflix.app.databinding.ItemEpisodeBinding
import com.familyflix.app.model.SourceItem
import android.graphics.Color

class EpisodeAdapter(
    private val onItemClick: (SourceItem) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

    private val items = mutableListOf<SourceItem>()
    private var currentPlayingId: String? = null

    fun submitList(newItems: List<SourceItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    fun setCurrentPlaying(id: String) {
        currentPlayingId = id
        notifyDataSetChanged()
    }

    inner class EpisodeViewHolder(private val binding: ItemEpisodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SourceItem) {
            binding.tvTitle.text = "第 ${item.order} 集"
            binding.tvSubtitle.text = item.name

            val isPlaying = item.id == currentPlayingId
            if (isPlaying) {
                binding.root.setBackgroundColor(Color.parseColor("#33000000")) // Highlight
                binding.tvTitle.setTextColor(Color.BLUE)
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
                binding.tvTitle.setTextColor(Color.BLACK) // Default text color
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

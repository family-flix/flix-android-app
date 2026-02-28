package com.familyflix.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyflix.app.databinding.FragmentMediaListBinding
import com.familyflix.app.model.MediaItem
import com.familyflix.app.model.MediaTypes
import com.familyflix.app.network.RetrofitClient
import com.familyflix.app.ui.adapter.MediaAdapter
import kotlinx.coroutines.launch

class MediaListFragment : Fragment() {

    private var _binding: FragmentMediaListBinding? = null
    private val binding get() = _binding!!

    private var mediaType: Int = MediaTypes.MOVIE
    private var isRandom: Boolean = false
    private var seed: Long = 0
    private lateinit var adapter: MediaAdapter
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_RANDOM = "random"

        fun newInstance(type: Int, isRandom: Boolean = false): MediaListFragment {
            val fragment = MediaListFragment()
            val args = Bundle()
            args.putInt(ARG_TYPE, type)
            args.putBoolean(ARG_RANDOM, isRandom)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mediaType = it.getInt(ARG_TYPE)
            isRandom = it.getBoolean(ARG_RANDOM, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        loadData(refresh = true)
    }

    private fun setupRecyclerView() {
        adapter = MediaAdapter { item ->
            context?.let {
                com.familyflix.app.PlayerActivity.start(it, item.id, item.type)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadData(refresh = false)
                    }
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData(refresh = true)
        }
    }

    private fun loadData(refresh: Boolean) {
        if (isLoading) return
        isLoading = true

        if (refresh) {
            currentPage = 1
            isLastPage = false
            if (isRandom) {
                seed = System.currentTimeMillis()
            }
        }

        lifecycleScope.launch {
            try {
                val request = com.familyflix.app.model.MediaListRequest(
                    page = currentPage,
                    pageSize = 20,
                    type = mediaType,
                    random = isRandom,
                    seed = if (isRandom) seed else 0
                )

                val response = if (mediaType == MediaTypes.SEASON) {
                    RetrofitClient.apiService.getSeasonList(request)
                } else {
                    RetrofitClient.apiService.getMediaList(request)
                }

                if (response.code == 0) {
                    val list = response.data.list
                    if (refresh) {
                        adapter.submitList(list)
                    } else {
                        adapter.addAll(list)
                    }
                    
                    if (adapter.itemCount == 0) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }

                    if (list.size < 20) {
                        isLastPage = true
                    } else {
                        currentPage++
                    }
                } else {
                    Toast.makeText(context, "Error: ${response.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                isLoading = false
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

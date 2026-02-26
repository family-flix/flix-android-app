package com.familyflix.app.model

import com.google.gson.annotations.SerializedName

data class MediaListResponse(
    val list: List<MediaItem>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int
)

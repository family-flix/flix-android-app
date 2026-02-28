package com.familyflix.app.model

import com.google.gson.annotations.SerializedName

data class MediaListRequest(
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    val type: Int,
    val random: Boolean = false,
    val seed: Long = 0
)

data class MediaPlayingRequest(
    @SerializedName("media_id") val mediaId: String,
    val type: Int
)

data class SourcePlayingRequest(
    val id: String,
    val type: String
)

data class LoginRequest(
    @SerializedName("token") val token: String,
    @SerializedName("tmp") val tmp: Int? = null
)

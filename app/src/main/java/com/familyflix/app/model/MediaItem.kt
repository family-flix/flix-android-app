package com.familyflix.app.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MediaItem(
    val id: String,
    val type: Int,
    val name: String,
    @SerializedName("original_name") val originalName: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?
) : Serializable

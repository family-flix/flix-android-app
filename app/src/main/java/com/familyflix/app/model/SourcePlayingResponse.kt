package com.familyflix.app.model

import com.google.gson.annotations.SerializedName

data class SourcePlayingResponse(
    val id: String,
    val url: String,
    val type: String
)

package com.familyflix.app.model

import com.google.gson.annotations.SerializedName

data class MediaPlayingResponse(
    val id: String,
    val name: String,
    val overview: String?,
    @SerializedName("cur_source") val curSource: CurSource?,
    val sources: List<SourceItem>
)

data class CurSource(
    val id: String,
    @SerializedName("cur_source_file_id") val curSourceFileId: String,
    val order: Int,
    val name: String
)

data class SourceItem(
    val id: String,
    val name: String,
    val order: Int,
    val sources: List<SourceFile>?
)

data class SourceFile(
    val id: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("parent_paths") val parentPaths: String
)

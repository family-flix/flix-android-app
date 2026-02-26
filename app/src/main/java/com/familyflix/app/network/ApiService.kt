package com.familyflix.app.network

import com.familyflix.app.model.*
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/v2/wechat/media/list")
    suspend fun getMediaList(@Body body: MediaListRequest): ApiResponse<MediaListResponse>

    @POST("/api/v2/wechat/season/list")
    suspend fun getSeasonList(@Body body: MediaListRequest): ApiResponse<MediaListResponse>

    @POST("/api/v2/wechat/media/playing")
    suspend fun getMediaPlaying(@Body body: MediaPlayingRequest): ApiResponse<MediaPlayingResponse>

    @POST("/api/v2/wechat/source")
    suspend fun getSourcePlaying(@Body body: SourcePlayingRequest): ApiResponse<SourcePlayingResponse>

    @POST("/api/validate")
    suspend fun loginWithTokenId(@Body body: LoginRequest): ApiResponse<LoginResponse>
}

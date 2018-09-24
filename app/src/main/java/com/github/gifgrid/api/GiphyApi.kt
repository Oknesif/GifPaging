package com.github.gifgrid.api

import com.github.gifgrid.trending.GifPaginatedResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyApi {

    @GET("trending")
    fun getTrending(
            @Query("offset") offset: Int,
            @Query("limit") limit: Int,
            @Query("key") key: String
    ): Single<GifPaginatedResponse>

    @GET("search")
    fun getSearch(
            @Query("q") q: String,
            @Query("offset") offset: Int,
            @Query("limit") limit: Int,
            @Query("key") key: String
    ): Single<GifPaginatedResponse>
}
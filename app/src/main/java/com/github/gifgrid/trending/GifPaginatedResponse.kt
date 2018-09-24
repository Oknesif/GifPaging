package com.github.gifgrid.trending

import com.squareup.moshi.Json

class GifPaginatedResponse(
        @field:Json(name = "data")
        val gifs: List<Gif>,
        @field:Json(name = "pagination")
        val pagination: Pagination
)


data class Gif(
        @field:Json(name = "id")
        val id: String
) {
    fun getMediaLink(): String = "https://media1.giphy.com/media/${this.id}/giphy.gif"
}

class Pagination(
        @field:Json(name = "offset")
        val offset: Int,
        @field:Json(name = "total_count")
        val totalCount: Int,
        @field:Json(name = "count")
        val count: Int
)
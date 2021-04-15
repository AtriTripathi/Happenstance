package com.atritripathi.happenstance.api

import com.squareup.moshi.Json

data class NewsArticleDto(
    @Json(name = "title")
    val title: String?,

    @Json(name = "url")
    val url: String,

    @Json(name = "urlToImage")
    val imageUrl: String?
)
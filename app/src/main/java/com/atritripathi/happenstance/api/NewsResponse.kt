package com.atritripathi.happenstance.api

import com.squareup.moshi.Json

data class NewsResponse(
    @Json(name = "articles")
    val articles: List<NewsArticleDto>
)
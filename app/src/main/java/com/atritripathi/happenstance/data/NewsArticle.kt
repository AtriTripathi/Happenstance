package com.atritripathi.happenstance.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticle(
    @ColumnInfo(name = "title")
    val title: String?,

    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "thumbnailUrl")
    val thumbnailUrl: String?,

    @ColumnInfo(name = "isBookmarked")
    val isBookmarked: Boolean,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "breaking_news")
data class BreakingNews(
    @ColumnInfo(name = "articleUrl")
    val articleUrl: String,

    /**
     * We're adding an `id` field as PK instead of just using the `articleUrl`
     * because SQLite may shuffle the news article from the order in which it
     * originally retrieved from the network.
     * Having an `id` with autoGenerate as true ensures that Room will store
     * articles in a sequential order, as it retrieves them.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0
)
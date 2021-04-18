package com.atritripathi.happenstance.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NewsArticle::class, BreakingNews::class],
    version = 1,
    exportSchema = false
)
abstract class NewsDatabase : RoomDatabase() {

    companion object {
        const val DB_NAME = "news_db"
    }

    abstract fun newsArticleDao(): NewsArticleDao
}
package com.atritripathi.happenstance.data

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsArticleDao {

    @Query("SELECT * FROM breaking_news INNER JOIN news_articles ON articleUrl = url")
    fun getAllBreakingNewsArticles(): Flow<List<NewsArticle>>

    @Query(
        """ SELECT * FROM search_results 
                 INNER JOIN news_articles ON articleUrl = url
                 WHERE searchQuery = :query ORDER BY queryPosition"""
    )
    fun getSearchResultArticlesPaged(query: String): PagingSource<Int, NewsArticle>

    @Query("SELECT * FROM news_articles WHERE isBookmarked = 1")
    fun getAllBookmarkedArticles(): Flow<List<NewsArticle>>

    @Query("SELECT MAX(queryPosition) FROM search_results WHERE searchQuery = :query")
    suspend fun getLastQueryPosition(query: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticle>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakingNews(breakingNews: List<BreakingNews>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(searchResults: List<SearchResult>)

    @Update
    suspend fun updateArticle(article: NewsArticle)

    @Query("UPDATE news_articles SET isBookmarked = 0")
    suspend fun resetAllBookmarks()

    @Query("DELETE FROM search_results WHERE searchQuery = :query")
    suspend fun deleteSearchResultsForQuery(query: String)

    @Query("DELETE FROM breaking_news")
    suspend fun deleteAllBreakingNews()

    @Query("DELETE FROM news_articles WHERE updatedAt < :timestampInMillis AND isBookmarked = 0")
    suspend fun deleteNonBookmarkedArticlesOlderThan(timestampInMillis: Long)
}
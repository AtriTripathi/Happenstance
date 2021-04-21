package com.atritripathi.happenstance.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.atritripathi.happenstance.api.NewsApi
import com.atritripathi.happenstance.util.Resource
import com.atritripathi.happenstance.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val newsApi: NewsApi,
    private val newsDb: NewsDatabase
) {
    private val newsArticleDao = newsDb.newsArticleDao()

    fun getBreakingNews(
        forceRefresh: Boolean,
        onFetchSuccess: () -> Unit,
        onFetchFailed: (Throwable) -> Unit
    ): Flow<Resource<List<NewsArticle>>> = networkBoundResource(
        query = {
            newsArticleDao.getAllBreakingNewsArticles()
        },
        fetch = {
            val response = newsApi.getBreakingNews()
            response.articles
        },
        saveFetchResult = { fetchedBreakingNewsArticles ->
            val bookmarkedArticles = newsArticleDao.getAllBookmarkedArticles().first()

            val breakingNewsArticles = fetchedBreakingNewsArticles.map { remoteNewsArticle ->
                val isBookmarked = bookmarkedArticles.any { bookmarkedArticle ->
                    bookmarkedArticle.url == remoteNewsArticle.url
                }

                NewsArticle(
                    title = remoteNewsArticle.title ?: "",
                    url = remoteNewsArticle.url,
                    thumbnailUrl = remoteNewsArticle.imageUrl ?: "",
                    isBookmarked = isBookmarked
                )
            }
            // For all breaking news articles fetched and saved into `news_article` table,
            // we also want to save them into the `breaking_news` table.
            val breakingNews = breakingNewsArticles.map { article ->
                BreakingNews(articleUrl = article.url)
            }

            newsDb.withTransaction {
                with(newsArticleDao) {
                    deleteAllBreakingNews()
                    insertArticles(breakingNewsArticles)
                    insertBreakingNews(breakingNews)
                }
            }
        },
        shouldFetch = { cachedArticles ->
            if (forceRefresh) {
                true
            } else {
                val sortedArticles = cachedArticles.sortedBy { article ->
                    article.updatedAt
                }
                val oldestTimestamp = sortedArticles.firstOrNull()?.updatedAt
                val needsRefresh = oldestTimestamp == null ||
                        oldestTimestamp < System.currentTimeMillis() -
                        TimeUnit.MINUTES.toMillis(5)
                needsRefresh
            }
        },
        onFetchSuccess = onFetchSuccess,
        onFetchFailed = { t ->
            if (t !is HttpException && t !is IOException) {
                // We explicitly want to crash the app, cause this error isn't expected
                // and can potentially leave the app in an unexpected state.
                throw t
            }
            onFetchFailed(t)
        }
    )

    fun getSearchResultsPaged(query: String): Flow<PagingData<NewsArticle>> =
        Pager(
            config = PagingConfig(pageSize = 20, maxSize = 200),
            remoteMediator = SearchNewsRemoteMediator(query, newsApi, newsDb),
            pagingSourceFactory = { newsArticleDao.getSearchResultArticlesPaged(query) }
        ).flow

    fun getAllBookmarkedArticles(): Flow<List<NewsArticle>> =
        newsArticleDao.getAllBookmarkedArticles()

    suspend fun updateArticle(article: NewsArticle) {
        newsArticleDao.updateArticle(article)
    }

    suspend fun resetAllBookmarks() {
        newsArticleDao.resetAllBookmarks()
    }

    suspend fun deleteNonBookmarkedArticlesOlderThan(timestampMillis: Long) {
        newsArticleDao.deleteNonBookmarkedArticlesOlderThan(timestampMillis)
    }
}
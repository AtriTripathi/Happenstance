package com.atritripathi.happenstance.data

import androidx.room.withTransaction
import com.atritripathi.happenstance.api.NewsApi
import com.atritripathi.happenstance.util.Resource
import com.atritripathi.happenstance.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val newsApi: NewsApi,
    private val newsDb: NewsDatabase
) {
    private val newsArticleDao = newsDb.newsArticleDao()

    fun getBreakingNews(
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
            val breakingNewsArticles = fetchedBreakingNewsArticles.map { newsArticle ->
                NewsArticle(
                    title = newsArticle.title ?: "",
                    url = newsArticle.url,
                    thumbnailUrl = newsArticle.imageUrl ?: "",
                    isBookmarked = false
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
}
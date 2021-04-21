package com.atritripathi.happenstance.features.searchnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.atritripathi.happenstance.data.NewsArticle
import com.atritripathi.happenstance.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchNewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {
    private val currentQuery = MutableStateFlow<String?>(null)

    val hasCurrentQuery = currentQuery.map { it != null }

    val searchResults = currentQuery.flatMapLatest { query ->
        query?.let {
            repository.getSearchResultsPaged(query)
        } ?: emptyFlow()
    }.cachedIn(viewModelScope)

    var refreshInProgress = false
    var pendingScrollToTopAfterRefresh = false
    var newQueryInProgress = false
    var pendingScrollToTopAfterNewQuery = false

    fun onSearchQuerySubmit(query: String) {
        currentQuery.value = query
        newQueryInProgress = true
        pendingScrollToTopAfterNewQuery = true
    }

    fun onBookmarkToggle(article: NewsArticle) {
        val updatedArticle = article.copy(isBookmarked = !article.isBookmarked)
        viewModelScope.launch {
            repository.updateArticle(updatedArticle)
        }
    }
}
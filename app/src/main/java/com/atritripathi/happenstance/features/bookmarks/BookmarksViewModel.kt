package com.atritripathi.happenstance.features.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atritripathi.happenstance.data.NewsArticle
import com.atritripathi.happenstance.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    val bookmarks = repository.getAllBookmarkedArticles()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun onBookmarkToggle(article: NewsArticle) {
        val updatedArticle = article.copy(isBookmarked = !article.isBookmarked)
        viewModelScope.launch {
            repository.updateArticle(updatedArticle)
        }
    }

    fun onDeleteAllBookmarks() =
        viewModelScope.launch {
            repository.resetAllBookmarks()
        }
}
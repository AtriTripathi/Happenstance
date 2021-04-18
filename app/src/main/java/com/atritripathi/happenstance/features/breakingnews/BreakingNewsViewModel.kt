package com.atritripathi.happenstance.features.breakingnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atritripathi.happenstance.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BreakingNewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    /**
     * I used `stateIn()` operator to convert the "cold" channelFlow into a "hot" stateFlow
     * which retains its latest value, this prevents the entire flow from being emitted on
     * every lifecycle changes like screen rotations, fragment transactions, etc.
     *
     * This basically allows the whole flow to be started or cancelled via the `viewModelScope`
     * passed as a parameter. I'm also using `Lazily` to start collecting the data only when it's
     * needed, because `getBreakingNews()` performs a network call which can be resource intensive.
     */
    val breakingNews = repository.getBreakingNews()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

}
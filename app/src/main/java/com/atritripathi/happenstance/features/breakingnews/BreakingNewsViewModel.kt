package com.atritripathi.happenstance.features.breakingnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atritripathi.happenstance.data.NewsRepository
import com.atritripathi.happenstance.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BreakingNewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val eventChannel = Channel<Event>()
    val events = eventChannel.receiveAsFlow()

    private val refreshTriggerChannel = Channel<Unit>()
    private val refreshTrigger = refreshTriggerChannel.receiveAsFlow()

    var pendingScrollToTopAfterRefresh = false

    /**
     * I used `stateIn()` operator to convert the "cold" channelFlow into a "hot" stateFlow
     * which retains its latest value, this prevents the entire flow from being emitted on
     * every lifecycle changes like screen rotations, fragment transactions, etc.
     *
     * This basically allows the whole flow to be started or cancelled via the `viewModelScope`
     * passed as a parameter. I'm also using `Lazily` to start collecting the data only when it's
     * needed, because `getBreakingNews()` performs a network call which can be resource intensive.
     */
    val breakingNews = refreshTrigger.flatMapLatest {
        repository.getBreakingNews(
            onFetchSuccess = {
                pendingScrollToTopAfterRefresh = true
            },
            onFetchFailed = { t ->
                viewModelScope.launch { eventChannel.send(Event.ShowErrorMessage(t)) }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun onStart() {
        // Only execute a retry, if the data isn't already being loaded.
        if (breakingNews.value !is Resource.Loading) {
            viewModelScope.launch {
                refreshTriggerChannel.send(Unit)
            }
        }
    }

    fun onManualRefresh() {
        // This check is to prevent from cancelling any previous refresh event.
        if (breakingNews.value !is Resource.Loading) {
            viewModelScope.launch {
                refreshTriggerChannel.send(Unit)
            }
        }
    }

    sealed class Event {
        data class ShowErrorMessage(val error: Throwable) : Event()
    }
}
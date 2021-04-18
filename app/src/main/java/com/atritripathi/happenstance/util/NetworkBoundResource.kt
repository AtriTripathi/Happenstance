package com.atritripathi.happenstance.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,  // ResultType from DB will be a list of `NewsArticle`.
    crossinline fetch: suspend () -> RequestType,   // RequestType from network will be a list of `NewsArticleDto`.
    crossinline saveFetchResult: suspend (RequestType) -> Unit, // Save the network fetch into the DB.
    crossinline shouldFetch: (ResultType) -> Boolean = { true } // Fetch from network based on staleness of data in DB
) = channelFlow {

    // This will only get a single value(List<NewsArticle>) from the DB and then stop collecting
    // from the Flow, because we only need this to check if the cached data is stale or not.
    val data = query().first()

    if (shouldFetch(data)) {    // If cached data is stale, we need to fetch fresh data from network.

        /* Emit `Loading` state inside a new coroutine, in order to execute the concurrent work
           inside a Flow, hence used `channelFlow()`. This allows the Bookmark icon to work even
           while new data is being fetched from the network.
        */
        val loading = launch {
            // New list of news articles from DB with live updated Bookmark states.
            query().collect { send(Resource.Loading(it)) }  // Emit `Loading` state with data.
        }

        try {   // If network fetch was successful
            delay(2000)     // Just to see progress bar for testing.
            saveFetchResult(fetch())    // Save the network fetch results into the DB.
            loading.cancel()    // Cancel the loading coroutine.
            query().collect { send(Resource.Success(it)) }  // Emit `Success` state with data.
        } catch (t: Throwable) {    // Else if unsuccessful
            loading.cancel()    // Cancel the loading coroutine.
            query().collect { send(Resource.Error(t, it)) } // Emit `Error` state with data.
        }
    } else {    // Else simply emit cached data from the DB
        query().collect { send(Resource.Success(it)) }  // Emit `Success` state with data.
    }
}
package com.github.gifgrid.trending

import android.arch.paging.PositionalDataSource
import com.github.gifgrid.BuildConfig
import com.github.gifgrid.api.GiphyApi
import com.github.gifgrid.api.NetworkState
import com.github.gifgrid.plusAssign
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class TrendingSearchDataSource(
        private val api: GiphyApi,
        private val compositeDisposable: CompositeDisposable,
        var searchString: String? = null
) : PositionalDataSource<Gif>() {

    private val stateSubject: Subject<NetworkState> = BehaviorSubject.create()
    fun getStateObservable(): Observable<NetworkState> = stateSubject

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Gif>) {
        compositeDisposable += getDataFromApi(params.requestedStartPosition, params.requestedLoadSize)
                .doOnSubscribe { stateSubject.onNext(NetworkState.Loading) }
                .subscribe({ response: GifPaginatedResponse ->
                    val totalCount = response.pagination.totalCount
                    val position = computeInitialLoadPosition(params, totalCount)
                    callback.onResult(response.gifs, position, totalCount)
                }, { /* Do nothing */ })
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Gif>) {
        compositeDisposable += getDataFromApi(params.startPosition, params.loadSize)
                .subscribe({ response: GifPaginatedResponse ->
                    callback.onResult(response.gifs)
                }, { /* Do nothing */ })
    }

    private fun getDataFromApi(offset: Int, limit: Int): Single<GifPaginatedResponse> {
        val query = searchString
        val key = BuildConfig.API_KEY
        return if (key.contains("KEY")) {
            Single.error(NoKeyException())
        } else {
            if (query == null || query.isEmpty()) {
                api.getTrending(offset, limit, key)
            } else {
                api.getSearch(query, offset, limit, key)
            }
        }.doOnError {
            stateSubject.onNext(NetworkState.Error(it))
        }.doOnSuccess {
            stateSubject.onNext(NetworkState.Loaded(it.gifs))
        }
    }
}

class NoKeyException : IllegalStateException()

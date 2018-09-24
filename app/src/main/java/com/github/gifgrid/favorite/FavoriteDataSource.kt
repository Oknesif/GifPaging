package com.github.gifgrid.favorite

import android.arch.paging.PositionalDataSource
import com.github.gifgrid.api.NetworkState
import com.github.gifgrid.plusAssign
import com.github.gifgrid.trending.Gif
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class FavoriteDataSource(
        private val favoriteManager: FavoriteManager,
        private val compositeDisposable: CompositeDisposable
) : PositionalDataSource<Gif>() {

    private val stateSubject: Subject<NetworkState> = BehaviorSubject.create()
    fun getStateObservable(): Observable<NetworkState> = stateSubject

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Gif>) {
        compositeDisposable += favoriteManager
                .favoriteListObservable()
                .firstOrError()
                .doOnSuccess { stateSubject.onNext(NetworkState.Loaded(it)) }
                .subscribe { list ->
                    val sublist = list.subList(params.startPosition, params.startPosition + params.loadSize)
                    callback.onResult(sublist)
                }
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Gif>) {
        compositeDisposable += favoriteManager
                .favoriteListObservable()
                .firstOrError()
                .doOnSubscribe { stateSubject.onNext(NetworkState.Loading) }
                .doOnSuccess { stateSubject.onNext(NetworkState.Loaded(it)) }
                .subscribe { list ->
                    val totalCount = list.size
                    val position = PositionalDataSource.computeInitialLoadPosition(params, totalCount)
                    val loadSize = PositionalDataSource.computeInitialLoadSize(params, position, totalCount)
                    val sublist = list.subList(position, position + loadSize)
                    callback.onResult(sublist, position, totalCount)
                }
    }
}
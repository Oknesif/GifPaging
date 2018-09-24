package com.github.gifgrid

import android.arch.paging.DataSource
import android.arch.paging.PagedList
import android.arch.paging.PositionalDataSource
import android.arch.paging.RxPagedListBuilder
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    this.add(disposable)
}

fun <T> PositionalDataSource<T>.toFlowable(pageSize: Int): Flowable<PagedList<T>> {
    val factory = object : DataSource.Factory<Int, T>() {
        override fun create(): DataSource<Int, T> {
            return this@toFlowable
        }
    }
    return RxPagedListBuilder(factory, pageSize).buildFlowable(BackpressureStrategy.BUFFER)
}
package com.github.gifgrid.favorite

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.gifgrid.plusAssign
import com.github.gifgrid.trending.Gif
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashSet

class FavoriteManager(
        private val rxPermissions: RxPermissions,
        private val client: OkHttpClient,
        private val context: Context,
        private val compositeDisposable: CompositeDisposable
) {
    private var initiated: Boolean = false
    private val favoriteSet: MutableSet<Gif> = Collections.synchronizedSet(LinkedHashSet<Gif>())
    private val itemsList: Subject<List<Gif>> = BehaviorSubject.create()

    init {
        compositeDisposable += readCachedFiles().subscribe()
    }

    fun getLinkForFavorite(gif: Gif): Uri {
        return Uri.fromFile(gif.toFile())
    }

    fun favoriteListObservable(): Observable<List<Gif>> = itemsList
            .observeOn(AndroidSchedulers.mainThread())

    fun isFavorite(gif: Gif): Boolean = favoriteSet.contains(gif)

    fun setFavorite(gif: Gif, value: Boolean) {
        compositeDisposable += rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .firstOrError()
                .observeOn(Schedulers.io())
                .flatMapCompletable { granted ->
                    if (granted) {
                        if (value) {
                            downloadAndSaveGif(gif)
                        } else {
                            deleteGif(gif)
                        }
                    } else {
                        Completable.complete()
                    }
                }
                .doOnError { Log.e("FavoriteManager", "setFavorite", it) }
                .onErrorComplete()
                .subscribe()
    }

    private fun readCachedFiles(): Completable {
        return Completable.fromAction {
            getCacheDir()
                    .walk()
                    .forEach { file ->
                        if (file.isFile) {
                            favoriteSet.add(Gif(file.name))
                        }
                    }
            itemsList.onNext(favoriteSet.toList())
            initiated = true
        }
    }

    private fun getCacheDir(): File {
        return File(context.filesDir, "gifs")
    }

    private fun Gif.toFile(): File {
        return File(getCacheDir(), this.id)
    }

    private fun downloadAndSaveGif(gif: Gif): Completable {
        return Completable.fromAction {
            val httpRequest = Request.Builder().url(gif.getMediaLink()).build()
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                val file = gif.toFile()
                val parentDir = file.parentFile
                if (parentDir.exists().not()) parentDir.mkdirs()
                val sink = Okio.buffer(Okio.sink(file))
                sink.writeAll(responseBody.source())
                sink.close()

                favoriteSet.add(gif)
                itemsList.onNext(favoriteSet.toList())
            }
        }
    }

    private fun deleteGif(gif: Gif): Completable {
        return Completable.fromAction {
            val result = gif.toFile().delete()
            if (result) {

                favoriteSet.remove(gif)
                itemsList.onNext(favoriteSet.toList())
            }
        }
    }
}
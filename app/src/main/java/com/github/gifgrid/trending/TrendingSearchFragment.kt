package com.github.gifgrid.trending

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.github.gifgrid.GifAdapter
import com.github.gifgrid.MainActivity
import com.github.gifgrid.R
import com.github.gifgrid.api.NetworkState
import com.github.gifgrid.plusAssign
import com.github.gifgrid.toFlowable
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TrendingSearchFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.trending_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val messageView: TextView = view.findViewById(R.id.message_view)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        val requestView: EditText = view.findViewById(R.id.request_view)
        val swipeView = view.findViewById<SwipeRefreshLayout>(R.id.swipe_view)
        (this.activity as MainActivity).let {
            val adapter = GifAdapter(it, it.favoriteManager)
            val dataSource = TrendingSearchDataSource(it.giphyApi, it.compositeDisposable)
            it.compositeDisposable += dataSource.getStateObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { state ->
                        when (state) {
                            is NetworkState.Loaded -> {
                                swipeView.isRefreshing = false
                                messageView.visibility = if (state.list.isNotEmpty()) View.GONE else View.VISIBLE
                                if (state.list.isEmpty()) {
                                    messageView.text = getText(R.string.nothing_found)
                                }
                            }
                            is NetworkState.Loading -> {
                                swipeView.isRefreshing = true
                            }
                            is NetworkState.Error -> {
                                swipeView.isRefreshing = false
                                messageView.visibility = View.VISIBLE
                                messageView.text = if (state.throwable is NoKeyException) {
                                    getText(R.string.no_key)
                                } else {
                                    state.throwable.toString()
                                }
                            }
                        }
                    }
            it.compositeDisposable += requestView.textChanges()
                    .debounce(1.toLong(), TimeUnit.SECONDS)
                    .observeOn(Schedulers.io())
                    .mergeWith(swipeView.refreshes().map { _ -> requestView.text })
                    .toFlowable(BackpressureStrategy.DROP)
                    .flatMap { search ->
                        dataSource.searchString = search.toString()
                        dataSource.toFlowable(15)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { list ->
                        adapter.submitList(list)
                    }

            it.compositeDisposable += it.favoriteManager
                    .favoriteListObservable()
                    .subscribe { _ -> adapter.notifyDataSetChanged() }

            recyclerView.layoutManager = GridLayoutManager(activity, 2)
            recyclerView.adapter = adapter
        }
    }
}


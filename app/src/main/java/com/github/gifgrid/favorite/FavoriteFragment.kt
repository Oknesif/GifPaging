package com.github.gifgrid.favorite

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.gifgrid.GifAdapter
import com.github.gifgrid.MainActivity
import com.github.gifgrid.R
import com.github.gifgrid.api.NetworkState
import com.github.gifgrid.plusAssign
import com.github.gifgrid.toFlowable
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers

class FavoriteFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.favorite_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        val messageView: TextView = view.findViewById(R.id.message_view)
        (this.activity as MainActivity).apply {
            val adapter = GifAdapter(
                    this,
                    this.favoriteManager)
            val dataSource = FavoriteDataSource(favoriteManager, compositeDisposable)
            compositeDisposable += dataSource.getStateObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { state ->
                        when (state) {
                            is NetworkState.Loaded -> {
                                messageView.visibility = if (state.list.isNotEmpty()) View.GONE else View.VISIBLE
                                if (state.list.isEmpty()) {
                                    messageView.text = getText(R.string.no_favorite)
                                }
                            }
                            is NetworkState.Loading -> {
                                messageView.text = getText(R.string.loading)
                            }
                            is NetworkState.Error -> {
                                messageView.text = state.throwable.toString()
                            }
                        }
                    }
            compositeDisposable += this.favoriteManager.favoriteListObservable()
                    .toFlowable(BackpressureStrategy.BUFFER)
                    .switchMap { dataSource.toFlowable(15) }
                    .subscribe({ list ->
                        adapter.submitList(list)
                    }, { throwable ->
                        messageView.text = throwable.toString()
                        messageView.visibility = View.VISIBLE
                    })

            recyclerView.layoutManager = GridLayoutManager(activity, 2)
            recyclerView.adapter = adapter
        }
    }
}
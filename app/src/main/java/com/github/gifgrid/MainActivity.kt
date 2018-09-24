package com.github.gifgrid

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.github.gifgrid.api.GiphyApi
import com.github.gifgrid.favorite.FavoriteFragment
import com.github.gifgrid.favorite.FavoriteManager
import com.github.gifgrid.trending.TrendingSearchFragment
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : AppCompatActivity() {

    val compositeDisposable = CompositeDisposable()
    val favoriteManager: FavoriteManager by lazy {
        FavoriteManager(
                rxPermissions = RxPermissions(this),
                client = OkHttpClient(),
                context = this,
                compositeDisposable = compositeDisposable
        )
    }
    val giphyApi: GiphyApi by lazy {
        Retrofit.Builder()
                .baseUrl("https://api.giphy.com/v1/gifs/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(GiphyApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpViewPager(findViewById(R.id.view_pager))
    }

    private fun setUpViewPager(viewPager: ViewPager) {
        viewPager.adapter = FragmentAdapter(supportFragmentManager)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}

class FragmentAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            TrendingSearchFragment()
        } else {
            FavoriteFragment()
        }
    }

    override fun getCount(): Int = 2
}
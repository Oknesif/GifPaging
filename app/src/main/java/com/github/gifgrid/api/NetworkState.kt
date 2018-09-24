package com.github.gifgrid.api

sealed class NetworkState {
    class Loaded(val list: List<Any>) : NetworkState()
    object Loading : NetworkState()
    class Error(val throwable: Throwable) : NetworkState()
}
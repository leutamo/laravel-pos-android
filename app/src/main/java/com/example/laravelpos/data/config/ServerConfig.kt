package com.example.laravelpos.data.config

import android.content.SharedPreferences
import com.example.laravelpos.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerConfig @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_SERVER_IP = "server_ip"
        private const val DEFAULT_PORT = "8000"
    }

    fun getServerIp(): String {
        return sharedPreferences.getString(KEY_SERVER_IP, BuildConfig.SERVER_IP) ?: BuildConfig.SERVER_IP
    }

    fun setServerIp(ip: String) {
        sharedPreferences.edit().putString(KEY_SERVER_IP, ip).apply()
    }

    fun getBaseUrl(): String {
        val ip = getServerIp()
        return "http://$ip:$DEFAULT_PORT/api/"
    }
}
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

    fun getFullImageUrl(relativePath: String?): String {
        if (relativePath.isNullOrBlank()) return ""
        
        val ip = getServerIp()
        
        // Si el servidor ya devuelve una URL completa (http...)
        if (relativePath.startsWith("http")) {
            // Reemplazamos 127.0.0.1 o localhost por la IP real configurada
            // ya que el servidor suele devolver su propia dirección local.
            return relativePath
                .replace("127.0.0.1", ip)
                .replace("localhost", ip)
        }
        
        // Si es una ruta relativa, asumimos la estructura de Laravel
        return "http://$ip:$DEFAULT_PORT/storage/$relativePath"
    }
}
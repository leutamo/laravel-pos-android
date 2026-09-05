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
        val input = getServerIp().trim()
        
        // Si ya incluye http o https, lo usamos tal cual
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return if (input.endsWith("/")) "${input}api/" else "$input/api/"
        }

        // Si parece un dominio (contiene letras)
        val isDomain = input.any { it.isLetter() }
        
        return if (isDomain) {
            "https://$input/api/"
        } else {
            // Si es IP, verificamos si ya tiene puerto
            if (input.contains(":")) {
                "http://$input/api/"
            } else {
                "http://$input:$DEFAULT_PORT/api/"
            }
        }
    }

    fun getFullImageUrl(relativePath: String?): String {
        if (relativePath.isNullOrBlank()) return ""
        
        val input = getServerIp().trim()
        val isDomain = input.any { it.isLetter() }
        
        // Si el servidor ya devuelve una URL completa (http...)
        if (relativePath.startsWith("http")) {
            // Reemplazamos 127.0.0.1 o localhost por la IP o Dominio real
            // Quitamos el protocolo del input para el replace si lo tuviera
            val cleanInput = input.replace("http://", "").replace("https://", "").removeSuffix("/")
            return relativePath
                .replace("127.0.0.1", cleanInput)
                .replace("localhost", cleanInput)
        }
        
        // Si es una ruta relativa
        return if (isDomain) {
            "https://$input/storage/$relativePath"
        } else {
            if (input.contains(":")) {
                "http://$input/storage/$relativePath"
            } else {
                "http://$input:$DEFAULT_PORT/storage/$relativePath"
            }
        }
    }
}
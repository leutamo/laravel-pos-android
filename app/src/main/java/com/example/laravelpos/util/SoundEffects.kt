package com.example.laravelpos.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object SoundEffects {
    private var toneGenerator: ToneGenerator? = null

    private fun getToneGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            } catch (e: Exception) {
                Log.e("SoundEffects", "Error instanciando ToneGenerator: ${e.message}")
            }
        }
        return toneGenerator
    }

    /**
     * Tono de éxito tipo escáner de supermercado al agregar un producto.
     */
    fun playBeepSuccess() {
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            Log.e("SoundEffects", "Error reproduciendo tono de éxito: ${e.message}")
        }
    }

    /**
     * Tono grave de error cuando no hay stock disponible.
     */
    fun playBeepError() {
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_SUP_ERROR, 150)
        } catch (e: Exception) {
            Log.e("SoundEffects", "Error reproduciendo tono de error: ${e.message}")
        }
    }
}

package com.mercata.pingworks

import android.content.Context
import android.media.MediaPlayer

class SoundPlayer(val context: Context) {

    fun playSwoosh() {
        val  mediaPlayer = MediaPlayer.create(context, R.raw.swoosh)
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
        mediaPlayer.start()
    }

}
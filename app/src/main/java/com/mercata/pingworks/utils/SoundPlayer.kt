package com.mercata.pingworks.utils

import android.content.Context
import android.media.MediaPlayer
import com.mercata.pingworks.R

class SoundPlayer(val context: Context) {

    fun playSwoosh() {
        val  mediaPlayer = MediaPlayer.create(context, R.raw.swoosh)
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
        mediaPlayer.start()
    }

}
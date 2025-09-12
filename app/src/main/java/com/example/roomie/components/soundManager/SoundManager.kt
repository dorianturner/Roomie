package com.example.roomie.components.soundManager

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.roomie.R

const val MAX_STREAMS = 4
enum class UISounds { SWIPE_LEFT, SWIPE_RIGHT }

class SoundManager(context: Context) {
    // SoundPool -> short audio clips
    private val soundPool: SoundPool
    private val loaded = mutableSetOf<Int>()
    private val soundIds = mutableMapOf<UISounds, Int>()

    init {
        val attributes = AudioAttributes.Builder()
            // Sonification -> UI feedback sounds
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(MAX_STREAMS)
            .build()

        soundIds[UISounds.SWIPE_RIGHT] = soundPool.load(context, R.raw.swipe_right, 1)
        soundIds[UISounds.SWIPE_LEFT] = soundPool.load(context, R.raw.swipe_left, 1)

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loaded += sampleId
            }
        }
    }

        private fun play(id: Int?) {
            if (id == null || id !in loaded) return
            val streamId = soundPool.play(id, 1f, 1f, 1, 0, 1f)
        }

        fun swipeRight() = play(soundIds[UISounds.SWIPE_RIGHT])
        fun swipeLeft() = play(soundIds[UISounds.SWIPE_LEFT])

        fun release() {
            soundPool.release()
        }
    }
package com.example.myapplication.ui.idcard.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackManager(context: Context) {

    private var vibrator: Vibrator? = null
    private var currentLevel = 0

    init {
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    fun update(isDetected: Boolean, score: Float) {
        if (!isDetected) {
            if (currentLevel != 0) {
                currentLevel = 0
                vibrator?.cancel()
            }
            return
        }

        val newLevel = when {
            score >= 0.90f -> 5
            score >= 0.75f -> 4
            score >= 0.60f -> 3
            score >= 0.40f -> 2
            score >= 0.20f -> 1
            else -> 0
        }

        if (newLevel != currentLevel) {
            currentLevel = newLevel
            vibrator?.cancel()

            if (newLevel > 0) {
                startPattern(newLevel)
            }
        }
    }

    private fun startPattern(level: Int) {
        vibrator?.let { vib ->
            if (!vib.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (level) {
                    5 -> vib.vibrate(VibrationEffect.createOneShot(10000L, 200))
                    4 -> vib.vibrate(VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 60, 50, 60, 50, 60), 0))
                    3 -> vib.vibrate(VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 120, 50, 120, 50, 120), 0))
                    2 -> vib.vibrate(VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 250, 50, 250, 50, 250), 0))
                    1 -> vib.vibrate(VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 500, 50, 500, 50, 500), 0))
                }
            } else {
                @Suppress("DEPRECATION")
                val pattern = when (level) {
                    5 -> longArrayOf(0, 10000)
                    4 -> longArrayOf(0, 50, 60)
                    3 -> longArrayOf(0, 50, 120)
                    2 -> longArrayOf(0, 50, 250)
                    else -> longArrayOf(0, 50, 500)
                }
                vib.vibrate(pattern, if (level == 5) -1 else 0)
            }
        }
    }

    fun stop() {
        currentLevel = 0
        vibrator?.cancel()
    }
}
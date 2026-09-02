package eu.emufii.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * The one slow clock for everything that moves continuously. Do not add a
 * second: the cost is how often we draw, not how much. Frozen when the system
 * has animations off.
 * pourquoi : docs/decisions/performance-rendu.md § One clock for everything that moves continuously
 */
@Composable
fun rememberSlowMillis(): Double {
    val running = rememberAnimationsEnabled()
    val millis = remember { mutableDoubleStateOf(FROZEN_MS) }
    LaunchedEffect(running) {
        if (!running) {
            millis.doubleValue = FROZEN_MS
            return@LaunchedEffect
        }
        // `delay`, not `withInfiniteAnimationFrameNanos`: the latter calls back on every
        // display frame (120 Hz on the Thor) and keeps the frame loop awake, so lowering
        // the beat from 30 to 12 changed nothing. Measured twice, 30 % both times.
        val origin = System.nanoTime()
        while (true) {
            millis.doubleValue = ((System.nanoTime() - origin) / 1_000_000).toDouble()
            delay(FRAME_INTERVAL_MS)
        }
    }
    return millis.doubleValue
}

/**
 * The background cycle runs nineteen seconds, a wave thirty-five, the cursor gradient
 * 1.8 s: at 120 Hz each advances a fraction of a pixel between frames. Not private:
 * the cursor derives its period from it so a step lands on a beat.
 */
internal const val FRAME_INTERVAL_MS = 1_000L / 12

/** Where everything freezes with animations off. Not zero: at zero the waves show nothing. */
private const val FROZEN_MS = 8_000.0

package eu.emufii.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
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
    if (!rememberAnimationsEnabled()) return FROZEN_MS

    val millis = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        // `delay`, not `withInfiniteAnimationFrameNanos`: the latter calls back on
        // every display frame (120 Hz on the Thor) even when nothing is written, and
        // keeps the frame loop awake, so lowering the beat from 30 to 12 changed
        // nothing at all. Measured twice, 30 % both times.
        val origin = System.nanoTime()
        while (true) {
            millis.longValue = (System.nanoTime() - origin) / 1_000_000
            delay(FRAME_INTERVAL_MS)
        }
    }
    return millis.longValue.toDouble()
}

/**
 * Twelve beats a second. The background cycle runs nineteen seconds, a wave
 * thirty-five, the cursor gradient 1.8 s: at 120 Hz each advances a fraction of
 * a pixel between frames.
 */
private const val FRAME_INTERVAL_MS = 1_000L / 12

/** Where everything freezes with animations off. Not zero: at zero the waves show nothing. */
private const val FROZEN_MS = 8_000.0

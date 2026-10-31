package eu.kanade.presentation.library.components

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.Image
import coil3.asDrawable
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.anime.model.AnimeCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

// ANZ -->
/**
 * The colours of one cover, used to paint the Home backdrop so it looks like the artwork.
 *
 * [lights] are four vivid colours taken from the cover itself (the most present first, then different
 * hues, then a bright tint of the main one); [top] / [mid] / [deep] are the dark base of the page in the
 * same hue family. An empty [lights] means the cover has no real colour (black and white art): the page
 * then falls back to the single vibrant colour it used before.
 *
 * [presence] is the share of the cover that really has colour (0..1): grey covers get calmer lights.
 */
@Immutable
internal class CoverTones(
    val coverVersion: Long,
    val presence: Float,
    val top: Color,
    val mid: Color,
    val deep: Color,
    val lights: List<Color>,
)

internal object CoverTonesCache {

    /** By anime id. Written on the main thread only, so the page recomposes as soon as a cover is read. */
    private val cache = mutableStateMapOf<Long, CoverTones>()

    /** Ids being read right now (main thread only). */
    private val inFlight = HashSet<Long>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun get(animeId: Long, coverVersion: Long): CoverTones? =
        cache[animeId]?.takeIf { it.coverVersion == coverVersion }

    /** Reads the colours of a cover that has just loaded, once per cover version. Call it on the main thread. */
    fun request(cover: AnimeCover, result: AsyncImagePainter.State.Success) {
        val id = cover.animeId
        val version = cover.lastModified
        if (get(id, version) != null || !inFlight.add(id)) return
        val image = result.result.image
        scope.launch {
            val tones = try {
                extract(image, version)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            }
            withContext(Dispatchers.Main) {
                inFlight.remove(id)
                if (tones != null) {
                    if (cache.size > 64) cache.clear()
                    cache[id] = tones
                }
            }
        }
    }

    private fun extract(image: Image, version: Long): CoverTones? {
        val context = Injekt.get<Application>()
        val original = when (image) {
            is BitmapImage -> image.bitmap
            else -> image.asDrawable(context.resources).toBitmap()
        }
        val software = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && original.config == Bitmap.Config.HARDWARE) {
            original.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        } else {
            original
        }
        // a tiny copy is plenty to find the main colours
        val scale = 100f / maxOf(software.width, software.height).coerceAtLeast(1)
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                software,
                (software.width * scale).toInt().coerceAtLeast(1),
                (software.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            software
        }
        val palette = Palette.from(small).maximumColorCount(24).generate()
        return tonesFrom(palette, version)
    }
}

private class Candidate(val hue: Float, val sat: Float, val value: Float, val score: Float)

private fun tonesFrom(palette: Palette, version: Long): CoverTones {
    val swatches = palette.swatches
    val total = swatches.sumOf { it.population }.coerceAtLeast(1)
    val hsv = FloatArray(3)
    val candidates = ArrayList<Candidate>()
    var presence = 0f
    for (swatch in swatches) {
        android.graphics.Color.colorToHSV(swatch.rgb, hsv)
        // greys, blacks and whites carry no hue
        if (hsv[1] < 0.22f || hsv[2] < 0.12f) continue
        val share = swatch.population.toFloat() / total
        presence += share
        // area counts (its square root, so small bright details still get a say), and so does colourfulness
        val score = sqrt(share) * (0.35f + hsv[1]) * (0.70f + 0.30f * hsv[2])
        candidates.add(Candidate(hsv[0], hsv[1], hsv[2], score))
    }
    if (candidates.isEmpty()) {
        return CoverTones(version, 0f, Color.Black, Color.Black, Color.Black, emptyList())
    }
    candidates.sortByDescending { it.score }

    // up to three clearly different hues, the most present first
    val picked = ArrayList<Candidate>(3)
    for (c in candidates) {
        if (picked.size == 3) break
        if (picked.all { hueDistance(it.hue, c.hue) >= 28f }) picked.add(c)
    }
    val main = picked[0]
    // a cover with a single hue: neighbouring hues of it instead
    while (picked.size < 3) {
        val offset = if (picked.size == 1) 24f else -24f
        picked.add(Candidate(wrapHue(main.hue + offset), main.sat, main.value, 0f))
    }

    val lights = listOf(
        vividColor(main.hue, main.sat, main.value),
        vividColor(picked[1].hue, picked[1].sat, picked[1].value),
        vividColor(picked[2].hue, picked[2].sat, picked[2].value),
        vividColor(main.hue, 0.50f, 1f),
    )

    // the dark base stays in the main hue, leaning a little towards the second one
    val delta = (((picked[1].hue - main.hue + 540f) % 360f) - 180f).coerceIn(-60f, 60f)
    val sat = (main.sat * 1.1f).coerceIn(0.60f, 0.90f)
    return CoverTones(
        coverVersion = version,
        presence = presence.coerceIn(0f, 1f),
        top = hsvColor(main.hue, sat, 0.56f),
        mid = hsvColor(wrapHue(main.hue + 0.35f * delta), sat * 0.95f, 0.42f),
        deep = hsvColor(wrapHue(main.hue + 0.60f * delta), (sat * 0.9f).coerceIn(0.50f, 0.85f), 0.32f),
        lights = lights,
    )
}

/** Same hue, but saturated and bright enough to glow, and never so bright that white text over it suffers. */
private fun vividColor(hue: Float, sat: Float, value: Float): Color {
    val s = (sat * 1.1f).coerceIn(0.62f, 1f)
    var v = value.coerceIn(0.75f, 0.90f)
    var color = hsvColor(hue, s, v)
    var steps = 0
    while (relativeLuma(color) > MaxLightLuma && steps < 10 && v > 0.45f) {
        v *= 0.93f
        color = hsvColor(hue, s, v)
        steps++
    }
    return color
}

private const val MaxLightLuma = 0.22f

private fun hsvColor(hue: Float, sat: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(wrapHue(hue), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))))

private fun wrapHue(hue: Float): Float = ((hue % 360f) + 360f) % 360f

private fun hueDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return minOf(d, 360f - d)
}

private fun relativeLuma(color: Color): Float {
    fun linear(c: Float): Float = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * linear(color.red) + 0.7152f * linear(color.green) + 0.0722f * linear(color.blue)
}
// ANZ <--

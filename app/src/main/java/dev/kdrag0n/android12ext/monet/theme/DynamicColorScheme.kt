package dev.kdrag0n.android12ext.monet.theme

import dev.kdrag0n.android12ext.monet.colors.Color
import dev.kdrag0n.android12ext.monet.colors.Oklab.Companion.toOklab
import dev.kdrag0n.android12ext.monet.colors.Oklch
import dev.kdrag0n.android12ext.monet.colors.Oklch.Companion.toOklch
import dev.kdrag0n.android12ext.monet.colors.Srgb
import timber.log.Timber

class DynamicColorScheme(
    targetColors: ColorScheme,
    primaryRgb8: Int,
) : ColorScheme() {
    private val primaryNeutral = Srgb(primaryRgb8).toLinearSrgb().toOklab().toOklch()

    // Boost chroma of primary color for accents.
    // This interpolates up to C=0.04 using a scaled hyperbolic tangent.
    private val primaryAccent = primaryNeutral.let { lch ->
<<<<<<< HEAD
        if (boostAccentChroma) {
            lch.copy(
                C = if (lch.C < 0.04) {
                    tanhScaled(lch.C, MIN_ACCENT_CHROMA, MIN_ACCENT_CHROMA_TANH_SCALE) * MIN_ACCENT_CHROMA
                } else {
                    lch.C
                }
            )
=======
        lch.copy(C = if (lch.C < 0.04) {
            tanhScaled(lch.C, MIN_ACCENT_CHROMA, MIN_ACCENT_CHROMA_TANH_SCALE) * MIN_ACCENT_CHROMA
>>>>>>> parent of f8f1e33 (monet: theme: Make accent chroma boosting optional)
        } else {
            lch.C
        })
    }

    init {
        Timber.i("Primary color: ${String.format("%06x", primaryRgb8)} => $primaryNeutral")
    }

    // Main background color. Tinted with the primary color.
    override val neutral1 = transformQuantizedColors(targetColors.neutral1, primaryNeutral)

    // Secondary background color. Slightly tinted with the primary color.
    override val neutral2 = transformQuantizedColors(targetColors.neutral2, primaryNeutral)

    // Main accent color. Generally, this is close to the primary color.
<<<<<<< HEAD
    override val accent1 = transformQuantizedColors(targetColors.accent1, primaryAccent)

    // Secondary accent color. Darker shades of accent1.
    override val accent2 = transformQuantizedColors(targetColors.accent2, primaryAccent)

=======
    override val accent1 = transformQuantizedColors(targetColors.accent1, primaryNeutral)
    // Secondary accent color. Darker shades of accent1.
    override val accent2 = transformQuantizedColors(targetColors.accent2, primaryNeutral)
>>>>>>> parent of f8f1e33 (monet: theme: Make accent chroma boosting optional)
    // Tertiary accent color. Primary color shifted to the next secondary color via hue offset.
    override val accent3 = transformQuantizedColors(targetColors.accent3, primaryNeutral) { lch ->
        lch.copy(h = lch.h + ACCENT3_HUE_SHIFT_DEGREES)
    }

    private fun transformQuantizedColors(
        colors: Map<Int, Color>,
        primary: Oklch,
        colorFilter: (Oklch) -> Oklch = { it },
    ): Map<Int, Color> {
        return colors.map { (shade, color) ->
            val target = color as? Oklch
                ?: color.toLinearSrgb().toOklab().toOklch()
            val new = colorFilter(transformColor(target, primary))
            val newColor = new.toOklab().toLinearSrgb().toSrgb()

            val newRgb8 = newColor.quantize8()
            Timber.d("Transform: [$shade] $target => $new => ${String.format("%06x", newRgb8)}")
            shade to newColor
        }.toMap()
    }

    private fun transformColor(target: Oklch, primary: Oklch): Oklch {
        return Oklch(
            // Keep target luminance. Themes should never need to change it.
            L = target.L,
            // Allow colorless gray.
            C = primary.C.coerceIn(0.0, target.C),
            // Use the primary color's hue, since it's the most prominent feature of the theme.
            h = primary.h,
        )
    }

    companion object {
        // Hue shift for the tertiary accent color (accent3), in degrees.
        // 60 degrees = shifting by a secondary color
        private const val ACCENT3_HUE_SHIFT_DEGREES = 60.0

        // Minimum target chroma for accents.
        // This is not a hard clamp; we interpolate to it with tanh.
        private const val MIN_ACCENT_CHROMA = 0.04

        // Scale to target tanhScaled(MIN_ACCENT_CHROMA) = 1.0
        // This value was found empirically. There doesn't seem to be an analytical way to do this.
        private const val MIN_ACCENT_CHROMA_TANH_SCALE = 1 / 0.21
    }
}

package app.screenfilters

/** Everything the overlay can do is alpha-blended on top of the game, so a preset is just layers. */
data class Preset(
    val name: String,
    val topTint: Int,     // ARGB, color layer at top of screen
    val bottomTint: Int,  // ARGB, color layer at bottom of screen
    val vignette: Float,  // 0..1 edge darkening (adds depth / contrast feel)
    val glow: Float,      // 0..1 soft center highlight lift (pseudo-HDR bloom)
    val dim: Float        // 0..1 flat black layer (blacks / contrast)
)

object Presets {
    val all = listOf(
        Preset("Normal",      0x00000000, 0x00000000, 0.00f, 0.00f, 0.00f),
        Preset("4K Quality",  0x0A7FB2FF, 0x08FFE2C0, 0.14f, 0.05f, 0.04f),
        Preset("Cool",        0x2A4F9BFF, 0x1A3F7FFF, 0.10f, 0.02f, 0.03f),
        Preset("Warm",        0x2AFFA040, 0x24FF8A30, 0.10f, 0.03f, 0.03f),
        // DLSS5: cool shadows up top, warm highlights below (teal/orange split), strong vignette for
        // depth, center glow for HDR pop, slight dim to deepen blacks.
        Preset("DLSS5",       0x1C3A7BFF, 0x22FFAA55, 0.30f, 0.09f, 0.07f)
    )
    fun byName(n: String) = all.firstOrNull { it.name == n } ?: all[0]
}

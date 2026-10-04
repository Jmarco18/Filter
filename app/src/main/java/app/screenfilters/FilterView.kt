package app.screenfilters

import android.content.Context
import android.graphics.*
import android.view.View

class FilterView(c: Context) : View(c) {
    var preset: Preset = Presets.all[0]; set(v) { field = v; invalidate() }
    var intensity = 1f; set(v) { field = v.coerceIn(0f, 1.5f); invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun scaled(argb: Int): Int {
        val a = ((argb ushr 24) * intensity).toInt().coerceIn(0, 255)
        return (a shl 24) or (argb and 0xFFFFFF)
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        val pr = preset
        // 1) teal/orange split tint
        p.shader = LinearGradient(0f, 0f, 0f, h, scaled(pr.topTint), scaled(pr.bottomTint), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        // 2) center glow (soft highlight lift)
        if (pr.glow > 0f) {
            val a = (255 * pr.glow * intensity).toInt().coerceIn(0, 255)
            p.shader = RadialGradient(w / 2, h / 2, maxOf(w, h) * 0.55f,
                intArrayOf(Color.argb(a, 255, 250, 240), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, w, h, p)
        }
        // 3) vignette (edge depth)
        if (pr.vignette > 0f) {
            val a = (255 * pr.vignette * intensity).toInt().coerceIn(0, 255)
            p.shader = RadialGradient(w / 2, h / 2, maxOf(w, h) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(a, 0, 0, 0)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, w, h, p)
        }
        // 4) black level
        if (pr.dim > 0f) {
            p.shader = null
            p.color = Color.argb((255 * pr.dim * intensity).toInt().coerceIn(0, 255), 0, 0, 0)
            cv.drawRect(0f, 0f, w, h, p)
        }
    }
}

package app.screenfilters

import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private data class Game(val label: String, val pkg: String) { override fun toString() = label }
    private var games = listOf<Game>()
    private lateinit var spinner: Spinner
    private var preset = "DLSS5"
    private var intensity = 1f
    private var fps = 0f
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        val sv = ScrollView(this).apply { addView(col) }
        setContentView(sv)

        fun label(t: String) = TextView(this).apply { text = t; textSize = 13f; setPadding(0, 28, 0, 8) }.also(col::addView)

        label("GAME (NBA 2K found on this phone)")
        spinner = Spinner(this); col.addView(spinner)
        col.addView(Button(this).apply { text = "LAUNCH SELECTED GAME + FILTER"; setOnClickListener { launchGame() } })

        label("LOOK")
        val row = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        Presets.all.forEach { p ->
            row.addView(RadioButton(this).apply {
                text = p.name; id = View.generateViewId(); isChecked = p.name == preset
                setOnClickListener { preset = p.name; apply() }
            })
        }
        col.addView(row)

        label("INTENSITY")
        col.addView(SeekBar(this).apply {
            max = 150; progress = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { intensity = p / 100f; if (u) apply() }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        label("REFRESH RATE HINT")
        val rates = floatArrayOf(0f, 60f, 90f, 120f)
        val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        rates.forEach { r ->
            rg.addView(RadioButton(this).apply {
                text = if (r == 0f) "Auto" else "${r.toInt()}Hz"; id = View.generateViewId(); isChecked = r == 0f
                setOnClickListener { fps = r; apply() }
            })
        }
        col.addView(rg)

        col.addView(Button(this).apply { text = "STOP FILTER"; setOnClickListener {
            startService(Intent(this@MainActivity, FilterService::class.java).setAction("STOP")) } })
        status = TextView(this).apply { gravity = Gravity.CENTER; setPadding(0, 30, 0, 0) }; col.addView(status)
    }

    override fun onResume() { super.onResume(); loadGames(); status.text = permText() }

    private fun loadGames() {
        val pm = packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        games = pm.queryIntentActivities(i, 0).map { Game(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.label.contains("2K", true) || it.pkg.contains("2k", true) }.distinctBy { it.pkg }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            if (games.isEmpty()) listOf(Game("No NBA 2K found", "")) else games)
    }

    private fun hasUsage(): Boolean {
        val a = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return a.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }
    private fun permText() = "Overlay: ${if (Settings.canDrawOverlays(this)) "OK" else "needed"}  |  Usage access: ${if (hasUsage()) "OK" else "needed"}"

    private fun apply(target: String = (spinner.selectedItem as? Game)?.pkg ?: "") {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return }
        if (target.isNotEmpty() && !hasUsage()) {
            Toast.makeText(this, "Enable Usage Access for Screen Filters", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); return }
        startForegroundService(Intent(this, FilterService::class.java)
            .putExtra("preset", preset).putExtra("intensity", intensity).putExtra("fps", fps).putExtra("target", target))
    }

    private fun launchGame() {
        val g = spinner.selectedItem as? Game ?: return
        if (g.pkg.isEmpty()) return
        apply(g.pkg)
        if (!Settings.canDrawOverlays(this) || !hasUsage()) return
        packageManager.getLaunchIntentForPackage(g.pkg)?.let { startActivity(it) }
    }
}

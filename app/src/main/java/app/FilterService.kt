package app.screenfilters

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager

/**
 * Draws the filter overlay. If a target game package is set, the overlay only shows while that game
 * is in the foreground (detected via Usage Access), like SHADER NI LOKO does.
 */
class FilterService : Service() {
    private lateinit var wm: WindowManager
    private var view: FilterView? = null
    private var params: WindowManager.LayoutParams? = null
    private var target: String? = null
    private var visible = false
    private var lastFg: String? = null
    private val h = Handler(Looper.getMainLooper())

    private val poll = object : Runnable {
        override fun run() {
            val t = target
            if (t != null) {
                foreground()?.let { lastFg = it }
                setVisible(lastFg == t)
            } else setVisible(true)
            h.postDelayed(this, 1000)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        startFg()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (view == null) {
            view = FilterView(this)
            params = WindowManager.LayoutParams(
                -1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT)
            wm.addView(view, params); visible = true
            h.post(poll)
        }
        i?.let {
            view?.preset = Presets.byName(it.getStringExtra("preset") ?: "Normal")
            view?.intensity = it.getFloatExtra("intensity", 1f)
            target = it.getStringExtra("target")?.takeIf { s -> s.isNotEmpty() }
            val fps = it.getFloatExtra("fps", 0f)
            params?.preferredRefreshRate = fps   // 0 = system default; a hint, the display decides
            try { wm.updateViewLayout(view, params) } catch (_: Exception) {}
        }
        return START_STICKY
    }

    private fun setVisible(v: Boolean) {
        if (v == visible) return
        visible = v
        view?.visibility = if (v) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun foreground(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val ev = usm.queryEvents(now - 10_000, now)
        val e = UsageEvents.Event(); var pkg: String? = null
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) pkg = e.packageName
        }
        return pkg
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("sf", "Screen Filters", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0, Intent(this, FilterService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "sf").setContentTitle("Screen filter active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null; super.onDestroy()
    }
}

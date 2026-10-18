package com.pindou.studio.data

import android.content.Context

/** 简单设置存储（SharedPreferences）。 */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("pindou_settings", Context.MODE_PRIVATE)

    var lastPaletteId: String
        get() = prefs.getString("lastPaletteId", "") ?: ""
        set(value) = prefs.edit().putString("lastPaletteId", value).apply()

    var showCodes: Boolean
        get() = prefs.getBoolean("showCodes", false)
        set(value) = prefs.edit().putBoolean("showCodes", value).apply()

    var showGrid: Boolean
        get() = prefs.getBoolean("showGrid", true)
        set(value) = prefs.edit().putBoolean("showGrid", value).apply()

    var showMajorGrid: Boolean
        get() = prefs.getBoolean("showMajorGrid", true)
        set(value) = prefs.edit().putBoolean("showMajorGrid", value).apply()

    var displayMode: String
        get() = prefs.getString("displayMode", "standard") ?: "standard"
        set(value) = prefs.edit().putString("displayMode", value).apply()

    var roundBeads: Boolean
        get() = prefs.getBoolean("roundBeads", false)
        set(value) = prefs.edit().putBoolean("roundBeads", value).apply()

    var showRuler: Boolean
        get() = prefs.getBoolean("showRuler", true)
        set(value) = prefs.edit().putBoolean("showRuler", value).apply()

    var lastProjectId: String
        get() = prefs.getString("lastProjectId", "") ?: ""
        set(value) = prefs.edit().putString("lastProjectId", value).apply()

    var introShown: Boolean
        get() = prefs.getBoolean("introShown", false)
        set(value) = prefs.edit().putBoolean("introShown", value).apply()
}

package com.liquor.ledger

// Imports Needed for Theme Colors
import android.app.Activity
import android.graphics.Color

/*
 * ThemeManager
 *
 * Central place for Dark Mode and Colorblind Mode colors.
 */
object ThemeManager {

    // SHARED PREFERENCES FILE USED BY SETTINGS PAGE
    private const val PREFS_NAME = "settings_prefs"

    // SETTING KEY FOR COLORBLIND MODE
    private const val KEY_COLORBLIND_MODE = "colorblind_mode"

    // SETTING KEY FOR DARK MODE
    private const val KEY_DARK_MODE = "dark_mode"

    // CHECKS IF DARK MODE IS ENABLED
    fun isDarkMode(activity: Activity): Boolean {
        return activity
            .getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    // CHECKS IF COLORBLIND MODE IS ENABLED
    fun isColorblindMode(activity: Activity): Boolean {
        return activity
            .getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
            .getBoolean(KEY_COLORBLIND_MODE, false)
    }

    // MAIN PAGE BACKGROUND COLOR
    fun pageBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(38, 38, 38)
        } else {
            Color.WHITE
        }
    }

    // SECTION OR TABLE HEADER BACKGROUND COLOR
    fun sectionBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(48, 48, 48)
        } else {
            Color.rgb(248, 249, 250)
        }
    }

    // INPUT FIELD BACKGROUND COLOR
    fun inputBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(60, 60, 60)
        } else {
            Color.rgb(243, 244, 246)
        }
    }

    // MAIN TEXT COLOR
    fun primaryText(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    // SECONDARY TEXT COLOR
    fun secondaryText(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.LTGRAY
        } else {
            Color.rgb(55, 65, 81)
        }
    }

    // MUTED TEXT OR HINT TEXT COLOR
    fun mutedText(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(180, 180, 180)
        } else {
            Color.GRAY
        }
    }

    // PRIMARY BUTTON OR LINK COLOR
    fun primaryAction(activity: Activity): Int {
        return if (isColorblindMode(activity)) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(45, 95, 255)
        }
    }

    // SUCCESS COLOR USED FOR POSITIVE/ACTION STATUS COLORS
    fun positive(activity: Activity): Int {
        return if (isColorblindMode(activity)) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(34, 197, 94)
        }
    }

    // WARNING COLOR USED FOR HOTLINE OR LOW-STOCK STYLE COLORS
    fun warning(activity: Activity): Int {
        return if (isColorblindMode(activity)) {
            Color.rgb(230, 159, 0)
        } else {
            Color.rgb(202, 138, 4)
        }
    }

    // ERROR OR EMERGENCY COLOR
    fun negative(activity: Activity): Int {
        return if (isColorblindMode(activity)) {
            Color.rgb(213, 94, 0)
        } else {
            Color.rgb(220, 38, 38)
        }
    }

    // CARD BACKGROUND COLOR USED FOR INNER CARDS
    fun cardBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(60, 60, 60)
        } else {
            Color.rgb(250, 250, 250)
        }
    }

    // EMERGENCY PROTOCOL BOX BACKGROUND
    fun emergencyBackground(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(65, 45, 45)
        } else {
            Color.rgb(255, 248, 248)
        }
    }

    // DIVIDER COLOR
    fun divider(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(80, 80, 80)
        } else {
            Color.rgb(229, 231, 235)
        }
    }

    // MUTED BUTTON COLOR
    fun mutedButton(activity: Activity): Int {
        return if (isDarkMode(activity)) {
            Color.rgb(90, 90, 90)
        } else {
            Color.rgb(107, 114, 128)
        }
    }
}
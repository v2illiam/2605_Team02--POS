package com.liquor.ledger

// IMPORTS NEEDED FOR UI COMPONENTS
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/*
 * SettingsPage
 *
 * Builds the Settings screen programmatically.
 *
 * This is NOT an Activity.
 * MainActivity creates this page and places it inside the main content box.
 *
 * onThemeChanged is a callback.
 * It lets SettingsPage notify MainActivity when Dark Mode or Colorblind Mode changes,
 * so the full app shell can refresh.
 */
class SettingsPage(
    private val context: Context,
    private val onThemeChanged: (() -> Unit)? = null
) {

    /*
     * SharedPreferences
     *
     * Saves small settings locally on the device.
     * Each toggle is saved as true or false.
     */
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    /*
     * Activity reference
     *
     * ThemeManager needs an Activity so it can read the same saved settings.
     */
    private val activity = context as Activity

    /*
     * Setting keys
     *
     * These names must match the keys MainActivity and other pages read.
     */
    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"
    private val KEY_LOW_STOCK_ALERTS = "low_stock_alerts"
    private val KEY_SALES_ALERTS = "sales_alerts"
    private val KEY_SOUND_NOTIFICATIONS = "sound_notifications"
    private val KEY_AUTO_PRINT_RECEIPTS = "auto_print_receipts"

    /*
     * build()
     *
     * Creates the full Settings page layout.
     */
    fun build(): LinearLayout {

        // MAIN VERTICAL CONTAINER
        val layout = LinearLayout(context)

        // STACK ITEMS TOP TO BOTTOM
        layout.orientation = LinearLayout.VERTICAL

        // BUILD THE PAGE CONTENT
        buildSettingsContent(layout)

        // RETURN COMPLETED SETTINGS PAGE
        return layout
    }

    /*
     * buildSettingsContent()
     *
     * Builds or rebuilds the Settings page.
     *
     * We rebuild when Dark Mode or Colorblind Mode changes
     * so this page visually updates immediately.
     */
    private fun buildSettingsContent(layout: LinearLayout) {

        // CLEAR OLD SETTINGS VIEWS BEFORE REBUILDING
        layout.removeAllViews()

        // APPLY SETTINGS PAGE BACKGROUND FROM THEME MANAGER
        layout.setBackgroundColor(ThemeManager.pageBackground(activity))

        // ADD INNER PADDING
        layout.setPadding(dp(20), dp(20), dp(20), dp(20))

        // PAGE TITLE
        layout.addView(makeTitle("Settings"))

        // ALL SETTINGS TOGGLES
        layout.addView(makeFunctionalSwitch("Colorblind Mode", KEY_COLORBLIND_MODE, layout))
        layout.addView(makeFunctionalSwitch("Dark Mode", KEY_DARK_MODE, layout))
        layout.addView(makeFunctionalSwitch("Low Stock Alerts", KEY_LOW_STOCK_ALERTS, layout))
        layout.addView(makeFunctionalSwitch("Sales Alerts", KEY_SALES_ALERTS, layout))
        layout.addView(makeFunctionalSwitch("Sound Notifications", KEY_SOUND_NOTIFICATIONS, layout))
        layout.addView(makeFunctionalSwitch("Auto Print Receipts", KEY_AUTO_PRINT_RECEIPTS, layout))

        // TEST PRINTER BUTTON
        val testPrinterButton = Button(context)
        testPrinterButton.text = "Test Printer"

        // STYLE TEST PRINTER BUTTON USING THEME MANAGER
        testPrinterButton.setBackgroundColor(ThemeManager.primaryAction(activity))
        testPrinterButton.setTextColor(Color.WHITE)

        // SIMULATED TEST PRINTER FUNCTIONALITY
        testPrinterButton.setOnClickListener {
            testPrinter()
        }

        // ADD BUTTON TO SETTINGS PAGE
        layout.addView(testPrinterButton)
    }

    /*
     * makeTitle()
     *
     * Creates the Settings page title.
     */
    private fun makeTitle(text: String): TextView {

        val title = TextView(context)
        title.text = text
        title.textSize = 28f
        title.gravity = Gravity.START
        title.setPadding(0, 0, 0, dp(20))

        // APPLY TITLE COLOR FROM THEME MANAGER
        title.setTextColor(ThemeManager.primaryText(activity))

        return title
    }

    /*
     * makeFunctionalSwitch()
     *
     * Creates a switch that:
     * 1. Loads its saved ON/OFF state
     * 2. Saves whenever the user changes it
     * 3. Runs behavior for that setting
     */
    private fun makeFunctionalSwitch(
        text: String,
        settingKey: String,
        parentLayout: LinearLayout
    ): Switch {

        val settingSwitch = Switch(context)

        // SWITCH LABEL
        settingSwitch.text = text

        // STYLE
        settingSwitch.textSize = 18f
        settingSwitch.setPadding(0, dp(10), 0, dp(10))

        // TEXT COLOR FROM THEME MANAGER
        settingSwitch.setTextColor(ThemeManager.secondaryText(activity))

        // LOAD SAVED VALUE
        settingSwitch.isChecked = prefs.getBoolean(settingKey, false)

        // SAVE VALUE WHEN SWITCH CHANGES
        settingSwitch.setOnCheckedChangeListener { _, isChecked ->

            prefs.edit()
                .putBoolean(settingKey, isChecked)
                .apply()

            handleSettingChanged(settingKey, isChecked, parentLayout)
        }

        return settingSwitch
    }

    /*
     * handleSettingChanged()
     *
     * Runs the correct behavior when a setting is toggled.
     */
    private fun handleSettingChanged(
        settingKey: String,
        isChecked: Boolean,
        parentLayout: LinearLayout
    ) {
        when (settingKey) {

            KEY_COLORBLIND_MODE -> {
                showToast(
                    if (isChecked) {
                        "Colorblind Mode enabled"
                    } else {
                        "Colorblind Mode disabled"
                    }
                )

                // Refresh SettingsPage visuals
                buildSettingsContent(parentLayout)

                // Tell MainActivity to refresh the full app shell
                onThemeChanged?.invoke()
            }

            KEY_DARK_MODE -> {
                showToast(
                    if (isChecked) {
                        "Dark Mode enabled"
                    } else {
                        "Dark Mode disabled"
                    }
                )

                // Refresh SettingsPage visuals
                buildSettingsContent(parentLayout)

                // Tell MainActivity to refresh the full app shell
                onThemeChanged?.invoke()
            }

            KEY_LOW_STOCK_ALERTS -> {
                showToast(
                    if (isChecked) {
                        "Low Stock Alerts enabled"
                    } else {
                        "Low Stock Alerts disabled"
                    }
                )
            }

            KEY_SALES_ALERTS -> {
                showToast(
                    if (isChecked) {
                        "Sales Alerts enabled"
                    } else {
                        "Sales Alerts disabled"
                    }
                )
            }

            KEY_SOUND_NOTIFICATIONS -> {
                showToast(
                    if (isChecked) {
                        "Sound Notifications enabled"
                    } else {
                        "Sound Notifications disabled"
                    }
                )

                if (isChecked) {
                    playNotificationSound()
                }
            }

            KEY_AUTO_PRINT_RECEIPTS -> {
                showToast(
                    if (isChecked) {
                        "Auto Print Receipts enabled"
                    } else {
                        "Auto Print Receipts disabled"
                    }
                )
            }
        }
    }

    /*
     * testPrinter()
     *
     * Simulates printer behavior for now.
     *
     * Later, this is where actual printer integration can go.
     */
    private fun testPrinter() {

        val autoPrintEnabled = prefs.getBoolean(KEY_AUTO_PRINT_RECEIPTS, false)
        val soundEnabled = prefs.getBoolean(KEY_SOUND_NOTIFICATIONS, false)

        if (soundEnabled) {
            playNotificationSound()
        }

        if (autoPrintEnabled) {
            showToast("Auto Print is ON: sending test receipt...")
        } else {
            showToast("Printer test started")
        }
    }

    /*
     * playNotificationSound()
     *
     * Plays a short system beep.
     */
    private fun playNotificationSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            showToast("Unable to play notification sound")
        }
    }

    /*
     * showToast()
     *
     * Shows a short popup message.
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /*
     * dp()
     *
     * Converts dp into pixels.
     */
    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
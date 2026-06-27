package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var sidebarScrollView: ScrollView
    private lateinit var mainContent: LinearLayout
    private lateinit var header: TextView
    private lateinit var contentBox: LinearLayout

    private var reportsExpanded = false
    private var selectedPage = "POS / Register"

    private val darkBlue = Color.rgb(16, 30, 55)
    private val lightGray = Color.rgb(245, 245, 245)

    /*
     * SharedPreferences
     *
     * Reads the same saved settings that SettingsPage writes.
     * This allows Dark Mode and Colorblind Mode to affect MainActivity.
     */
    private val prefs by lazy {
        getSharedPreferences("settings_prefs", MODE_PRIVATE)
    }

    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ROOT LAYOUT
        root = LinearLayout(this)
        root.orientation = LinearLayout.HORIZONTAL
        root.setBackgroundColor(getRootBackgroundColor())

        // SIDEBAR
        sidebarScrollView = ScrollView(this)
        sidebarScrollView.setBackgroundColor(getSidebarColor())
        sidebarScrollView.isVerticalScrollBarEnabled = true
        sidebarScrollView.isScrollbarFadingEnabled = false

        sidebar = LinearLayout(this)
        sidebar.orientation = LinearLayout.VERTICAL
        sidebar.setBackgroundColor(getSidebarColor())

        sidebarScrollView.addView(sidebar)

        // SIDEBAR SIZE
        val sidebarParams = LinearLayout.LayoutParams(
            dp(230),
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // MAIN CONTENT AREA
        mainContent = LinearLayout(this)
        mainContent.orientation = LinearLayout.VERTICAL
        mainContent.setBackgroundColor(getMainBackgroundColor())

        // MAIN CONTENT SIZE
        val mainParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        // ADD SIDEBAR AND CONTENT TO ROOT
        root.addView(sidebarScrollView, sidebarParams)

        root.addView(mainContent, mainParams)

        // SET SCREEN CONTENT
        setContentView(root)

        // BUILD SIDEBAR
        buildSidebar()

        // DEFAULT PAGE
        loadPage("POS / Register")
    }

    /*
     * buildSidebar()
     *
     * Builds the sidebar navigation.
     * This is used on startup and whenever Reports is expanded/collapsed.
     */
    private fun buildSidebar() {

        // CLEAR OLD SIDEBAR ITEMS
        sidebar.removeAllViews()

        // APPLY CURRENT SIDEBAR COLOR
        sidebar.setBackgroundColor(getSidebarColor())

        // APP TITLE
        val title = TextView(this)
        title.text = "Liquor\nLedger"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(24), dp(48), dp(16), dp(40))

        sidebar.addView(title)

        // SIDEBAR TABS
        sidebar.addView(makeTab("POS / Register"))
        sidebar.addView(makeTab("Inventory"))

        if (SessionManager.currentEmployee?.position == "Manager") {
            sidebar.addView(makeTab("Purchase Orders"))
        }

        sidebar.addView(makeReportsTab())

        if (reportsExpanded) {
            sidebar.addView(makeTab("   Sales Analytics"))
            sidebar.addView(makeTab("   Sales Report"))
            sidebar.addView(makeTab("   Inventory Report"))
            sidebar.addView(makeTab("   Inventory Alert"))
        }

        // Timecard tab
        sidebar.addView(makeTab("Timecard"))

        sidebar.addView(makeTab("User Info"))

        sidebar.addView(makeTab("Emergency Contacts"))

        sidebar.addView(makeTab("Settings"))

        // SPACER — pushes logout button to the bottom
        val spacer = android.view.View(this)
        val spacerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        sidebar.addView(spacer, spacerParams)

        // LOGOUT BUTTON — always visible at bottom of sidebar
        val logoutBtn = makeTab("Logout")
        logoutBtn.setTextColor(Color.rgb(239, 68, 68))
        logoutBtn.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            SessionManager.clear()
            val intent = android.content.Intent(this, LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        sidebar.addView(logoutBtn)

    }

    /*
     * makeTab()
     *
     * Creates a sidebar tab.
     */
    private fun makeTab(text: String): TextView {

        val tab = TextView(this)

        tab.text = text
        tab.textSize = 16f
        tab.gravity = Gravity.CENTER_VERTICAL
        tab.setPadding(dp(28), dp(18), dp(16), dp(18))
        tab.setTextColor(Color.WHITE)

        if (text == selectedPage) {
            tab.setBackgroundColor(Color.rgb(45, 95, 255))
            tab.setTextColor(Color.WHITE)
        } else {
            tab.setBackgroundColor(Color.TRANSPARENT)
            tab.setTextColor(Color.WHITE)
        }

        tab.setOnClickListener {
            loadPage(text)
        }

        return tab
    }

    /*
     * makeReportsTab()
     *
     * Creates the Reports tab.
     * Clicking it expands/collapses the report submenu.
     */
    private fun makeReportsTab(): TextView {

        val tab = TextView(this)

        tab.text = "Reports"
        tab.textSize = 16f
        tab.gravity = Gravity.CENTER_VERTICAL
        tab.setPadding(dp(28), dp(18), dp(16), dp(18))
        tab.setTextColor(Color.WHITE)

        tab.setOnClickListener {
            reportsExpanded = !reportsExpanded
            buildSidebar()
        }

        return tab
    }

    /*
     * loadPage()
     *
     * Clears the old content and loads the selected page.
     */
    private fun loadPage(pageName: String) {
        selectedPage = pageName
        buildSidebar()

        // CLEAR OLD PAGE
        mainContent.removeAllViews()

        // APPLY CURRENT APP THEME
        applyAppTheme()

        // PAGE HEADER
        header = TextView(this)
        header.text = pageName
        header.textSize = 32f
        header.setTextColor(getHeaderTextColor())
        header.setPadding(dp(32), dp(32), 0, dp(16))

        // CONTENT BOX
        contentBox = LinearLayout(this)
        contentBox.orientation = LinearLayout.VERTICAL
        contentBox.setBackgroundColor(getContentBoxColor())
        contentBox.setPadding(dp(20), dp(20), dp(20), dp(20))

        // CONTENT BOX SIZE
        val boxParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        // CONTENT BOX MARGINS
        boxParams.setMargins(dp(24), dp(10), dp(24), dp(48))

        // ADD HEADER AND CONTENT BOX
        mainContent.addView(header)
        mainContent.addView(contentBox, boxParams)

        if (pageName == "POS / Register") {

            val posPage = POSPage(this)

            contentBox.addView(
                posPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        } else if (pageName == "Reports" || pageName == "   Sales Analytics") {

            val reportsPage = ReportsPage(this)

            contentBox.addView(
                reportsPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        }
        else if (pageName == "   Sales Report") {

            val salesReportPage = SalesReportPage(this)

            contentBox.addView(
                salesReportPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        }
        else if (pageName == "   Inventory Report") {

            val inventoryReportPage = InventoryReportPage(this)

            contentBox.addView(
                inventoryReportPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        }
        else if (pageName == "   Inventory Alert") {

            val inventoryAlertPage = InventoryAlertPage(this)

            contentBox.addView(
                inventoryAlertPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        }
        else if (pageName == "Inventory") {

            val inventoryPage = InventoryPage(this)

            contentBox.addView(
                inventoryPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

        } else if (pageName == "Purchase Orders") {

            val purchaseOrdersPage = PurchaseOrdersPage(this)

            contentBox.addView(
                purchaseOrdersPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

            else if (pageName == "Timecard") {
                val timecardPage = TimecardPage(this)
                contentBox.addView(
                    timecardPage.build(),
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }

        else if (pageName == "User Info") {
            val userInfoPage = UserInfoPage(this)
            contentBox.addView(
                userInfoPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        else if (pageName == "Emergency Contacts") {

            val emergencyContactsPage = EmergencyContactsPage(this)

            contentBox.addView(
                emergencyContactsPage.build(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

            else if (pageName == "Settings") {

            /*
             * SettingsPage callback
             *
             * When Dark Mode or Colorblind Mode changes,
             * SettingsPage can tell MainActivity to refresh.
             */
            val settingsPage = SettingsPage(this) {
                applyAppTheme()
                buildSidebar()
                loadPage("Settings")
            }

            contentBox.addView(settingsPage.build())

        } else {

            val pageText = TextView(this)
            pageText.text = "$pageName screen will go here"
            pageText.textSize = 18f
            pageText.setTextColor(getBodyTextColor())
            pageText.setPadding(dp(20), dp(20), dp(20), dp(20))

            contentBox.addView(pageText)
        }
    }

    /*
     * Checks if Dark Mode is enabled.
     */
    private fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    /*
     * Checks if Colorblind Mode is enabled.
     */
    private fun isColorblindModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_COLORBLIND_MODE, false)
    }

    /*
     * Root background color.
     */
    private fun getRootBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(18, 18, 18)
        } else {
            Color.WHITE
        }
    }

    /*
     * Main content background color.
     */
    private fun getMainBackgroundColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(24, 24, 24)
        } else {
            Color.WHITE
        }
    }

    /*
     * Sidebar color.
     *
     * Colorblind Mode uses a stronger accessible blue.
     */
    private fun getSidebarColor(): Int {
        return when {
            isColorblindModeEnabled() -> Color.rgb(0, 90, 150)
            isDarkModeEnabled() -> Color.rgb(10, 20, 35)
            else -> darkBlue
        }
    }

    /*
     * Content box color.
     */
    private fun getContentBoxColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.rgb(38, 38, 38)
        } else {
            lightGray
        }
    }

    /*
     * Header text color.
     */
    private fun getHeaderTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    /*
     * Body/placeholder text color.
     */
    private fun getBodyTextColor(): Int {
        return if (isDarkModeEnabled()) {
            Color.LTGRAY
        } else {
            Color.DKGRAY
        }
    }

    /*
     * applyAppTheme()
     *
     * Applies Dark Mode / Colorblind Mode to the app shell.
     */
    private fun applyAppTheme() {
        root.setBackgroundColor(getRootBackgroundColor())
        sidebarScrollView.setBackgroundColor(getSidebarColor())
        mainContent.setBackgroundColor(getMainBackgroundColor())

        if (::header.isInitialized) {
            header.setTextColor(getHeaderTextColor())
        }

        if (::contentBox.isInitialized) {
            contentBox.setBackgroundColor(getContentBoxColor())
        }
    }

    /*
     * Converts dp to pixels.
     */
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

package com.liquor.ledger

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

import java.util.Calendar

import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ReportsPage(private val activity: Activity) {



    private val db = FirebaseManager.db

    /*
     * SharedPreferences
     *
     * Reads the same settings saved from SettingsPage.
     * This allows ReportsPage to respond to Dark Mode
     * and Colorblind Mode.
     */
    private val prefs = activity.getSharedPreferences("settings_prefs", Activity.MODE_PRIVATE)

    /*
     * Setting keys
     *
     * These must match the keys used in SettingsPage.
     */
    private val KEY_COLORBLIND_MODE = "colorblind_mode"
    private val KEY_DARK_MODE = "dark_mode"
    private lateinit var totalRevenueCard: TextView
    private lateinit var transactionCountCard: TextView
    private lateinit var taxCollectedCard: TextView
    private var selectedDateFilter = "Today"

    fun build(): LinearLayout {

        val scrollView = ScrollView(activity)
        scrollView.setBackgroundColor(getPageBackgroundColor())

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL
        page.setBackgroundColor(getPageBackgroundColor())
        page.setPadding(dp(20), dp(20), dp(20), dp(20))

        // REPORT TITLE
        val title = TextView(activity)
        title.text = "Sales Analytics"
        title.textSize = 22f
        title.setTextColor(getPrimaryTextColor())

        // REPORT DESCRIPTION
        val description = TextView(activity)
        description.text = "Comprehensive sales data and revenue analysis."
        description.textSize = 14f
        description.setTextColor(getSecondaryTextColor())
        description.setPadding(0, dp(8), 0, dp(20))

        val filterRow = LinearLayout(activity)
        filterRow.orientation = LinearLayout.HORIZONTAL

        val filterParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        filterParams.setMargins(0, 0, 0, dp(20))

        val todayButton = TextView(activity)
        todayButton.text = "Today"

        val thirtyDayButton = TextView(activity)
        thirtyDayButton.text = "30 Days"

        val ninetyDayButton = TextView(activity)
        ninetyDayButton.text = "90 Days"

        val ytdButton = TextView(activity)
        ytdButton.text = "YTD"

        val customButton = TextView(activity)
        customButton.text = "Custom"

        val filterButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        listOf(
            todayButton,
            thirtyDayButton,
            ninetyDayButton,
            ytdButton,
            customButton
        ).forEach {

            it.gravity = Gravity.CENTER
            it.textSize = 14f
            it.setTextColor(Color.WHITE)
            it.setBackgroundColor(getPrimaryActionColor())
            it.setPadding(0, dp(10), 0, dp(10))

            val buttonParams = LinearLayout.LayoutParams(filterButtonParams)
            buttonParams.setMargins(dp(4), 0, dp(4), 0)

            filterRow.addView(it, buttonParams)
        }

        todayButton.setOnClickListener {
            selectedDateFilter = "Today"
            loadSalesData(selectedDateFilter)
        }

        thirtyDayButton.setOnClickListener {
            selectedDateFilter = "30 Days"
            loadSalesData(selectedDateFilter)
        }

        ninetyDayButton.setOnClickListener {
            selectedDateFilter = "90 Days"
            loadSalesData(selectedDateFilter)
        }

        ytdButton.setOnClickListener {
            selectedDateFilter = "YTD"
            loadSalesData(selectedDateFilter)
        }

        // SUMMARY ROW
        val summaryRow = LinearLayout(activity)
        summaryRow.orientation = LinearLayout.HORIZONTAL

        totalRevenueCard = makeSummaryCard(
            "Total Revenue",
            "$0.00",
            getPositiveColor()
        )

        transactionCountCard = makeSummaryCard(
            "Transactions",
            "0",
            getPrimaryActionColor()
        )

        taxCollectedCard = makeSummaryCard(
            "Tax Collected",
            "$0.00",
            getNegativeColor()
        )

        summaryRow.addView(totalRevenueCard)
        summaryRow.addView(transactionCountCard)
        summaryRow.addView(taxCollectedCard)

        // GRAPH DATA
        val graphData = listOf(
            ReportBar("Sales Revenue", 0f),
            ReportBar("ATM Commissions", 0f)
        )

        // GRAPH
        val graph = BarChartView(
            activity = activity,
            data = graphData,
            darkModeEnabled = isDarkModeEnabled(),
            colorblindModeEnabled = isColorblindModeEnabled()
        )

        val graphParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(220)
        )

        graphParams.setMargins(0, dp(30), 0, dp(20))

        // EXPORT BUTTON PLACEHOLDER
        val exportButton = TextView(activity)
        exportButton.text = "Export to Excel"
        exportButton.textSize = 16f
        exportButton.gravity = Gravity.CENTER
        exportButton.setTextColor(Color.WHITE)
        exportButton.setBackgroundColor(getPositiveColor())
        exportButton.setPadding(0, dp(14), 0, dp(14))

        page.addView(title)
        page.addView(description)
        page.addView(filterRow, filterParams)
        page.addView(summaryRow)
        page.addView(graph, graphParams)
        page.addView(exportButton)

        loadSalesData(selectedDateFilter)

        scrollView.addView(page)

        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(getPageBackgroundColor())
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        return root
    }

    private fun makeSummaryCard(label: String, amount: String, color: Int): TextView {

        val card = TextView(activity)
        card.text = "$label\n$amount"
        card.textSize = 18f
        card.setTextColor(color)
        card.setPadding(dp(20), dp(16), dp(20), dp(16))
        card.setBackgroundColor(getCardBackgroundColor())

        val params = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        params.setMargins(dp(6), 0, dp(6), 0)
        card.layoutParams = params

        return card
    }

    /*
     * Checks whether Dark Mode is enabled.
     */
    private fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    /*
     * Checks whether Colorblind Mode is enabled.
     */
    private fun isColorblindModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_COLORBLIND_MODE, false)
    }

    /*
     * Main page background.
     */
    private fun getPageBackgroundColor(): Int {
        // Uses ThemeManager so page backgrounds follow Light or Dark Mode - AF
        return ThemeManager.pageBackground(activity)
    }

    /*
     * Summary card background.
     */
    private fun getCardBackgroundColor(): Int {
        // Uses ThemeManager so card backgrounds follow Light or Dark Mode - AF
        return ThemeManager.sectionBackground(activity)
    }

    /*
     * Main title text color.
     */
    private fun getPrimaryTextColor(): Int {
        // Uses ThemeManager so primary text follows Light or Dark Mode - AF
        return ThemeManager.primaryText(activity)
    }

    /*
     * Description / secondary text color.
     */
    private fun getSecondaryTextColor(): Int {
        // Uses ThemeManager so secondary text follows Light or Dark Mode - AF
        return ThemeManager.secondaryText(activity)
    }

    /*
     * Primary action color.
     *
     * In Colorblind Mode, use accessible blue.
     */
    private fun getPrimaryActionColor(): Int {
        // Uses ThemeManager so action colors support Colorblind Mode - AF
        return ThemeManager.primaryAction(activity)
    }

    /*
     * Positive/success color.
     *
     * In regular mode this is green.
     * In Colorblind Mode, use blue so users are not forced
     * to rely on green/red differences.
     */
    private fun getPositiveColor(): Int {
        // Uses ThemeManager so positive colors support Colorblind Mode - AF
        return ThemeManager.positive(activity)
    }

    /*
     * Negative/error color.
     *
     * In Colorblind Mode, use orange-red instead of pure red.
     */
    private fun getNegativeColor(): Int {
        // Uses ThemeManager so negative colors support Colorblind Mode - AF
        return ThemeManager.negative(activity)
    }
    private fun loadSalesData(filter: String) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val calendar = Calendar.getInstance()

                when (filter) {
                    "Today" -> {
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                    }

                    "30 Days" -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -30)
                    }

                    "90 Days" -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -90)
                    }

                    "YTD" -> {
                        calendar.set(Calendar.MONTH, Calendar.JANUARY)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                    }
                }

                val startDate = calendar.time

                val snapshot = db.collection("sales")
                    .whereGreaterThanOrEqualTo("timestamp", startDate)
                    .get()
                    .await()

                var totalRevenue = 0.0
                var totalTax = 0.0
                var transactionCount = 0

                snapshot.documents.forEach { document ->
                    totalRevenue += document.getDouble("total") ?: 0.0
                    totalTax += document.getDouble("tax") ?: 0.0
                    transactionCount++
                }

                withContext(Dispatchers.Main) {
                    totalRevenueCard.text =
                        "Total Revenue\n$${"%.2f".format(totalRevenue)}"

                    transactionCountCard.text =
                        "Transactions\n$transactionCount"

                    taxCollectedCard.text =
                        "Tax Collected\n$${"%.2f".format(totalTax)}"
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    totalRevenueCard.text =
                        "Total Revenue\nError"

                    transactionCountCard.text =
                        "Transactions\nError"

                    taxCollectedCard.text =
                        "Tax Collected\nError"
                }
            }
        }
    }
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}

data class ReportBar(
    val label: String,
    val value: Float
)

class BarChartView(
    activity: Activity,
    private val data: List<ReportBar>,
    private val darkModeEnabled: Boolean,
    private val colorblindModeEnabled: Boolean
) : View(activity) {

    private val barPaint = Paint()
    private val textPaint = Paint()
    private val axisPaint = Paint()

    init {
        barPaint.color = getPositiveColor()

        textPaint.color = getTextColor()
        textPaint.textSize = 28f
        textPaint.textAlign = Paint.Align.CENTER

        axisPaint.color = getGridColor()
        axisPaint.strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            return
        }

        canvas.drawColor(getChartBackgroundColor())

        val paddingLeft = 100f
        val paddingTop = 40f
        val paddingBottom = 100f

        val chartBottom = height - paddingBottom
        val chartTop = paddingTop
        val chartHeight = chartBottom - chartTop

        val maxValue = 500f
        val increment = 50f

        // GRID LINE PAINT
        val gridPaint = Paint()
        gridPaint.color = getGridColor()
        gridPaint.strokeWidth = 2f

        // AXIS TEXT PAINT
        val axisTextPaint = Paint()
        axisTextPaint.color = getAxisTextColor()
        axisTextPaint.textSize = 24f

        // DRAW GRID LINES + LABELS
        var currentValue = 0f

        while (currentValue <= maxValue) {

            val y = chartBottom - (currentValue / maxValue) * chartHeight

            // LINE
            canvas.drawLine(
                paddingLeft,
                y,
                width.toFloat(),
                y,
                gridPaint
            )

            // NUMBER LABEL
            canvas.drawText(
                currentValue.toInt().toString(),
                20f,
                y + 8f,
                axisTextPaint
            )

            currentValue += increment
        }

        // BAR SIZING
        val barSpace = (width - paddingLeft) / data.size
        val barWidth = barSpace * 0.45f

        // DRAW BARS
        for (i in data.indices) {

            val item = data[i]

            val barHeight = (item.value / maxValue) * chartHeight

            val left = paddingLeft + i * barSpace + barSpace * 0.25f
            val top = chartBottom - barHeight
            val right = left + barWidth
            val bottom = chartBottom

            // BAR
            canvas.drawRect(left, top, right, bottom, barPaint)

            // VALUE LABEL
            canvas.drawText(
                "$${item.value}",
                left + barWidth / 2f,
                top - 10f,
                textPaint
            )

            // BAR NAME
            canvas.drawText(
                item.label,
                left + barWidth / 2f,
                height - 30f,
                textPaint
            )
        }
    }

    /*
     * Chart background color.
     */
    private fun getChartBackgroundColor(): Int {
        return if (darkModeEnabled) {
            Color.rgb(38, 38, 38)
        } else {
            Color.WHITE
        }
    }

    /*
     * Main chart label text color.
     */
    private fun getTextColor(): Int {
        return if (darkModeEnabled) {
            Color.WHITE
        } else {
            Color.DKGRAY
        }
    }

    /*
     * Axis number text color.
     */
    private fun getAxisTextColor(): Int {
        return if (darkModeEnabled) {
            Color.LTGRAY
        } else {
            Color.GRAY
        }
    }

    /*
     * Grid line color.
     */
    private fun getGridColor(): Int {
        return if (darkModeEnabled) {
            Color.rgb(80, 80, 80)
        } else {
            Color.LTGRAY
        }
    }

    /*
     * Bar color.
     */
    private fun getPositiveColor(): Int {
        return if (colorblindModeEnabled) {
            Color.rgb(0, 114, 178)
        } else {
            Color.rgb(20, 180, 120)
        }
    }
}
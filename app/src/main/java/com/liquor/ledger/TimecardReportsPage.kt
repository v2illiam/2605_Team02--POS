package com.liquor.ledger

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TimecardReportsPage shows all employees clock in/out records
// Only accessible to managers via the Timecard page
// onBack callback is called when the back button is pressed

class TimecardReportsPage(
    private val activity: Activity,
    private val onBack: () -> Unit
) {

    private val db: FirebaseFirestore = FirebaseManager.db
    private lateinit var reportContainer: LinearLayout
    private lateinit var weekRangeLabel: TextView
    private var currentWeekStart: Calendar = getWeekStart(Calendar.getInstance())

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL

        // Changes page background based on Settings - AF
        page.setBackgroundColor(ThemeManager.pageBackground(activity))

        // TOP BAR with back button and title
        val topBar = LinearLayout(activity)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(dp(16), dp(12), dp(16), dp(12))

        // Changes top bar background based on Settings - AF
        topBar.setBackgroundColor(ThemeManager.sectionBackground(activity))

        val backBtn = TextView(activity)
        backBtn.text = "< Back to Timecard"
        backBtn.textSize = 14f

        // Changes back button color based on Settings - AF
        backBtn.setTextColor(ThemeManager.primaryAction(activity))

        backBtn.setPadding(0, 0, dp(16), 0)
        backBtn.setOnClickListener { onBack() }

        val titleText = TextView(activity)
        titleText.text = "Timecard Reports"
        titleText.textSize = 18f

        // Changes title color based on Settings - AF
        titleText.setTextColor(ThemeManager.primaryText(activity))

        titleText.setTypeface(null, Typeface.BOLD)
        titleText.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        topBar.addView(backBtn)
        topBar.addView(titleText)
        page.addView(topBar)

        val subtitle = TextView(activity)
        subtitle.text = "All employee attendance records"
        subtitle.textSize = 13f

        // Changes subtitle color based on Settings - AF
        subtitle.setTextColor(ThemeManager.mutedText(activity))

        subtitle.setPadding(dp(16), dp(4), dp(16), dp(8))
        page.addView(subtitle)

        // WEEK NAVIGATION
        val weekNav = LinearLayout(activity)
        weekNav.orientation = LinearLayout.HORIZONTAL
        weekNav.gravity = Gravity.CENTER_VERTICAL
        weekNav.setPadding(dp(16), dp(8), dp(16), dp(8))

        val prevBtn = makeNavButton("< Prev Week")
        prevBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            loadReport()
        }

        weekRangeLabel = TextView(activity)
        weekRangeLabel.textSize = 14f

        // Changes week range color based on Settings - AF
        weekRangeLabel.setTextColor(ThemeManager.primaryText(activity))

        weekRangeLabel.setTypeface(null, Typeface.BOLD)
        weekRangeLabel.gravity = Gravity.CENTER
        weekRangeLabel.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val nextBtn = makeNavButton("Next Week >")
        nextBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            loadReport()
        }

        weekNav.addView(prevBtn)
        weekNav.addView(weekRangeLabel)
        weekNav.addView(nextBtn)
        page.addView(weekNav)

        // TABLE HEADER
        page.addView(makeTableHeader())

        // SCROLLABLE REPORT
        val scrollView = ScrollView(activity)

        // Changes scroll background based on Settings - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        reportContainer = LinearLayout(activity)

        // Changes report container background based on Settings - AF
        reportContainer.setBackgroundColor(ThemeManager.pageBackground(activity))

        reportContainer.orientation = LinearLayout.VERTICAL
        scrollView.addView(reportContainer)
        page.addView(scrollView, scrollParams)

        loadReport()

        return page
    }

    // Loads all employee timecards for the selected week
    private fun loadReport() {
        reportContainer.removeAllViews()

        val weekEnd = getWeekEnd(currentWeekStart)
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        weekRangeLabel.text = "Week of ${dateFormat.format(currentWeekStart.time)}" +
            " - ${dateFormat.format(weekEnd.time)}" +
            ", ${yearFormat.format(weekEnd.time)}"

        val loadingText = TextView(activity)
        loadingText.text = "Loading report..."
        loadingText.textSize = 14f

        // Changes loading text color based on Settings - AF
        loadingText.setTextColor(ThemeManager.mutedText(activity))

        loadingText.setPadding(dp(16), dp(16), dp(16), dp(16))
        reportContainer.addView(loadingText)

        // Build list of dates for this week
        val weekDates = mutableListOf<String>()
        val cal = currentWeekStart.clone() as Calendar
        val dateStrFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0..6) {
            weekDates.add(dateStrFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load all employees first
                val employeesSnapshot = db.collection("employees").get().await()
                val employees = employeesSnapshot.documents.mapNotNull { doc ->
                    Employee(
                        employeeId = doc.getString("employeeId") ?: "",
                        name = doc.getString("name") ?: "",
                        position = doc.getString("position") ?: "",
                        email = doc.getString("email") ?: "",
                        uid = doc.getString("uid") ?: ""
                    )
                }.filter { it.employeeId.isNotEmpty() }

                // Load timecards one employee at a time to avoid
                // Firestore whereIn limit issues
                val timecardsByEmployee =
                    mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()

                employees.forEach { employee ->
                    val empTimecards = db.collection("timecards")
                        .whereEqualTo("employeeId", employee.employeeId)
                        .get()
                        .await()

                    empTimecards.documents.forEach { doc ->
                        val date = doc.getString("date") ?: ""
                        if (date.isNotEmpty() && weekDates.contains(date)) {
                            if (!timecardsByEmployee.containsKey(employee.employeeId)) {
                                timecardsByEmployee[employee.employeeId] = mutableMapOf()
                            }
                            timecardsByEmployee[employee.employeeId]!![date] = mapOf(
                                "clockIn" to doc.getTimestamp("clockIn"),
                                "clockOut" to doc.getTimestamp("clockOut"),
                                "breakMinutes" to doc.getLong("breakMinutes"),
                                "hoursWorked" to doc.getDouble("hoursWorked"),
                                "status" to doc.getString("status")
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    reportContainer.removeAllViews()

                    if (employees.isEmpty()) {
                        val emptyText = TextView(activity)
                        emptyText.text = "No employees found"
                        emptyText.textSize = 14f

                        // Changes empty text color based on Settings - AF
                        emptyText.setTextColor(ThemeManager.mutedText(activity))

                        emptyText.setPadding(dp(16), dp(16), dp(16), dp(16))
                        reportContainer.addView(emptyText)
                        return@withContext
                    }

                    employees.forEach { employee ->
                        val empTimecards = timecardsByEmployee[employee.employeeId]
                            ?: emptyMap()

                        // Employee name header row
                        val empHeader = LinearLayout(activity)
                        empHeader.orientation = LinearLayout.HORIZONTAL
                        empHeader.setPadding(dp(16), dp(12), dp(16), dp(8))

                        // Changes employee header background based on Settings - AF
                        empHeader.setBackgroundColor(ThemeManager.sectionBackground(activity))

                        val empName = TextView(activity)
                        empName.text = "${employee.name} (${employee.position})"
                        empName.textSize = 15f

                        // Changes employee name color based on Settings - AF
                        empName.setTextColor(ThemeManager.primaryText(activity))

                        empName.setTypeface(null, Typeface.BOLD)
                        empName.layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                        val totalHours = empTimecards.values.sumOf {
                            it["hoursWorked"] as? Double ?: 0.0
                        }
                        val totalHoursText = TextView(activity)
                        totalHoursText.text = "Total: ${"%.2f".format(totalHours)} hrs"
                        totalHoursText.textSize = 14f

                        // Changes total hours color based on Settings - AF
                        totalHoursText.setTextColor(ThemeManager.primaryAction(activity))

                        totalHoursText.setTypeface(null, Typeface.BOLD)

                        empHeader.addView(empName)
                        empHeader.addView(totalHoursText)
                        reportContainer.addView(empHeader)

                        // Daily rows
                        weekDates.forEach { date ->
                            val timecard = empTimecards[date]
                            reportContainer.addView(makeDayRow(date, timecard))

                            val divider = android.view.View(activity)

                            // Changes divider color based on Settings - AF
                            divider.setBackgroundColor(ThemeManager.divider(activity))

                            reportContainer.addView(
                                divider,
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 1))
                        }

                        // Space between employees
                        val spacer = android.view.View(activity)

                        // Changes employee spacer color based on Settings - AF
                        spacer.setBackgroundColor(ThemeManager.divider(activity))

                        reportContainer.addView(
                            spacer,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, dp(4)))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    reportContainer.removeAllViews()
                    val errorText = TextView(activity)
                    errorText.text = "Error loading report: ${e.message}"
                    errorText.textSize = 14f

                    // Changes error text color based on Settings - AF
                    errorText.setTextColor(ThemeManager.negative(activity))

                    errorText.setPadding(dp(16), dp(16), dp(16), dp(16))
                    reportContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a single day row
    private fun makeDayRow(
        date: String,
        timecard: Map<String, Any?>?
    ): LinearLayout {

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(10), dp(16), dp(10))
        row.gravity = Gravity.CENTER_VERTICAL

        // Changes row background based on Settings - AF
        row.setBackgroundColor(ThemeManager.pageBackground(activity))

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val clockIn = timecard?.get("clockIn") as? Timestamp
        val clockOut = timecard?.get("clockOut") as? Timestamp
        val breakMinutes = timecard?.get("breakMinutes") as? Long ?: 0L
        val hoursWorked = timecard?.get("hoursWorked") as? Double ?: 0.0
        val status = timecard?.get("status") as? String

        val clockInStr = clockIn?.let { timeFormat.format(it.toDate()) } ?: "-"
        val clockOutStr = clockOut?.let { timeFormat.format(it.toDate()) } ?: "-"
        val breakStr = if (breakMinutes > 0) "${breakMinutes}m" else "-"
        val hoursStr = if (hoursWorked > 0)
            "${"%.2f".format(hoursWorked)} hrs" else "-"

        val displayStatus = when {
            status == "Completed" -> "Completed"
            status == "In Progress" -> "In Progress"
            timecard == null -> "Day Off"
            else -> "Day Off"
        }

        val statusColor = when (displayStatus) {
            "Completed" -> Color.rgb(34, 197, 94)
            "In Progress" -> Color.rgb(45, 95, 255)
            else -> Color.GRAY
        }

        // Changes row cell text colors based on Settings - AF
        row.addView(makeCell(date, 2f, ThemeManager.secondaryText(activity)))
        row.addView(makeCell(clockInStr, 1f, ThemeManager.secondaryText(activity)))
        row.addView(makeCell(clockOutStr, 1f, ThemeManager.secondaryText(activity)))
        row.addView(makeCell(breakStr, 1f, ThemeManager.secondaryText(activity)))
        row.addView(makeCell(hoursStr, 1f, ThemeManager.secondaryText(activity)))
        row.addView(makeCell(displayStatus, 1f, statusColor))

        return row
    }

    // Creates the table header
    private fun makeTableHeader(): LinearLayout {
        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.setPadding(dp(16), dp(10), dp(16), dp(10))

        // Changes table header background based on Settings - AF
        header.setBackgroundColor(ThemeManager.sectionBackground(activity))

        listOf(
            Pair("Date", 2f),
            Pair("Clock In", 1f),
            Pair("Clock Out", 1f),
            Pair("Break", 1f),
            Pair("Hours", 1f),
            Pair("Status", 1f)
        ).forEach { (text, weight) ->
            val cell = TextView(activity)
            cell.text = text
            cell.textSize = 12f

            // Changes table header text color based on Settings - AF
            cell.setTextColor(ThemeManager.mutedText(activity))

            cell.setTypeface(null, Typeface.BOLD)
            cell.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            header.addView(cell)
        }

        return header
    }

    // Creates a table cell
    private fun makeCell(text: String, weight: Float, color: Int): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 13f
        cell.setTextColor(color)
        cell.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
        return cell
    }

    // Creates a nav button
    private fun makeNavButton(text: String): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 14f
        btn.gravity = Gravity.CENTER

        // Changes nav button color based on Settings - AF
        btn.setTextColor(ThemeManager.primaryAction(activity))

        btn.setPadding(dp(12), dp(8), dp(12), dp(8))
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        return btn
    }

    // Gets the Monday of the week
    private fun getWeekStart(cal: Calendar): Calendar {
        val result = cal.clone() as Calendar
        result.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        result.set(Calendar.HOUR_OF_DAY, 0)
        result.set(Calendar.MINUTE, 0)
        result.set(Calendar.SECOND, 0)
        result.set(Calendar.MILLISECOND, 0)
        return result
    }

    // Gets the Sunday of the week
    private fun getWeekEnd(weekStart: Calendar): Calendar {
        val result = weekStart.clone() as Calendar
        result.add(Calendar.DAY_OF_WEEK, 6)
        return result
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}

package com.liquor.ledger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
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

// TimecardPage shows the weekly timesheet for the current employee
// Managers can also view and edit other employees timesheets
// via a dropdown at the top of the screen

class TimecardPage(private val activity: Activity) {

    // Firestore instance
    private val db: FirebaseFirestore = FirebaseManager.db

    // Current logged in employee
    private val currentEmployee = SessionManager.currentEmployee

    // Is the current user a manager
    private val isManager = currentEmployee?.position == "Manager"

    // The employee currently being viewed
    // Defaults to the logged in employee
    private var viewingEmployee: Employee? = currentEmployee

    // List of all employees for the manager dropdown
    private var allEmployees: List<Employee> = emptyList()

    // The timesheet table container
    private lateinit var timesheetContainer: LinearLayout

    // The clock in/out/break buttons
    private lateinit var clockInBtn: TextView
    private lateinit var clockOutBtn: TextView
    private lateinit var breakBtn: TextView

    // Week navigation
    private var currentWeekStart: Calendar = getWeekStart(Calendar.getInstance())

    // Week range label
    private lateinit var weekRangeLabel: TextView

    // Total hours label
    private lateinit var totalHoursLabel: TextView

    // Current active timecard document ID
    private var activeTimecardId: String? = null

    // Is the employee currently on break
    private var onBreak = false

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL

        // Changes page background based on Settings - AF
        page.setBackgroundColor(ThemeManager.pageBackground(activity))

        // TOP SECTION — employee selector for managers
        if (isManager) {
            val selectorRow = LinearLayout(activity)
            selectorRow.orientation = LinearLayout.HORIZONTAL
            selectorRow.gravity = Gravity.CENTER_VERTICAL
            selectorRow.setPadding(dp(16), dp(12), dp(16), dp(12))

            // Changes selector row background based on Settings - AF
            selectorRow.setBackgroundColor(ThemeManager.sectionBackground(activity))

            val selectorLabel = TextView(activity)
            selectorLabel.text = "Viewing: "
            selectorLabel.textSize = 14f

            // Changes selector label color based on Settings - AF
            selectorLabel.setTextColor(ThemeManager.secondaryText(activity))

            val employeeSpinner = Spinner(activity)
            val spinnerParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            employeeSpinner.layoutParams = spinnerParams

            selectorRow.addView(selectorLabel)
            selectorRow.addView(employeeSpinner, spinnerParams)
            page.addView(selectorRow)

            // Load employees for dropdown
            loadEmployeesForDropdown(employeeSpinner)
        }

        // WEEK NAVIGATION ROW
        val weekNav = LinearLayout(activity)
        weekNav.orientation = LinearLayout.HORIZONTAL
        weekNav.gravity = Gravity.CENTER_VERTICAL
        weekNav.setPadding(dp(16), dp(12), dp(16), dp(12))

        val prevWeekBtn = makeNavButton("< Prev")
        prevWeekBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            loadTimesheet()
        }

        weekRangeLabel = TextView(activity)
        weekRangeLabel.textSize = 15f

        // Changes week range text color based on Settings - AF
        weekRangeLabel.setTextColor(ThemeManager.primaryText(activity))

        weekRangeLabel.setTypeface(null, Typeface.BOLD)
        weekRangeLabel.gravity = Gravity.CENTER
        weekRangeLabel.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val nextWeekBtn = makeNavButton("Next >")
        nextWeekBtn.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            loadTimesheet()
        }

        weekNav.addView(prevWeekBtn)
        weekNav.addView(weekRangeLabel)
        weekNav.addView(nextWeekBtn)
        page.addView(weekNav)

        // TOTAL HOURS ROW
        totalHoursLabel = TextView(activity)
        totalHoursLabel.text = "Total Hours This Week: 0.00 hrs"
        totalHoursLabel.textSize = 14f

        // Changes total hours text color based on Settings - AF
        totalHoursLabel.setTextColor(ThemeManager.primaryAction(activity))

        totalHoursLabel.setTypeface(null, Typeface.BOLD)
        totalHoursLabel.setPadding(dp(16), dp(8), dp(16), dp(8))
        totalHoursLabel.gravity = Gravity.END
        page.addView(totalHoursLabel)

        // TABLE HEADER
        page.addView(makeTableHeader())

        // SCROLLABLE TIMESHEET
        val scrollView = ScrollView(activity)

        // Changes scroll background based on Settings - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        timesheetContainer = LinearLayout(activity)
        timesheetContainer.orientation = LinearLayout.VERTICAL

        // Changes timesheet container background based on Settings - AF
        timesheetContainer.setBackgroundColor(ThemeManager.pageBackground(activity))

        scrollView.addView(timesheetContainer)
        page.addView(scrollView, scrollParams)

        // TIMECARD REPORTS BUTTON — managers only
        if (isManager) {
            val reportsBtn = TextView(activity)
            reportsBtn.text = "View Timecard Reports"
            reportsBtn.textSize = 14f
            reportsBtn.gravity = Gravity.CENTER
            reportsBtn.setTextColor(Color.WHITE)

            // Changes reports button color based on Settings - AF
            reportsBtn.setBackgroundColor(ThemeManager.primaryAction(activity))

            reportsBtn.setPadding(dp(16), dp(12), dp(16), dp(12))

            val reportsBtnParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            reportsBtnParams.setMargins(dp(16), dp(8), dp(16), dp(4))
            reportsBtn.layoutParams = reportsBtnParams

            reportsBtn.setOnClickListener {
                // Replace the timecard page with the reports page
                val parent = page.parent as? LinearLayout
                if (parent != null) {
                    parent.removeAllViews()
                    val reportsPage = TimecardReportsPage(activity) {
                        parent.removeAllViews()
                        parent.addView(build())
                    }
                    parent.addView(reportsPage.build())
                }
            }

            page.addView(reportsBtn)
        }

        // CLOCK IN/OUT/BREAK BUTTONS
        val buttonRow = LinearLayout(activity)
        buttonRow.orientation = LinearLayout.HORIZONTAL
        buttonRow.setPadding(dp(16), dp(12), dp(16), dp(12))

        // Changes button row background based on Settings - AF
        buttonRow.setBackgroundColor(ThemeManager.sectionBackground(activity))

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        clockInBtn = makeClockButton("Clock In", ThemeManager.positive(activity))

        clockInBtn.setOnClickListener { clockIn() }

        clockOutBtn = makeClockButton("Clock Out", ThemeManager.negative(activity))

        clockOutBtn.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle("Clock Out?")
                .setMessage("Are you sure you want to clock out now?")
                .setPositiveButton("Clock Out") { _, _ -> clockOut() }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        breakBtn = makeClockButton("Break Out", ThemeManager.warning(activity))

        breakBtn.setOnClickListener { toggleBreak() }

        buttonRow.addView(clockInBtn)
        buttonRow.addView(clockOutBtn)
        buttonRow.addView(breakBtn)

        page.addView(buttonRow, buttonParams)

        // Load the timesheet
        loadTimesheet()

        return page
    }

    // Loads all employees into the manager dropdown
    private fun loadEmployeesForDropdown(spinner: Spinner) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("employees").get().await()
                val employees = snapshot.documents.mapNotNull { doc ->
                    Employee(
                        employeeId = doc.getString("employeeId") ?: "",
                        name = doc.getString("name") ?: "",
                        position = doc.getString("position") ?: "",
                        email = doc.getString("email") ?: "",
                        uid = doc.getString("uid") ?: ""
                    )
                }

                withContext(Dispatchers.Main) {
                    allEmployees = employees
                    val names = employees.map { it.name }
                    val adapter = ArrayAdapter(
                        activity,
                        android.R.layout.simple_spinner_item,
                        names
                    )
                    adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter

                    // Default to current employee
                    val currentIndex = employees.indexOfFirst {
                        it.employeeId == currentEmployee?.employeeId
                    }
                    if (currentIndex >= 0) spinner.setSelection(currentIndex)

                    spinner.onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: android.view.View?,
                                position: Int,
                                id: Long
                            ) {
                                viewingEmployee = employees[position]
                                loadTimesheet()
                            }
                            override fun onNothingSelected(
                                parent: android.widget.AdapterView<*>?) {}
                        }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadTimesheet()
                }
            }
        }
    }

    // Loads the timesheet for the current week and employee
    private fun loadTimesheet() {
        timesheetContainer.removeAllViews()

        val weekEnd = getWeekEnd(currentWeekStart)

        // Update week range label
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        weekRangeLabel.text = "Week of ${dateFormat.format(currentWeekStart.time)}" +
            " - ${dateFormat.format(weekEnd.time)}" +
            ", ${yearFormat.format(weekEnd.time)}"

        val loadingText = TextView(activity)
        loadingText.text = "Loading..."
        loadingText.textSize = 14f

        // Changes loading text color based on Settings - AF
        loadingText.setTextColor(ThemeManager.mutedText(activity))

        loadingText.setPadding(dp(16), dp(16), dp(16), dp(16))
        timesheetContainer.addView(loadingText)

        val employeeId = viewingEmployee?.employeeId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all timecards for this employee
                val snapshot = db.collection("timecards")
                    .whereEqualTo("employeeId", employeeId)
                    .get()
                    .await()

                // Build a map of date string to timecard document
                val timecardMap = mutableMapOf<String, Map<String, Any?>>()
                snapshot.documents.forEach { doc ->
                    val date = doc.getString("date") ?: ""
                    if (date.isNotEmpty()) {
                        timecardMap[date] = mapOf(
                            "docId" to doc.id,
                            "clockIn" to doc.getTimestamp("clockIn"),
                            "clockOut" to doc.getTimestamp("clockOut"),
                            "breakStart" to doc.getTimestamp("breakStart"),
                            "breakEnd" to doc.getTimestamp("breakEnd"),
                            "breakMinutes" to doc.getLong("breakMinutes"),
                            "hoursWorked" to doc.getDouble("hoursWorked"),
                            "status" to doc.getString("status")
                        )
                    }
                }

                // Check for active timecard today
                val todayStr = SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                val todayCard = timecardMap[todayStr]
                val hasClockIn = todayCard?.get("clockIn") != null
                val hasClockOut = todayCard?.get("clockOut") != null
                val hasBreakStart = todayCard?.get("breakStart") != null
                val hasBreakEnd = todayCard?.get("breakEnd") != null
                activeTimecardId = todayCard?.get("docId") as? String
                onBreak = hasBreakStart && !hasBreakEnd

                withContext(Dispatchers.Main) {
                    timesheetContainer.removeAllViews()

                    // Update button states
                    updateButtonStates(hasClockIn, hasClockOut)

                    var totalMinutes = 0.0

                    // Generate a row for each day of the week
                    val cal = currentWeekStart.clone() as Calendar
                    for (i in 0..6) {
                        val dateStr = SimpleDateFormat(
                            "yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                        val dayName = SimpleDateFormat(
                            "EEEE", Locale.getDefault()).format(cal.time)
                        val displayDate = SimpleDateFormat(
                            "yyyy-MM-dd", Locale.getDefault()).format(cal.time)

                        val timecard = timecardMap[dateStr]
                        val row = makeTimesheetRow(
                            displayDate, dayName, timecard)
                        timesheetContainer.addView(row)

                        // Add to total hours
                        val hours = timecard?.get("hoursWorked") as? Double ?: 0.0
                        totalMinutes += hours * 60

                        // Divider
                        val divider = android.view.View(activity)

                        // Changes divider color based on Settings - AF
                        divider.setBackgroundColor(ThemeManager.divider(activity))

                        timesheetContainer.addView(
                            divider,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1))

                        cal.add(Calendar.DAY_OF_WEEK, 1)
                    }

                    // Update total hours
                    val totalHours = totalMinutes / 60
                    totalHoursLabel.text =
                        "Total Hours This Week: ${"%.2f".format(totalHours)} hrs"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    timesheetContainer.removeAllViews()
                    val errorText = TextView(activity)
                    errorText.text = "Error loading timesheet: ${e.message}"
                    errorText.textSize = 14f

                    // Changes error text color based on Settings - AF
                    errorText.setTextColor(ThemeManager.negative(activity))

                    errorText.setPadding(dp(16), dp(16), dp(16), dp(16))
                    timesheetContainer.addView(errorText)
                }
            }
        }
    }

    // Creates a single timesheet row for a day
    private fun makeTimesheetRow(
        date: String,
        dayName: String,
        timecard: Map<String, Any?>?
    ): LinearLayout {

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(12), dp(16), dp(12))
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
        val hoursStr = if (hoursWorked > 0) "${"%.2f".format(hoursWorked)} hrs" else "-"

        val displayStatus = when {
            status == "Completed" -> "Completed"
            status == "In Progress" -> "In Progress"
            timecard == null -> "Day Off"
            else -> "Day Off"
        }

        // Changes status colors based on Settings - AF
        val statusColor = when (displayStatus) {
            "Completed" -> ThemeManager.positive(activity)
            "In Progress" -> ThemeManager.primaryAction(activity)
            else -> ThemeManager.mutedText(activity)
        }

        row.addView(makeRowCell(date, 2f))
        row.addView(makeRowCell(dayName, 1f))
        row.addView(makeRowCell(clockInStr, 1f))
        row.addView(makeRowCell(clockOutStr, 1f))
        row.addView(makeRowCell(breakStr, 1f))
        row.addView(makeRowCell(hoursStr, 1f))

        val statusCell = TextView(activity)
        statusCell.text = displayStatus
        statusCell.textSize = 13f
        statusCell.setTextColor(statusColor)
        statusCell.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(statusCell)

        // Manager can click a row to edit it
        if (isManager && timecard != null) {
            row.isClickable = true
            row.isFocusable = true
            row.setOnClickListener {
                showEditTimecardDialog(
                    timecard["docId"] as? String ?: "",
                    date,
                    clockIn,
                    clockOut,
                    timecard["breakStart"] as? Timestamp,
                    timecard["breakEnd"] as? Timestamp
                )
            }
        }

        return row
    }

    // Clock in the current employee
    private fun clockIn() {
        val employeeId = viewingEmployee?.employeeId ?: return
        val employeeName = viewingEmployee?.name ?: ""

        val today = SimpleDateFormat(
            "yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val dayName = SimpleDateFormat(
            "EEEE", Locale.getDefault()).format(Calendar.getInstance().time)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if already clocked in today
                val existing = db.collection("timecards")
                    .whereEqualTo("employeeId", employeeId)
                    .whereEqualTo("date", today)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            activity,
                            "Already clocked in today",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Create new timecard document
                val newTimecard = hashMapOf(
                    "employeeId" to employeeId,
                    "employeeName" to employeeName,
                    "date" to today,
                    "dayOfWeek" to dayName,
                    "clockIn" to Timestamp.now(),
                    "clockOut" to null,
                    "breakStart" to null,
                    "breakEnd" to null,
                    "breakMinutes" to 0L,
                    "hoursWorked" to 0.0,
                    "status" to "In Progress"
                )

                val docRef = db.collection("timecards").add(newTimecard).await()
                activeTimecardId = docRef.id

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Clocked in successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadTimesheet()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error clocking in: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Clock out the current employee
    private fun clockOut() {
        val docId = activeTimecardId ?: run {
            android.widget.Toast.makeText(
                activity,
                "No active clock in found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the current timecard
                val doc = db.collection("timecards")
                    .document(docId)
                    .get()
                    .await()

                val clockInTime = doc.getTimestamp("clockIn")
                val breakMinutes = doc.getLong("breakMinutes") ?: 0L
                val clockOutTime = Timestamp.now()

                // Calculate hours worked
                val totalMinutes = if (clockInTime != null) {
                    val diff = clockOutTime.toDate().time - clockInTime.toDate().time
                    (diff / 1000 / 60).toDouble()
                } else 0.0

                val netMinutes = totalMinutes - breakMinutes
                val hoursWorked = netMinutes / 60

                db.collection("timecards")
                    .document(docId)
                    .update(
                        mapOf(
                            "clockOut" to clockOutTime,
                            "hoursWorked" to hoursWorked,
                            "status" to "Completed"
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Clocked out. Hours worked: ${"%.2f".format(hoursWorked)}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadTimesheet()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error clocking out: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Toggle break in/out
    private fun toggleBreak() {
        val docId = activeTimecardId ?: run {
            android.widget.Toast.makeText(
                activity,
                "No active clock in found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!onBreak) {
                    // Starting break — record break start
                    db.collection("timecards")
                        .document(docId)
                        .update("breakStart", Timestamp.now())
                        .await()

                    onBreak = true

                    withContext(Dispatchers.Main) {
                        breakBtn.text = "Break In"

                        // Changes break out button color based on Settings - AF
                        breakBtn.setBackgroundColor(ThemeManager.warning(activity))

                        android.widget.Toast.makeText(
                            activity,
                            "Break started",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    // Ending break — record break end and calculate break minutes
                    val doc = db.collection("timecards")
                        .document(docId)
                        .get()
                        .await()

                    val breakStart = doc.getTimestamp("breakStart")
                    val breakEnd = Timestamp.now()
                    val existingBreakMinutes = doc.getLong("breakMinutes") ?: 0L

                    val newBreakMinutes = if (breakStart != null) {
                        val diff = breakEnd.toDate().time - breakStart.toDate().time
                        existingBreakMinutes + (diff / 1000 / 60)
                    } else existingBreakMinutes

                    db.collection("timecards")
                        .document(docId)
                        .update(
                            mapOf(
                                "breakEnd" to breakEnd,
                                "breakMinutes" to newBreakMinutes
                            )
                        )
                        .await()

                    onBreak = false

                    withContext(Dispatchers.Main) {
                        breakBtn.text = "Break Out"

                        // Changes break out button color based on Settings - AF
                        breakBtn.setBackgroundColor(ThemeManager.warning(activity))

                        android.widget.Toast.makeText(
                            activity,
                            "Break ended. Total break: ${newBreakMinutes} min",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        loadTimesheet()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error toggling break: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Saves the edited timecard to Firestore
    private fun saveEditedTimecard(
        docId: String,
        date: String,
        clockInStr: String,
        clockOutStr: String,
        breakStartStr: String,
        breakEndStr: String
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateFormat.isLenient = false

        // Validates and parses a time string, returns null if invalid format
        // but distinguishes "empty" (ok) from "bad format" (not ok)
        fun parseTimeOrNull(timeStr: String): Pair<Timestamp?, Boolean> {
            if (timeStr.isBlank()) return Pair(null, true) // empty is valid (means "not set")
            return try {
                val parsed = dateFormat.parse("$date $timeStr")
                if (parsed != null) Pair(Timestamp(parsed), true) else Pair(null, false)
            } catch (e: Exception) {
                Pair(null, false)
            }
        }

        val (clockIn, clockInValid) = parseTimeOrNull(clockInStr)
        val (clockOut, clockOutValid) = parseTimeOrNull(clockOutStr)
        val (breakStart, breakStartValid) = parseTimeOrNull(breakStartStr)
        val (breakEnd, breakEndValid) = parseTimeOrNull(breakEndStr)

        if (!clockInValid || !clockOutValid || !breakStartValid || !breakEndValid) {
            android.widget.Toast.makeText(
                activity,
                "Invalid time format. Please use HH:mm (e.g. 09:00 or 17:30)",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        if (clockIn != null && clockOut != null &&
            clockOut.toDate().before(clockIn.toDate())) {
            android.widget.Toast.makeText(
                activity,
                "Clock out time cannot be before clock in time",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Recalculate break minutes
                val breakMinutes = if (breakStart != null && breakEnd != null) {
                    val diff = breakEnd.toDate().time - breakStart.toDate().time
                    diff / 1000 / 60
                } else 0L

                // Recalculate hours worked
                val hoursWorked = if (clockIn != null && clockOut != null) {
                    val totalMinutes = (clockOut.toDate().time -
                        clockIn.toDate().time) / 1000 / 60
                    (totalMinutes - breakMinutes).toDouble() / 60
                } else 0.0

                val status = if (clockIn != null && clockOut != null)
                    "Completed" else "In Progress"

                db.collection("timecards")
                    .document(docId)
                    .update(
                        mapOf(
                            "clockIn" to clockIn,
                            "clockOut" to clockOut,
                            "breakStart" to breakStart,
                            "breakEnd" to breakEnd,
                            "breakMinutes" to breakMinutes,
                            "hoursWorked" to hoursWorked,
                            "status" to status
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Timecard updated successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadTimesheet()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error updating timecard: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Shows a dialog for managers to edit a timecard entry
    private fun showEditTimecardDialog(
        docId: String,
        date: String,
        currentClockIn: Timestamp?,
        currentClockOut: Timestamp?,
        currentBreakStart: Timestamp?,
        currentBreakEnd: Timestamp?
    ) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Edit Timecard - $date")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val clockInLabel = makeDialogLabel("Clock In (HH:mm)")
        val clockInInput = makeDialogInput(
            currentClockIn?.let { timeFormat.format(it.toDate()) } ?: "")

        val clockOutLabel = makeDialogLabel("Clock Out (HH:mm)")
        val clockOutInput = makeDialogInput(
            currentClockOut?.let { timeFormat.format(it.toDate()) } ?: "")

        val breakStartLabel = makeDialogLabel("Break Start (HH:mm)")
        val breakStartInput = makeDialogInput(
            currentBreakStart?.let { timeFormat.format(it.toDate()) } ?: "")

        val breakEndLabel = makeDialogLabel("Break End (HH:mm)")
        val breakEndInput = makeDialogInput(
            currentBreakEnd?.let { timeFormat.format(it.toDate()) } ?: "")

        form.addView(clockInLabel)
        form.addView(clockInInput)
        form.addView(clockOutLabel)
        form.addView(clockOutInput)
        form.addView(breakStartLabel)
        form.addView(breakStartInput)
        form.addView(breakEndLabel)
        form.addView(breakEndInput)

        builder.setView(form)

        builder.setPositiveButton("Save") { _, _ ->
            saveEditedTimecard(
                docId,
                date,
                clockInInput.text.toString(),
                clockOutInput.text.toString(),
                breakStartInput.text.toString(),
                breakEndInput.text.toString()
            )
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Updates clock in/out/break button states
    private fun updateButtonStates(hasClockIn: Boolean, hasClockOut: Boolean) {
        // Only show buttons when viewing your own timecard
        // or when manager is viewing someone else
        val isCurrentWeek = isCurrentWeek(currentWeekStart)

        if (!isCurrentWeek) {
            clockInBtn.isEnabled = false
            clockOutBtn.isEnabled = false
            breakBtn.isEnabled = false
            clockInBtn.alpha = 0.5f
            clockOutBtn.alpha = 0.5f
            breakBtn.alpha = 0.5f
            return
        }

        clockInBtn.isEnabled = !hasClockIn
        clockOutBtn.isEnabled = hasClockIn && !hasClockOut
        breakBtn.isEnabled = hasClockIn && !hasClockOut

        clockInBtn.alpha = if (!hasClockIn) 1f else 0.5f
        clockOutBtn.alpha = if (hasClockIn && !hasClockOut) 1f else 0.5f
        breakBtn.alpha = if (hasClockIn && !hasClockOut) 1f else 0.5f

        breakBtn.text = if (onBreak) "Break In" else "Break Out"
        breakBtn.setBackgroundColor(
            if (onBreak) ThemeManager.positive(activity)
            else ThemeManager.warning(activity)
        )
    }

    // Gets the Monday of the week containing the given date
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

    // Checks if the given week start is the current week
    private fun isCurrentWeek(weekStart: Calendar): Boolean {
        val currentWeek = getWeekStart(Calendar.getInstance())
        return weekStart.get(Calendar.WEEK_OF_YEAR) ==
            currentWeek.get(Calendar.WEEK_OF_YEAR) &&
            weekStart.get(Calendar.YEAR) == currentWeek.get(Calendar.YEAR)
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
            Pair("Day", 1f),
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

    // Creates a standard row cell
    private fun makeRowCell(text: String, weight: Float): TextView {
        val cell = TextView(activity)
        cell.text = text
        cell.textSize = 13f

        // Changes row cell text color based on Settings - AF
        cell.setTextColor(ThemeManager.secondaryText(activity))

        cell.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
        return cell
    }

    // Creates a clock in/out/break button
    private fun makeClockButton(text: String, color: Int): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 15f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(16), dp(14), dp(16), dp(14))

        val params = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        params.setMargins(dp(4), 0, dp(4), 0)
        btn.layoutParams = params
        return btn
    }

    // Creates a nav button for week navigation
    private fun makeNavButton(text: String): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 14f
        btn.gravity = Gravity.CENTER

        // Changes week nav button color based on Settings - AF
        btn.setTextColor(ThemeManager.primaryAction(activity))

        btn.setPadding(dp(12), dp(8), dp(12), dp(8))
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        return btn
    }

    // Creates a dialog label
    private fun makeDialogLabel(text: String): TextView {
        val label = TextView(activity)
        label.text = text
        label.textSize = 13f

        // Changes dialog label color based on Settings - AF
        label.setTextColor(ThemeManager.mutedText(activity))

        label.setPadding(0, dp(8), 0, dp(2))
        return label
    }

    // Creates a dialog input field
    private fun makeDialogInput(defaultValue: String): android.widget.EditText {
        val input = android.widget.EditText(activity)
        input.setText(defaultValue)
        input.textSize = 14f

        // Changes dialog input colors based on Settings - AF
        input.setTextColor(ThemeManager.primaryText(activity))

        input.setPadding(dp(8), dp(8), dp(8), dp(8))

        input.setBackgroundColor(ThemeManager.inputBackground(activity))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, dp(4))
        input.layoutParams = params
        return input
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}

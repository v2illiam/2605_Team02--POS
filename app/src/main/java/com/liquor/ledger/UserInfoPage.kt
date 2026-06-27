package com.liquor.ledger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// UserInfoPage shows the logged in employee's profile
// Managers can view and edit all employees
// Managers can add new employees and reset passwords
// Managers can unlock locked accounts

class UserInfoPage(private val activity: Activity) {

    private val db: FirebaseFirestore = FirebaseManager.db
    private val auth: FirebaseAuth = FirebaseManager.auth

    private val currentEmployee = SessionManager.currentEmployee
    private val isManager = currentEmployee?.position == "Manager"

    // Container for the employee list (manager view)
    private lateinit var employeeListContainer: LinearLayout

    // Holds the full employee list so search can filter without refetching
    private var allEmployeesCache: List<Pair<Employee, Map<String, Any>>> = emptyList()
    private var currentSearch = ""

    fun build(): LinearLayout {

        val page = LinearLayout(activity)
        page.orientation = LinearLayout.VERTICAL

        // Changes page background based on Settings - AF
        page.setBackgroundColor(ThemeManager.pageBackground(activity))

        val scrollView = ScrollView(activity)

        // Changes scroll background based on Settings - AF
        scrollView.setBackgroundColor(ThemeManager.pageBackground(activity))

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL

        // Changes content background based on Settings - AF
        content.setBackgroundColor(ThemeManager.pageBackground(activity))

        content.setPadding(dp(24), dp(24), dp(24), dp(24))

        // MY PROFILE CARD
        content.addView(makeSectionTitle("My Profile"))
        content.addView(makeProfileCard(currentEmployee))

        // MANAGER SECTION
        if (isManager) {
            // ADD NEW EMPLOYEE BUTTON
            val addBtn = makeActionButton(
                "+ Add New Employee",
                ThemeManager.positive(activity)
            ) {
                showAddEmployeeDialog()
            }
            val addBtnParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addBtnParams.setMargins(0, dp(8), 0, dp(16))
            addBtn.layoutParams = addBtnParams
            content.addView(addBtn)

            // ALL EMPLOYEES SECTION
            content.addView(makeSectionTitle("All Employees"))

            // Search bar
            val searchInput = EditText(activity)
            searchInput.hint = "Search by name or Employee ID..."
            searchInput.textSize = 14f

            // Changes search input colors based on Settings - AF
            searchInput.setTextColor(ThemeManager.primaryText(activity))
            searchInput.setHintTextColor(ThemeManager.mutedText(activity))

            searchInput.setPadding(dp(12), dp(10), dp(12), dp(10))

            searchInput.setBackgroundColor(ThemeManager.inputBackground(activity))

            val searchParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            searchParams.setMargins(0, 0, 0, dp(12))
            searchInput.layoutParams = searchParams

            searchInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    currentSearch = s.toString().trim()
                    filterAndDisplayEmployees()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            content.addView(searchInput, searchParams)

            employeeListContainer = LinearLayout(activity)
            employeeListContainer.orientation = LinearLayout.VERTICAL
            content.addView(employeeListContainer)

            loadAllEmployees()
        }

        scrollView.addView(content)
        page.addView(scrollView, scrollParams)

        return page
    }

    // Loads all employees from Firestore and displays them
    private fun loadAllEmployees() {
        employeeListContainer.removeAllViews()

        val loadingText = TextView(activity)
        loadingText.text = "Loading employees..."
        loadingText.textSize = 14f

        // Changes loading text color based on Settings - AF
        loadingText.setTextColor(ThemeManager.mutedText(activity))

        employeeListContainer.addView(loadingText)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("employees").get().await()
                val employees = snapshot.documents.mapNotNull { doc ->
                    Employee(
                        employeeId = doc.getString("employeeId") ?: "",
                        name = doc.getString("name") ?: "",
                        position = doc.getString("position") ?: "",
                        email = doc.getString("email") ?: "",
                        uid = doc.getString("uid") ?: "",
                        docId = doc.id
                    ) to mapOf(
                        "isLocked" to (doc.getBoolean("isLocked") ?: false),
                        "failedAttempts" to (doc.getLong("failedAttempts") ?: 0L)
                    )
                }

                withContext(Dispatchers.Main) {
                    allEmployeesCache = employees
                    filterAndDisplayEmployees()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    employeeListContainer.removeAllViews()
                    val errorText = TextView(activity)
                    errorText.text = "Error loading employees: ${e.message}"
                    errorText.textSize = 14f

                    // Changes error text color based on Settings - AF
                    errorText.setTextColor(ThemeManager.negative(activity))

                    employeeListContainer.addView(errorText)
                }
            }
        }
    }

    // Filters the cached employee list by the current search term and displays it
    private fun filterAndDisplayEmployees() {
        employeeListContainer.removeAllViews()

        val filtered = if (currentSearch.isEmpty()) {
            allEmployeesCache
        } else {
            allEmployeesCache.filter { (employee, _) ->
                employee.name.contains(currentSearch, ignoreCase = true) ||
                    employee.employeeId.contains(currentSearch, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            val emptyText = TextView(activity)
            emptyText.text = "No employees found"
            emptyText.textSize = 14f

            // Changes empty text color based on Settings - AF
            emptyText.setTextColor(ThemeManager.mutedText(activity))

            employeeListContainer.addView(emptyText)
            return
        }

        filtered.forEach { (employee, meta) ->
            employeeListContainer.addView(makeEmployeeRow(employee, meta))

            val divider = android.view.View(activity)

            // Changes divider color based on Settings - AF
            divider.setBackgroundColor(ThemeManager.divider(activity))

            employeeListContainer.addView(
                divider,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1))
        }
    }

    // Creates a profile card for the current employee
    private fun makeProfileCard(employee: Employee?): LinearLayout {
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL

        // Changes profile card background based on Settings - AF
        card.setBackgroundColor(ThemeManager.sectionBackground(activity))

        card.setPadding(dp(20), dp(20), dp(20), dp(20))

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, dp(8), 0, dp(16))
        card.layoutParams = cardParams

        listOf(
            Pair("Employee ID", employee?.employeeId ?: ""),
            Pair("Name", employee?.name ?: ""),
            Pair("Position", employee?.position ?: "")
        ).forEach { (label, value) ->
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dp(6), 0, dp(6))

            val labelView = TextView(activity)
            labelView.text = "$label:"
            labelView.textSize = 14f

            // Changes profile label color based on Settings - AF
            labelView.setTextColor(ThemeManager.mutedText(activity))

            labelView.setTypeface(null, Typeface.BOLD)
            labelView.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val valueView = TextView(activity)
            valueView.text = value
            valueView.textSize = 14f

            // Changes profile value color based on Settings - AF
            valueView.setTextColor(ThemeManager.primaryText(activity))

            valueView.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)

            row.addView(labelView)
            row.addView(valueView)
            card.addView(row)
        }

        return card
    }

    // Creates a row for each employee in the manager list
    private fun makeEmployeeRow(
        employee: Employee,
        meta: Map<String, Any>
    ): LinearLayout {

        val isLocked = meta["isLocked"] as? Boolean ?: false

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(16), dp(14), dp(16), dp(14))
        // Changes employee row background based on Settings - AF
        row.setBackgroundColor(
            if (isLocked) ThemeManager.emergencyBackground(activity)
            else ThemeManager.pageBackground(activity)
        )

        // Employee info
        val infoColumn = LinearLayout(activity)
        infoColumn.orientation = LinearLayout.VERTICAL
        infoColumn.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val nameText = TextView(activity)
        nameText.text = employee.name
        nameText.textSize = 15f

        // Changes employee name color based on Settings - AF
        nameText.setTextColor(ThemeManager.primaryText(activity))

        nameText.setTypeface(null, Typeface.BOLD)

        val detailText = TextView(activity)
        detailText.text = "${employee.employeeId} • ${employee.position}"
        detailText.textSize = 13f


        // Changes employee detail color based on Settings - AF
        detailText.setTextColor(ThemeManager.mutedText(activity))

        if (isLocked) {
            val lockedText = TextView(activity)
            lockedText.text = "LOCKED"
            lockedText.textSize = 12f

            // Changes locked status color based on Settings - AF
            lockedText.setTextColor(ThemeManager.negative(activity))

            lockedText.setTypeface(null, Typeface.BOLD)
            infoColumn.addView(lockedText)
        }

        infoColumn.addView(nameText)
        infoColumn.addView(detailText)
        row.addView(infoColumn)

        // Action buttons
        val btnColumn = LinearLayout(activity)
        btnColumn.orientation = LinearLayout.HORIZONTAL
        btnColumn.gravity = Gravity.CENTER_VERTICAL

        // Edit button
        // Changes edit button color based on Settings - AF
        val editBtn = makeSmallButton("Edit", ThemeManager.primaryAction(activity))

        editBtn.setOnClickListener {
            showEditEmployeeDialog(employee)
        }
        btnColumn.addView(editBtn)

        // Set Password button
        // Changes password button color based on Settings - AF
        val passwordBtn = makeSmallButton("Set Password", ThemeManager.mutedButton(activity))

        passwordBtn.setOnClickListener {
            showSetPasswordDialog(employee)
        }
        val pwdParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        pwdParams.setMargins(dp(8), 0, 0, 0)
        passwordBtn.layoutParams = pwdParams
        btnColumn.addView(passwordBtn)

        // Unlock button — only shows if account is locked
        if (isLocked) {
            // Changes unlock button color based on Settings - AF
            val unlockBtn = makeSmallButton("Unlock", ThemeManager.warning(activity))
            unlockBtn.setOnClickListener {
                AlertDialog.Builder(activity)
                    .setTitle("Unlock ${employee.name}'s Account?")
                    .setMessage("This will reset their failed login attempts and allow them to log in again.")
                    .setPositiveButton("Unlock") { _, _ ->
                        unlockAccount(employee.docId)
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
            val unlockParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            unlockParams.setMargins(dp(8), 0, 0, 0)
            unlockBtn.layoutParams = unlockParams
            btnColumn.addView(unlockBtn)
        }

        row.addView(btnColumn)

        return row
    }

    // Shows dialog to edit an employee's info
    private fun showEditEmployeeDialog(employee: Employee) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Edit Employee")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val nameLabel = makeDialogLabel("Full Name")
        val nameInput = makeDialogInput(employee.name)

        val idLabel = makeDialogLabel("Employee ID")
        val idInput = makeDialogInput(employee.employeeId)

        val positionLabel = makeDialogLabel("Position")
        val positionSpinner = Spinner(activity)
        val positions = arrayOf("Cashier", "Manager")
        val spinnerAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            positions
        )
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        positionSpinner.adapter = spinnerAdapter
        positionSpinner.setSelection(
            if (employee.position == "Manager") 1 else 0)

        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.setMargins(0, 0, 0, dp(8))
        positionSpinner.layoutParams = spinnerParams

        form.addView(nameLabel)
        form.addView(nameInput)
        form.addView(idLabel)
        form.addView(idInput)
        form.addView(positionLabel)
        form.addView(positionSpinner, spinnerParams)

        builder.setView(form)

        builder.setPositiveButton("Save") { _, _ ->
            val newName = nameInput.text.toString().trim()
            val newId = idInput.text.toString().trim()
            val newPosition = positionSpinner.selectedItem.toString()

            if (newName.isEmpty() || newId.isEmpty()) {
                android.widget.Toast.makeText(
                    activity,
                    "Name and Employee ID are required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            if (newPosition != employee.position) {
                AlertDialog.Builder(activity)
                    .setTitle("Change Position?")
                    .setMessage("${employee.name}'s position will change from " +
                        "${employee.position} to $newPosition. This changes what " +
                        "they can access in the app.")
                    .setPositiveButton("Confirm") { _, _ ->
                        updateEmployee(employee.docId, newName, newId, newPosition)
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()
            } else {
                updateEmployee(employee.docId, newName, newId, newPosition)
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to set a new password for an employee
    private fun showSetPasswordDialog(employee: Employee) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Set Password for ${employee.name}")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val noteText = TextView(activity)
        noteText.text = "You can hand the tablet to the employee to set their own password."
        noteText.textSize = 13f

        // Changes password note text color based on Settings - AF
        noteText.setTextColor(ThemeManager.mutedText(activity))

        noteText.setPadding(0, 0, 0, dp(12))
        form.addView(noteText)

        val passwordLabel = makeDialogLabel("New Password")
        val passwordInput = makeDialogInput("")
        // Star out the password
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        val confirmLabel = makeDialogLabel("Confirm Password")
        val confirmInput = makeDialogInput("")
        confirmInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        form.addView(passwordLabel)
        form.addView(passwordInput)
        form.addView(confirmLabel)
        form.addView(confirmInput)

        builder.setView(form)

        builder.setPositiveButton("Set Password") { _, _ ->
            val password = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()

            if (password.isEmpty()) {
                android.widget.Toast.makeText(
                    activity, "Password cannot be empty",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            if (password != confirm) {
                android.widget.Toast.makeText(
                    activity, "Passwords do not match",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            if (password.length < 6) {
                android.widget.Toast.makeText(
                    activity, "Password must be at least 6 characters",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            setEmployeePassword(employee, password)
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Shows dialog to add a new employee
    private fun showAddEmployeeDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Add New Employee")

        val form = LinearLayout(activity)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(dp(20), dp(10), dp(20), dp(10))

        val nameLabel = makeDialogLabel("Full Name *")
        val nameInput = makeDialogInput("")

        val emailLabel = makeDialogLabel("Email *")
        val emailInput = makeDialogInput("")
        emailInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val positionLabel = makeDialogLabel("Position")
        val positionSpinner = Spinner(activity)
        val positions = arrayOf("Cashier", "Manager")
        val spinnerAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            positions
        )
        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        positionSpinner.adapter = spinnerAdapter

        val spinnerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spinnerParams.setMargins(0, 0, 0, dp(8))
        positionSpinner.layoutParams = spinnerParams

        val passwordLabel = makeDialogLabel("Initial Password *")
        val passwordInput = makeDialogInput("")
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        val noteText = TextView(activity)
        noteText.text = "Employee ID will be auto-generated."
        noteText.textSize = 12f

        // Changes add employee note text color based on Settings - AF
        noteText.setTextColor(ThemeManager.mutedText(activity))

        noteText.setPadding(0, dp(4), 0, 0)

        form.addView(nameLabel)
        form.addView(nameInput)
        form.addView(emailLabel)
        form.addView(emailInput)
        form.addView(positionLabel)
        form.addView(positionSpinner, spinnerParams)
        form.addView(passwordLabel)
        form.addView(passwordInput)
        form.addView(noteText)

        builder.setView(form)

        builder.setPositiveButton("Add Employee") { _, _ ->
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val position = positionSpinner.selectedItem.toString()
            val password = passwordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(
                    activity, "Name, email and password are required",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            if (password.length < 6) {
                android.widget.Toast.makeText(
                    activity, "Password must be at least 6 characters",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            createNewEmployee(name, email, position, password)
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // Updates an employee's info in Firestore
    private fun updateEmployee(
        docId: String,
        name: String,
        employeeId: String,
        position: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("employees")
                    .document(docId)
                    .update(
                        mapOf(
                            "name" to name,
                            "employeeId" to employeeId,
                            "position" to position
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity, "Employee updated successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadAllEmployees()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity, "Error updating employee: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Sets a new password for an employee using Firebase Auth Admin
    private fun setEmployeePassword(employee: Employee, newPassword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // signInWithEmailAndPassword then updatePassword

                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(employee.email, newPassword)

                // Sign in as the employee temporarily to update their password
                val result = auth.signInWithEmailAndPassword(
                    employee.email, newPassword).await()

                result.user?.updatePassword(newPassword)?.await()

                // Sign back in as the manager
                val managerEmail = currentEmployee?.email ?: ""

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Password updated for ${employee.name}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Fallback — send password reset email
                    sendPasswordResetEmail(employee.email)
                }
            }
        }
    }

    // Sends a password reset email as fallback
    private fun sendPasswordResetEmail(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Password reset email sent",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Creates a new employee in Firebase Auth and Firestore
    private fun createNewEmployee(
        name: String,
        email: String,
        position: String,
        password: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Auto-generate Employee ID based on current count
                val snapshot = db.collection("employees").get().await()
                val nextNumber = snapshot.size() + 1
                val year = java.util.Calendar.getInstance()
                    .get(java.util.Calendar.YEAR)
                val employeeId = "EMP-$year-" +
                    nextNumber.toString().padStart(4, '0')

                // Create Firebase Auth account
                val authResult = auth.createUserWithEmailAndPassword(
                    email, password).await()
                val uid = authResult.user?.uid ?: ""

                // Create Firestore employee document
                val newEmployee = hashMapOf(
                    "employeeId" to employeeId,
                    "name" to name,
                    "email" to email,
                    "position" to position,
                    "uid" to uid,
                    "failedAttempts" to 0L,
                    "isLocked" to false
                )

                db.collection("employees").add(newEmployee).await()

                val managerEmail = currentEmployee?.email ?: ""

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "$employeeId created for $name. Please log back in.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    // Sign out and go back to login
                    auth.signOut()
                    SessionManager.clear()
                    val intent = android.content.Intent(
                        activity, LoginActivity::class.java)
                    intent.flags =
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error creating employee: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Unlocks a locked employee account
    private fun unlockAccount(docId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("employees")
                    .document(docId)
                    .update(
                        mapOf(
                            "isLocked" to false,
                            "failedAttempts" to 0L
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Account unlocked successfully",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadAllEmployees()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        activity,
                        "Error unlocking account: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Creates a section title
    private fun makeSectionTitle(text: String): TextView {
        val title = TextView(activity)
        title.text = text
        title.textSize = 18f

        // Changes section title color based on Settings - AF
        title.setTextColor(ThemeManager.primaryText(activity))

        title.setTypeface(null, Typeface.BOLD)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(8))
        title.layoutParams = params
        return title
    }

    // Creates an action button
    private fun makeActionButton(
        text: String,
        color: Int,
        onClick: () -> Unit
    ): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 14f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(16), dp(10), dp(16), dp(10))
        btn.setOnClickListener { onClick() }
        return btn
    }

    // Creates a small button for employee rows
    private fun makeSmallButton(text: String, color: Int): TextView {
        val btn = TextView(activity)
        btn.text = text
        btn.textSize = 12f
        btn.gravity = Gravity.CENTER
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(color)
        btn.setPadding(dp(10), dp(6), dp(10), dp(6))
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        return btn
    }

    // Creates a dialog label
    private fun makeDialogLabel(text: String): TextView {
        val label = TextView(activity)
        label.text = text
        label.textSize = 13f

        // Changes dialog label color based on Settings - AF
        label.setTextColor(ThemeManager.mutedText(activity))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, dp(10), 0, dp(4))
        label.layoutParams = params
        return label
    }

    // Creates a dialog input field
    private fun makeDialogInput(defaultValue: String): EditText {
        val input = EditText(activity)
        input.setText(defaultValue)
        input.textSize = 14f

        // Changes dialog input colors based on Settings - AF
        input.setTextColor(ThemeManager.primaryText(activity))

        input.setPadding(dp(8), dp(8), dp(8), dp(8))

        input.setBackgroundColor(ThemeManager.inputBackground(activity))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(4))
        input.layoutParams = params
        return input
    }

    // Converts dp to pixels
    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}

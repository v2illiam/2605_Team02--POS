package com.liquor.ledger

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.liquor.ledger.firebase.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// LoginActivity is the first screen employees see when opening the app

class LoginActivity : Activity() {

    // Auth repository handles the Employee ID login logic
    private val authRepository = AuthRepository()

    // UI elements declared
    private lateinit var employeeIdInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var errorText: TextView
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already logged in skip login and go straight to MainActivity
        if (authRepository.currentUser != null) {
            goToMain()
            return
        }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.HORIZONTAL
        root.setBackgroundColor(Color.WHITE)

        val leftPanel = LinearLayout(this)
        leftPanel.orientation = LinearLayout.VERTICAL
        leftPanel.setBackgroundColor(Color.rgb(16, 30, 55))
        leftPanel.gravity = Gravity.CENTER

        val leftParams = LinearLayout.LayoutParams(
            dp(280),
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        // App title
        val appTitle = TextView(this)
        appTitle.text = "Liquor\nLedger"
        appTitle.textSize = 28f
        appTitle.setTextColor(Color.WHITE)
        appTitle.gravity = Gravity.CENTER
        appTitle.setTypeface(null, Typeface.BOLD)
        appTitle.setPadding(dp(20), 0, dp(20), dp(8))

        // Subtitle
        val appSubtitle = TextView(this)
        appSubtitle.text = "Business Tracker"
        appSubtitle.textSize = 14f
        appSubtitle.setTextColor(Color.rgb(156, 163, 175))
        appSubtitle.gravity = Gravity.CENTER

        leftPanel.addView(appTitle)
        leftPanel.addView(appSubtitle)

        // Right side login form
        val rightPanel = LinearLayout(this)
        rightPanel.orientation = LinearLayout.VERTICAL
        rightPanel.gravity = Gravity.CENTER
        rightPanel.setBackgroundColor(Color.rgb(248, 249, 250))

        val rightParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        // Login card container
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundColor(Color.WHITE)
        card.setPadding(dp(40), dp(40), dp(40), dp(40))

        val cardParams = LinearLayout.LayoutParams(
            dp(380),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Login title
        val loginTitle = TextView(this)
        loginTitle.text = "Employee Login"
        loginTitle.textSize = 24f
        loginTitle.setTextColor(Color.BLACK)
        loginTitle.setTypeface(null, Typeface.BOLD)

        // Login subtitle
        val loginSubtitle = TextView(this)
        loginSubtitle.text = "Sign in with your Employee ID"
        loginSubtitle.textSize = 14f
        loginSubtitle.setTextColor(Color.rgb(107, 114, 128))
        loginSubtitle.setPadding(0, dp(4), 0, dp(24))

        // Employee ID label
        val idLabel = TextView(this)
        idLabel.text = "Employee ID"
        idLabel.textSize = 14f
        idLabel.setTextColor(Color.rgb(55, 65, 81))
        idLabel.setPadding(0, 0, 0, dp(6))

        // Employee ID input field
        employeeIdInput = EditText(this)
        employeeIdInput.hint = "e.g. 0001"
        employeeIdInput.textSize = 15f
        employeeIdInput.setTextColor(Color.BLACK)
        employeeIdInput.setHintTextColor(Color.LTGRAY)
        employeeIdInput.setPadding(dp(12), dp(12), dp(12), dp(12))
        employeeIdInput.setBackgroundColor(Color.rgb(243, 244, 246))

        val inputParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        inputParams.setMargins(0, 0, 0, dp(16))

        // Password label
        val passwordLabel = TextView(this)
        passwordLabel.text = "Password"
        passwordLabel.textSize = 14f
        passwordLabel.setTextColor(Color.rgb(55, 65, 81))
        passwordLabel.setPadding(0, 0, 0, dp(6))

        // Password input field
        // Hides the password characters as dots by default
        passwordInput = EditText(this)
        passwordInput.hint = "Enter your password"
        passwordInput.inputType = 129
        passwordInput.textSize = 15f
        passwordInput.setTextColor(Color.BLACK)
        passwordInput.setHintTextColor(Color.LTGRAY)
        passwordInput.setPadding(dp(12), dp(12), dp(12), dp(12))
        passwordInput.setBackgroundColor(Color.rgb(243, 244, 246))

        val passwordFieldParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        // Show/Hide toggle button next to the password field
        var passwordVisible = false
        val togglePasswordBtn = TextView(this)
        togglePasswordBtn.text = "Show"
        togglePasswordBtn.textSize = 13f
        togglePasswordBtn.setTextColor(Color.rgb(45, 95, 255))
        togglePasswordBtn.gravity = Gravity.CENTER
        togglePasswordBtn.setPadding(dp(12), 0, dp(4), 0)
        togglePasswordBtn.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordBtn.text = "Hide"
            } else {
                passwordInput.inputType = 129
                togglePasswordBtn.text = "Show"
            }
            // Keep cursor at the end after switching input type
            passwordInput.setSelection(passwordInput.text.length)
        }

        // Row containing the password field and the toggle button
        val passwordRow = LinearLayout(this)
        passwordRow.orientation = LinearLayout.HORIZONTAL
        passwordRow.gravity = Gravity.CENTER_VERTICAL
        passwordRow.setBackgroundColor(Color.rgb(243, 244, 246))

        val passwordParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        passwordParams.setMargins(0, 0, 0, dp(8))

        // Error message box hidden until there is an error
        // Styled as a light red background box instead of plain text
        errorText = TextView(this)
        errorText.text = ""
        errorText.textSize = 13f
        errorText.setTextColor(Color.rgb(185, 28, 28))
        errorText.setBackgroundColor(Color.rgb(254, 226, 226))
        errorText.setPadding(dp(12), dp(10), dp(12), dp(10))
        errorText.visibility = View.GONE

        val errorParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        errorParams.setMargins(0, dp(4), 0, dp(16))
        // Loading spinner hidden until login is in progress
        progressBar = ProgressBar(this)
        progressBar.visibility = View.GONE

        val progressParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        progressParams.gravity = Gravity.CENTER_HORIZONTAL
        progressParams.setMargins(0, dp(8), 0, dp(8))

        // Sign In button
        loginButton = Button(this)
        loginButton.text = "Sign In"
        loginButton.textSize = 16f
        loginButton.setTextColor(Color.WHITE)
        loginButton.setBackgroundColor(Color.rgb(45, 95, 255))

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(50)
        )
        buttonParams.setMargins(0, dp(8), 0, 0)

        // Make the password field show "Done" on the keyboard
        // and trigger login when pressed
        passwordInput.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }

        // Button click attempt login
        loginButton.setOnClickListener {
            attemptLogin()
        }

        // Add all views to card
        card.addView(loginTitle)
        card.addView(loginSubtitle)
        card.addView(idLabel)
        card.addView(employeeIdInput, inputParams)
        card.addView(passwordLabel)
        passwordInput.setBackgroundColor(Color.TRANSPARENT)
        passwordRow.addView(passwordInput, passwordFieldParams)
        passwordRow.addView(togglePasswordBtn)
        card.addView(passwordRow, passwordParams)
        card.addView(errorText, errorParams)
        card.addView(progressBar, progressParams)
        card.addView(loginButton, buttonParams)

        rightPanel.addView(card, cardParams)

        // Add panels to root
        root.addView(leftPanel, leftParams)
        root.addView(rightPanel, rightParams)

        setContentView(root)
    }

    // Handles the login attempt when Sign In is clicked
    private fun attemptLogin() {
        val employeeId = employeeIdInput.text.toString().trim()
        val password = passwordInput.text.toString()

        // Validate fields are not empty
        if (employeeId.isEmpty() || password.isEmpty()) {
            showError("Please enter your Employee ID and password")
            return
        }

        // Show loading state
        setLoading(true)

        // Launch login in background thread so UI doesn't freeze
        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.loginWithEmployeeId(employeeId, password)

            // Switch back to main thread to update UI
            withContext(Dispatchers.Main) {
                setLoading(false)
                result.onSuccess { employee ->
                    // Save employee to session
                    SessionManager.currentEmployee = employee
                    // Go to main app
                    goToMain()
                }
                result.onFailure { error ->
                    val message = when {
                        error.message?.contains("Employee ID not found") == true ->
                            "Employee ID not found"
                        error.message?.contains("password") == true ->
                            "Incorrect password"
                        else ->
                            "Login failed. Please try again."
                    }
                    showError(message)
                }
            }
        }
    }

    // Shows an error message below the password field
    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    // Shows or hides the loading spinner and disables the button while loading
    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        errorText.visibility = View.GONE
    }

    // Navigates to MainActivity after successful login
    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

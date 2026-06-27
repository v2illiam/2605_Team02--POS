package com.liquor.ledger

// Stores the currently logged in employee's information
// so any screen can access it without hitting Firestore every time
// It's an object so there's only one instance shared across the whole app

object SessionManager
{

    // The currently logged in employee
    // Null means nobody is logged in
    var currentEmployee: Employee? = null

    // Checks if the logged in employee is a Manager
    // Used to show or hide screens like Reports and Purchase Orders
    val isManager: Boolean
        get() = currentEmployee?.position == "Manager"

    // Clears the session when the user logs out
    fun clear()
    {

        currentEmployee = null

    }
}

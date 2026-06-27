package com.liquor.ledger

// This data class represents an employee
// It maps directly to documents in the Firestore employees collection

data class Employee(
    val employeeId: String = "",
    val name: String = "",
    val position: String = "",      // Controls screen access
    val email: String = "",         // used for Firebase Auth login behind the scenes
    val uid: String = "",            // links to Firebase Auth user
    val docId: String = ""
)

package com.liquor.ledger.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.Employee
import kotlinx.coroutines.tasks.await

// AuthRepository handles all authentication operations
// Supports Employee ID login with account lockout after 5 failed attempts

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseManager.auth
    private val db: FirebaseFirestore = FirebaseManager.db

    // Returns the currently logged in user or null
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // Login using last 4 digits of Employee ID and password
    // Checks for account lockout before attempting Firebase Auth
    suspend fun loginWithEmployeeId(
        employeeId: String,
        password: String
    ): Result<Employee> {
        return try {
            // Get all employees and find matching last 4 digits
            val snapshot = db.collection("employees").get().await()

            val matchingDoc = snapshot.documents.firstOrNull { doc ->
                val fullId = doc.getString("employeeId") ?: ""
                fullId.endsWith(employeeId)
            }

            if (matchingDoc == null) {
                return Result.failure(Exception("Employee ID not found"))
            }

            // Check if account is locked
            val isLocked = matchingDoc.getBoolean("isLocked") ?: false
            if (isLocked) {
                return Result.failure(
                    Exception("Account locked. Contact your manager to unlock."))
            }

            val email = matchingDoc.getString("email")
                ?: return Result.failure(Exception("No email on file"))

            val employee = Employee(
                employeeId = matchingDoc.getString("employeeId") ?: "",
                name = matchingDoc.getString("name") ?: "",
                position = matchingDoc.getString("position") ?: "Cashier",
                email = email,
                uid = matchingDoc.getString("uid") ?: "",
                docId = matchingDoc.id
            )

            // Attempt Firebase Auth login
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                // Login failed — increment failed attempts
                val currentAttempts = matchingDoc.getLong("failedAttempts") ?: 0L
                val newAttempts = currentAttempts + 1

                if (newAttempts >= 5) {
                    // Lock the account
                    db.collection("employees")
                        .document(matchingDoc.id)
                        .update(
                            mapOf(
                                "failedAttempts" to newAttempts,
                                "isLocked" to true
                            )
                        )
                        .await()
                    return Result.failure(
                        Exception("Too many failed attempts. Account locked. Contact your manager."))
                } else {
                    // Increment failed attempts
                    db.collection("employees")
                        .document(matchingDoc.id)
                        .update("failedAttempts", newAttempts)
                        .await()
                    val remaining = 5 - newAttempts
                    return Result.failure(
                        Exception("Incorrect password. $remaining attempt(s) remaining."))
                }
            }

            // Login successful — reset failed attempts
            db.collection("employees")
                .document(matchingDoc.id)
                .update(
                    mapOf(
                        "failedAttempts" to 0L,
                        "isLocked" to false
                    )
                )
                .await()

            Result.success(employee)

        } catch (e: Exception) {
        Result.failure(e)
    }
    }

    // Logs out the current user
    fun logout() = auth.signOut()
}

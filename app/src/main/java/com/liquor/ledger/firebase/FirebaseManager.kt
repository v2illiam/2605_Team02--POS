// This file is the central access point for all Firebase services in the app.


package com.liquor.ledger.firebase

// Import Firebase Authentication — handles user login and logout
import com.google.firebase.auth.FirebaseAuth

// Import Firebase Firestore — handles all database read/write operations
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager
{

    // auth gives access to Firebase Authentication
    // lazy is used to ensure that it is only initialized the fist time it is needed
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // db gives access to the database
    // used to read and write firestore collections
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

}


package com.example.stealthchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Check if user is signed in (non-null) and redirect to LoginActivity if not
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Debug log to check Firebase setup
        Log.d("FirebaseCheck", "🔥 Firebase initialized successfully!")

        // Continue with the rest of your MainActivity code (e.g., setContent, etc.)
    }
}

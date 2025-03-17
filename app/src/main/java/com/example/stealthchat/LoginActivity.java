package com.example.stealthchat;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // dep change here
import androidx.activity.result.IntentSenderRequest; // dep change here
import androidx.activity.result.contract.ActivityResultContracts; // dep change here
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.identity.BeginSignInRequest; // dep change here
import com.google.android.gms.auth.api.identity.Identity; // dep change here
import com.google.android.gms.auth.api.identity.SignInClient; // dep change here
import com.google.android.gms.auth.api.identity.SignInCredential; // dep change here
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    // New Identity API client and request (dep change here)
    private SignInClient oneTapClient; // dep change here
    private BeginSignInRequest signInRequest; // dep change here
    private ActivityResultLauncher<IntentSenderRequest> signInLauncher; // dep change here

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Configure the new Identity API for Google Sign-In (dep change here)
        oneTapClient = Identity.getSignInClient(this); // dep change here
        signInRequest = BeginSignInRequest.builder() // dep change here
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder() // dep change here
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                )
                .build();

        // Register the activity result launcher for the new sign-in flow (dep change here)
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(), // dep change here
                result -> { // dep change here
                    if (result.getResultCode() == RESULT_OK) { // dep change here
                        try {
                            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData()); // dep change here
                            String idToken = credential.getGoogleIdToken();
                            String displayName = credential.getDisplayName();
                            if (idToken != null) {
                                firebaseAuthWithGoogle(idToken, displayName);
                            } else {
                                Log.w("LoginActivity", "No ID token!");
                                Toast.makeText(LoginActivity.this, "Sign in failed: No ID token", Toast.LENGTH_SHORT).show();
                            }
                        } catch (ApiException e) {
                            Log.w("LoginActivity", "Google sign in failed", e);
                            Toast.makeText(LoginActivity.this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Set the click listener for the Google Sign-In button
        SignInButton signInButton = findViewById(R.id.btnGoogleSignIn);
        signInButton.setOnClickListener(view -> signIn());
    }

    private void signIn() {
        // Begin the sign-in flow using the new Identity API (dep change here)
        oneTapClient.beginSignIn(signInRequest) // dep change here
                .addOnSuccessListener(result -> {
                    try {
                        IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build(); // dep change here
                        signInLauncher.launch(intentSenderRequest); // dep change here
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Sign in failed: " + e.getLocalizedMessage());
                    Toast.makeText(LoginActivity.this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Modified to accept the idToken and displayName directly (dep change here)
    private void firebaseAuthWithGoogle(String idToken, String displayName) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            // Retrieve the FCM token
                            FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(tokenTask -> {
                                        if (tokenTask.isSuccessful()) {
                                            String fcmToken = tokenTask.getResult();
                                            Log.d("LoginActivity", "FCM Token: " + fcmToken); // Debug print

                                            // Prepare user data
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("uid", user.getUid());
                                            // Use displayName from sign-in credential if available, otherwise fallback to FirebaseUser displayName
                                            userData.put("name", displayName != null ? displayName : (user.getDisplayName() != null ? user.getDisplayName() : "Unknown"));
                                            userData.put("fcmToken", fcmToken);

                                            // Debug print the entire data that will be stored (dep change here)
                                            Log.d("LoginActivity", "Storing user data: " + userData.toString()); // dep change here

                                            // Store user data in Firestore under the collection "users"
                                            firestore.collection("users")
                                                    .document(user.getUid())
                                                    .set(userData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d("LoginActivity", "User data stored successfully.");
                                                        // Navigate to home screen or next activity here if needed
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("LoginActivity", "Error storing user data", e);
                                                    });
                                        } else {
                                            Log.e("LoginActivity", "FCM token retrieval failed", tokenTask.getException());
                                        }
                                    });
                        }
                    } else {
                        Log.w("LoginActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


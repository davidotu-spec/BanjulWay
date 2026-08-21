package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data structure representing a logged authentication or session event.
 */
data class AuthLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val eventType: AuthEventType,
    val message: String,
    val userUid: String? = null,
    val userEmail: String? = null,
    val isSuccess: Boolean = true,
    val diagnosticReason: String? = null,
    val resolutionSuggestion: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

enum class AuthEventType {
    SESSION_CHANGE,
    SIGN_IN_SUCCESS,
    SIGN_IN_FAILURE,
    SIGN_UP_SUCCESS,
    SIGN_UP_FAILURE,
    SIGN_OUT,
    TOKEN_REFRESH,
    INITIALIZATION,
    CONSOLE_DIAGNOSTIC
}

/**
 * AuthLogger observes FirebaseAuth state changes via [FirebaseAuth.AuthStateListener]
 * and provides detailed diagnostic logging for sign-in successes, sign-in/sign-up failures,
 * and user session lifecycles.
 *
 * It specifically monitors and helps diagnose why created users may or may not appear in the
 * Firebase Authentication Console (e.g., placeholder API keys, project mismatches, uncommitted sessions).
 */
object AuthLogger {
    private const val TAG = "WayGoAuthLogger"
    private const val MAX_LOG_HISTORY = 150

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var idTokenListener: FirebaseAuth.IdTokenListener? = null
    private var isObserving = false

    private val _logEntries = MutableStateFlow<List<AuthLogEntry>>(emptyList())
    val logEntries: StateFlow<List<AuthLogEntry>> = _logEntries.asStateFlow()

    private val _currentSessionSummary = MutableStateFlow<String>("AuthLogger Initializing...")
    val currentSessionSummary: StateFlow<String> = _currentSessionSummary.asStateFlow()

    /**
     * Attaches AuthStateListener to FirebaseAuth.getInstance() and begins logging.
     */
    @Synchronized
    fun startObserving(context: Context? = null) {
        if (isObserving) {
            Log.d(TAG, "AuthLogger is already observing FirebaseAuth state.")
            return
        }

        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                Log.w(TAG, "FirebaseApp has not been initialized yet. Initializing default app...")
                FirebaseApp.initializeApp(context)
            }

            val auth = try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Cannot get FirebaseAuth instance for AuthLogger: ${e.message}", e)
                recordEntry(
                    AuthLogEntry(
                        tag = "Init",
                        eventType = AuthEventType.INITIALIZATION,
                        message = "FirebaseAuth instance unavailable: ${e.message}",
                        isSuccess = false,
                        diagnosticReason = "FirebaseApp is either not initialized or google-services.json is missing.",
                        resolutionSuggestion = "Ensure google-services.json is present in app/ and contains valid project credentials."
                    )
                )
                return
            }

            // Inspect Google App Configuration
            inspectFirebaseConfig(auth)

            // AuthStateListener observing user session transitions
            authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                handleAuthStateChange(currentUser)
            }

            // Attach listener
            auth.addAuthStateListener(authStateListener!!)

            // Also attach IdTokenListener for tracking token refreshes
            idTokenListener = FirebaseAuth.IdTokenListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    Log.d(TAG, "ID Token refreshed for user: ${user.uid} (${user.email ?: "no-email"})")
                    recordEntry(
                        AuthLogEntry(
                            tag = "Token",
                            eventType = AuthEventType.TOKEN_REFRESH,
                            message = "Firebase ID Token refreshed for user.",
                            userUid = user.uid,
                            userEmail = user.email,
                            isSuccess = true
                        )
                    )
                }
            }
            auth.addIdTokenListener(idTokenListener!!)

            isObserving = true
            Log.i(TAG, "FirebaseAuth.AuthStateListener successfully registered with AuthLogger.")
            recordEntry(
                AuthLogEntry(
                    tag = "Init",
                    eventType = AuthEventType.INITIALIZATION,
                    message = "AuthLogger started observing FirebaseAuth state.",
                    isSuccess = true
                )
            )

            // Trigger initial state evaluation
            handleAuthStateChange(auth.currentUser)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AuthLogger: ${e.message}", e)
        }
    }

    /**
     * Inspects configuration and logs diagnostic reasons if credentials will block Console visibility.
     */
    private fun inspectFirebaseConfig(auth: FirebaseAuth) {
        val app = auth.app
        val options = app.options
        val apiKey = options.apiKey
        val projectId = options.projectId
        val appId = options.applicationId

        val isPlaceholderKey = apiKey.contains("Placeholder", ignoreCase = true) || apiKey.length < 20

        val configMsg = "Firebase Config: ProjectId='$projectId', AppId='$appId', API Key Masked='${maskApiKey(apiKey)}'"
        Log.i(TAG, configMsg)

        if (isPlaceholderKey) {
            val diagnostic = "API key '$apiKey' is a placeholder. Firebase Authentication will NOT persist users to Firebase Console until a real Web API Key is supplied."
            val resolution = "Download google-services.json with valid credentials from the Firebase Console (Project Settings > General) and place it in the app/ module."
            Log.w(TAG, "⚠️ CONSOLE WARNING: $diagnostic | Resolution: $resolution")
            recordEntry(
                AuthLogEntry(
                    tag = "ConfigCheck",
                    eventType = AuthEventType.CONSOLE_DIAGNOSTIC,
                    message = "Placeholder Firebase API Key Detected",
                    isSuccess = false,
                    diagnosticReason = diagnostic,
                    resolutionSuggestion = resolution
                )
            )
        } else {
            recordEntry(
                AuthLogEntry(
                    tag = "ConfigCheck",
                    eventType = AuthEventType.CONSOLE_DIAGNOSTIC,
                    message = "Live Firebase API key configured for project '$projectId'.",
                    isSuccess = true
                )
            )
        }
    }

    /**
     * Handles changes triggered by [FirebaseAuth.AuthStateListener].
     */
    private fun handleAuthStateChange(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val providers = currentUser.providerData.map { it.providerId }.joinToString(", ")
            val creationTime = currentUser.metadata?.creationTimestamp?.let {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))
            } ?: "unknown"
            val lastSignIn = currentUser.metadata?.lastSignInTimestamp?.let {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))
            } ?: "unknown"

            val details = "User Signed In -> UID: ${currentUser.uid}, Email: ${currentUser.email ?: "N/A"}, Verified: ${currentUser.isEmailVerified}, Providers: [$providers], Created: $creationTime, LastSignIn: $lastSignIn"
            Log.i(TAG, details)

            _currentSessionSummary.value = "Active Session: ${currentUser.email ?: currentUser.uid} (Verified: ${currentUser.isEmailVerified})"

            recordEntry(
                AuthLogEntry(
                    tag = "AuthState",
                    eventType = AuthEventType.SESSION_CHANGE,
                    message = "User session active (UID: ${currentUser.uid})",
                    userUid = currentUser.uid,
                    userEmail = currentUser.email,
                    isSuccess = true,
                    diagnosticReason = if (currentUser.isAnonymous) "User is anonymous; will appear in Firebase Console if Anonymous Auth is enabled." else "Authenticated via providers: [$providers]"
                )
            )
        } else {
            Log.i(TAG, "User Signed Out (Current session is NULL).")
            _currentSessionSummary.value = "No Active Firebase User Session"

            recordEntry(
                AuthLogEntry(
                    tag = "AuthState",
                    eventType = AuthEventType.SIGN_OUT,
                    message = "User is signed out (FirebaseAuth.currentUser == null).",
                    isSuccess = true
                )
            )
        }
    }

    /**
     * Explicitly log a successful sign-in operation.
     */
    fun logSignInSuccess(user: FirebaseUser?, method: String) {
        val uid = user?.uid ?: "unknown"
        val email = user?.email ?: "no-email"
        val msg = "Sign-In Succeeded via $method (UID: $uid, Email: $email)"
        Log.i(TAG, "✅ $msg")

        recordEntry(
            AuthLogEntry(
                tag = "SignIn",
                eventType = AuthEventType.SIGN_IN_SUCCESS,
                message = msg,
                userUid = uid,
                userEmail = email,
                isSuccess = true,
                diagnosticReason = "Authentication token generated successfully. User should appear in Firebase Console under Authentication > Users."
            )
        )
    }

    /**
     * Explicitly log a successful sign-up / registration operation.
     */
    fun logSignUpSuccess(user: FirebaseUser?, method: String) {
        val uid = user?.uid ?: "unknown"
        val email = user?.email ?: "no-email"
        val msg = "Sign-Up Succeeded via $method: User registered in Firebase (UID: $uid, Email: $email)"
        Log.i(TAG, "🎉 $msg")

        recordEntry(
            AuthLogEntry(
                tag = "SignUp",
                eventType = AuthEventType.SIGN_UP_SUCCESS,
                message = msg,
                userUid = uid,
                userEmail = email,
                isSuccess = true,
                diagnosticReason = "New user record created in Firebase Auth. Check Firebase Console > Project 'banjulway' > Authentication."
            )
        )
    }

    /**
     * Explicitly log an authentication failure with root cause analysis.
     */
    fun logAuthFailure(
        actionType: AuthEventType,
        method: String,
        exception: Throwable?,
        identifier: String? = null
    ) {
        val errorMsg = exception?.localizedMessage ?: exception?.message ?: "Unknown authentication error"
        val (diagnostic, resolution) = diagnoseAuthException(exception, errorMsg)

        val logMessage = "Auth Failed [$method] for '$identifier': $errorMsg"
        Log.e(TAG, "❌ $logMessage | Cause: $diagnostic | Suggested Action: $resolution", exception)

        recordEntry(
            AuthLogEntry(
                tag = "AuthFailure",
                eventType = actionType,
                message = logMessage,
                userEmail = identifier,
                isSuccess = false,
                diagnosticReason = diagnostic,
                resolutionSuggestion = resolution
            )
        )
    }

    /**
     * Diagnoses common Firebase Authentication exceptions that cause accounts not to register or appear in console.
     */
    private fun diagnoseAuthException(exception: Throwable?, message: String): Pair<String, String> {
        val msgLower = message.lowercase(Locale.ROOT)
        return when {
            exception is FirebaseAuthWeakPasswordException || msgLower.contains("weak-password") -> {
                Pair(
                    "Password does not meet Firebase minimum security criteria (at least 6 characters required).",
                    "Prompt user for a stronger password with at least 6 characters."
                )
            }
            exception is FirebaseAuthUserCollisionException || msgLower.contains("email-already-in-use") -> {
                Pair(
                    "The email address is already registered in Firebase Console.",
                    "Sign in with the existing account instead of creating a new one, or use Password Reset."
                )
            }
            exception is FirebaseAuthInvalidCredentialsException || msgLower.contains("invalid-credential") || msgLower.contains("badly formatted") -> {
                Pair(
                    "Invalid email format, expired token, or incorrect password credentials.",
                    "Verify the email format (e.g. user@domain.com) and ensure the password is correct."
                )
            }
            exception is FirebaseAuthInvalidUserException || msgLower.contains("user-not-found") -> {
                Pair(
                    "No user record found in Firebase Authentication corresponding to this email.",
                    "Switch to Registration mode to create the user account in Firebase first."
                )
            }
            msgLower.contains("api key not valid") || msgLower.contains("api-key-not-valid") || msgLower.contains("invalid_api_key") -> {
                Pair(
                    "The Google API Key in google-services.json is invalid or restricted in Google Cloud Console.",
                    "Replace app/google-services.json with the valid file from Firebase Console > Project Settings."
                )
            }
            msgLower.contains("internal-error") || msgLower.contains("internal error") -> {
                Pair(
                    "Firebase Authentication server returned an internal error (often caused by placeholder API key or service restrictions).",
                    "Check network connectivity and verify the Web API Key in google-services.json."
                )
            }
            msgLower.contains("network") || msgLower.contains("unreachable") || msgLower.contains("timeout") -> {
                Pair(
                    "Network communication with Firebase Auth endpoints (identitytoolkit.googleapis.com) failed.",
                    "Ensure device/emulator has active internet connectivity and DNS resolution."
                )
            }
            else -> {
                Pair(
                    "Firebase Auth rejected the operation: $message",
                    "Review Firebase Console Authentication settings to verify that Email/Password provider is enabled."
                )
            }
        }
    }

    private fun maskApiKey(apiKey: String): String {
        return if (apiKey.length > 8) {
            "${apiKey.take(6)}...${apiKey.takeLast(4)}"
        } else {
            "***"
        }
    }

    private fun recordEntry(entry: AuthLogEntry) {
        val currentList = _logEntries.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > MAX_LOG_HISTORY) {
            _logEntries.value = currentList.take(MAX_LOG_HISTORY)
        } else {
            _logEntries.value = currentList
        }
    }

    /**
     * Clears in-memory audit log list.
     */
    fun clearLogs() {
        _logEntries.value = emptyList()
    }

    /**
     * Removes the registered listeners.
     */
    @Synchronized
    fun stopObserving() {
        try {
            val auth = FirebaseAuth.getInstance()
            authStateListener?.let { auth.removeAuthStateListener(it) }
            idTokenListener?.let { auth.removeIdTokenListener(it) }
            authStateListener = null
            idTokenListener = null
            isObserving = false
            Log.i(TAG, "AuthLogger stopped observing.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AuthLogger: ${e.message}", e)
        }
    }
}

package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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
 * Diagnostic log item holding timestamped diagnostics and severity.
 */
data class AuthDiagnosticLog(
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date()),
    val tag: String,
    val level: LogLevel,
    val message: String,
    val details: String? = null,
    val actionableResolution: String? = null
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, CRITICAL
}

/**
 * Diagnostic report summarizing the current Firebase Authentication configuration.
 */
data class DiagnosticAuthReport(
    val isFirebaseInitialized: Boolean,
    val projectId: String,
    val applicationId: String,
    val apiKeyMasked: String,
    val isPlaceholderApiKey: Boolean,
    val currentUserId: String?,
    val currentUserEmail: String?,
    val isEmailVerified: Boolean,
    val statusSummary: String
)

/**
 * DiagnosticAuthManager
 *
 * Dedicated diagnostic manager that initializes Firebase, tracks auth state changes,
 * provides instrumented wrappers for signInWithEmailAndPassword, createUserWithEmailAndPassword,
 * and pinpoints root causes for runtime errors like "Internal Error" or "API Key not Valid".
 */
object DiagnosticAuthManager {
    private const val TAG = "DiagnosticAuthManager"

    private val _diagnosticLogs = MutableStateFlow<List<AuthDiagnosticLog>>(emptyList())
    val diagnosticLogs: StateFlow<List<AuthDiagnosticLog>> = _diagnosticLogs.asStateFlow()

    private val _latestReport = MutableStateFlow<DiagnosticAuthReport?>(null)
    val latestReport: StateFlow<DiagnosticAuthReport?> = _latestReport.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var isInitialized = false

    /**
     * Initializes Firebase diagnostic monitoring and attaches AuthStateListener.
     */
    fun initialize(context: Context): FirebaseAuth? {
        log(LogLevel.INFO, "Init", "DiagnosticAuthManager initializing...", "Context: ${context.packageName}")
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                log(LogLevel.INFO, "Init", "FirebaseApp not yet initialized. Initializing from default resources...")
                FirebaseApp.initializeApp(context)
            }

            val app = FirebaseApp.getInstance()
            val options: FirebaseOptions = app.options

            val isPlaceholder = options.apiKey.contains("Placeholder", ignoreCase = true) ||
                    options.apiKey.length < 20 ||
                    !options.apiKey.startsWith("AIza")

            val apiKeyPreview = if (options.apiKey.length > 8) {
                "${options.apiKey.take(6)}...${options.apiKey.takeLast(4)}"
            } else {
                options.apiKey
            }

            log(
                level = if (isPlaceholder) LogLevel.WARN else LogLevel.INFO,
                tag = "FirebaseConfig",
                message = "Firebase Config loaded: ProjectId='${options.projectId ?: "unknown"}', AppId='${options.applicationId}'",
                details = "API Key: $apiKeyPreview (IsPlaceholder: $isPlaceholder)",
                actionableResolution = if (isPlaceholder) {
                    "Replace app/google-services.json with the official JSON downloaded from Firebase Console to provide a live Google Cloud API key."
                } else null
            )

            val auth = FirebaseAuth.getInstance()

            // Attach AuthStateListener to monitor state transitions
            if (authStateListener == null) {
                authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val user: FirebaseUser? = firebaseAuth.currentUser
                    if (user != null) {
                        log(
                            level = LogLevel.INFO,
                            tag = "AuthStateChange",
                            message = "User SIGNED_IN: ${user.email ?: "Anonymous"} (UID: ${user.uid})",
                            details = "EmailVerified: ${user.isEmailVerified}, Providers: ${user.providerData.map { it.providerId }}"
                        )
                    } else {
                        log(
                            level = LogLevel.INFO,
                            tag = "AuthStateChange",
                            message = "User SIGNED_OUT (No active Firebase session)"
                        )
                    }
                    updateReport(auth)
                }
                auth.addAuthStateListener(authStateListener!!)
                log(LogLevel.DEBUG, "Init", "FirebaseAuth AuthStateListener attached successfully.")
            }

            isInitialized = true
            updateReport(auth)
            return auth
        } catch (e: Exception) {
            log(
                level = LogLevel.ERROR,
                tag = "InitializationError",
                message = "Failed to initialize Firebase DiagnosticAuthManager: ${e.message}",
                details = e.stackTraceToString(),
                actionableResolution = "Ensure google-services.json is present in app/ and com.google.gms.google-services plugin is applied."
            )
            return null
        }
    }

    /**
     * Diagnostic wrapper for signInWithEmailAndPassword with detailed exception analysis.
     */
    fun diagnosticSignIn(
        auth: FirebaseAuth?,
        email: String,
        pass: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onError: (String, AuthDiagnosticLog) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        log(
            level = LogLevel.INFO,
            tag = "SignInAttempt",
            message = "Starting signInWithEmailAndPassword for: $cleanEmail",
            details = "Password length: ${cleanPass.length} chars"
        )

        if (auth == null) {
            val diagnostic = log(
                level = LogLevel.WARN,
                tag = "PlaceholderMode",
                message = "FirebaseAuth instance is null. Live Firebase sign in unavailable.",
                actionableResolution = "Proceeding with local credential authentication."
            )
            onError("FirebaseAuth is not initialized.", diagnostic)
            return
        }

        val app = FirebaseApp.getInstance()
        val apiKey = app.options.apiKey
        val isPlaceholder = apiKey.contains("Placeholder", ignoreCase = true) ||
                apiKey.length < 20 ||
                !apiKey.startsWith("AIza")

        if (isPlaceholder) {
            val diagnostic = log(
                level = LogLevel.INFO,
                tag = "PlaceholderMode",
                message = "Live Firebase sign in skipped (google-services.json using placeholder key).",
                actionableResolution = "Delegating to verified local credential registry."
            )
            onError("PlaceholderMode", diagnostic)
            return
        }

        if (cleanPass.length < 6) {
            val diagnostic = log(
                level = LogLevel.WARN,
                tag = "SignInValidation",
                message = "Password length is less than Firebase 6-character minimum (${cleanPass.length} chars).",
                actionableResolution = "Passwords must be at least 6 characters."
            )
            onError("Password must be at least 6 characters long.", diagnostic)
            return
        }

        try {
            auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        val diagnostic = log(
                            level = LogLevel.INFO,
                            tag = "SignInSuccess",
                            message = "Successfully signed in: ${user.email} (UID: ${user.uid})",
                            details = "Verified: ${user.isEmailVerified}, Providers: ${user.providerData.map { it.providerId }}"
                        )
                        onSuccess(user)
                    } else {
                        val diagnostic = log(
                            level = LogLevel.WARN,
                            tag = "SignInWarning",
                            message = "Sign in succeeded but returned null FirebaseUser."
                        )
                        onError("Sign in returned empty user.", diagnostic)
                    }
                }
                .addOnFailureListener { exception ->
                    val diagnostic = analyzeAuthException("signInWithEmailAndPassword", cleanEmail, exception)
                    onError(diagnostic.message, diagnostic)
                }
        } catch (e: Exception) {
            val diagnostic = log(
                level = LogLevel.ERROR,
                tag = "SignInException",
                message = "Synchronous exception during signInWithEmailAndPassword: ${e.message}",
                details = e.stackTraceToString()
            )
            onError(e.localizedMessage ?: "Unexpected error during sign in.", diagnostic)
        }
    }

    /**
     * Diagnostic wrapper for createUserWithEmailAndPassword with detailed exception analysis.
     */
    fun diagnosticCreateUser(
        auth: FirebaseAuth?,
        email: String,
        pass: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onError: (String, AuthDiagnosticLog) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        log(
            level = LogLevel.INFO,
            tag = "CreateUserAttempt",
            message = "Starting createUserWithEmailAndPassword for: $cleanEmail",
            details = "Password length: ${cleanPass.length} chars"
        )

        if (auth == null) {
            val diagnostic = log(
                level = LogLevel.WARN,
                tag = "PlaceholderMode",
                message = "FirebaseAuth instance is null. Live Firebase user creation unavailable.",
                actionableResolution = "Account will be stored in local persistent credential registry."
            )
            onError("FirebaseAuth is not initialized.", diagnostic)
            return
        }

        val app = FirebaseApp.getInstance()
        val apiKey = app.options.apiKey
        val isPlaceholder = apiKey.contains("Placeholder", ignoreCase = true) ||
                apiKey.length < 20 ||
                !apiKey.startsWith("AIza")

        if (isPlaceholder) {
            val diagnostic = log(
                level = LogLevel.INFO,
                tag = "PlaceholderMode",
                message = "Live Firebase user creation skipped (google-services.json using placeholder key).",
                actionableResolution = "Delegating to local persistent credential registry."
            )
            onError("PlaceholderMode", diagnostic)
            return
        }

        if (cleanPass.length < 6) {
            val diagnostic = log(
                level = LogLevel.WARN,
                tag = "CreateUserValidation",
                message = "Firebase requires at least 6 characters for passwords. Provided: ${cleanPass.length}",
                actionableResolution = "Enforce at least 6 characters in registration form."
            )
            onError("Password must be at least 6 characters long.", diagnostic)
            return
        }

        try {
            auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        val diagnostic = log(
                            level = LogLevel.INFO,
                            tag = "CreateUserSuccess",
                            message = "Account created in Firebase Console! Email: ${user.email}, UID: ${user.uid}",
                            details = "New account provisioned directly in Firebase Authentication backend."
                        )
                        onSuccess(user)
                    } else {
                        val diagnostic = log(
                            level = LogLevel.WARN,
                            tag = "CreateUserWarning",
                            message = "User creation completed but FirebaseUser is null."
                        )
                        onError("User creation returned empty user.", diagnostic)
                    }
                }
                .addOnFailureListener { exception ->
                    val diagnostic = analyzeAuthException("createUserWithEmailAndPassword", cleanEmail, exception)
                    onError(diagnostic.message, diagnostic)
                }
        } catch (e: Exception) {
            val diagnostic = log(
                level = LogLevel.ERROR,
                tag = "CreateUserException",
                message = "Synchronous exception during createUserWithEmailAndPassword: ${e.message}",
                details = e.stackTraceToString()
            )
            onError(e.localizedMessage ?: "Unexpected error during account creation.", diagnostic)
        }
    }

    /**
     * Analyzes and classifies Firebase Auth exceptions into explicit diagnostic messages
     * specifically catching "API Key not Valid" and "Internal Error" issues.
     */
    private fun analyzeAuthException(operation: String, email: String, exception: Exception): AuthDiagnosticLog {
        val rawMessage = exception.localizedMessage ?: exception.message ?: "Unknown error"
        val isApiKeyError = rawMessage.contains("API key", ignoreCase = true) ||
                rawMessage.contains("API_KEY", ignoreCase = true) ||
                (exception is FirebaseAuthException && exception.errorCode.contains("API_KEY", ignoreCase = true))

        val isInternalError = rawMessage.contains("internal error", ignoreCase = true) ||
                (exception is FirebaseAuthException && exception.errorCode.contains("INTERNAL_ERROR", ignoreCase = true))

        return when {
            isApiKeyError -> {
                log(
                    level = LogLevel.CRITICAL,
                    tag = "InvalidApiKey",
                    message = "Firebase rejected the request: 'API Key not valid'.",
                    details = "Operation: $operation | Exception: $rawMessage",
                    actionableResolution = "1. Download google-services.json from your Firebase Console (project 'banjulway').\n" +
                            "2. Replace /app/google-services.json with the downloaded file.\n" +
                            "3. Verify that the Google Cloud 'Identity Toolkit API' is enabled."
                )
            }
            isInternalError -> {
                log(
                    level = LogLevel.ERROR,
                    tag = "InternalError",
                    message = "Firebase Internal Error occurred during $operation.",
                    details = "Raw: $rawMessage",
                    actionableResolution = "Internal errors typically occur when the Google Cloud API key is restricted or when Google Play Services is outdated. Check Firebase Console Authentication settings."
                )
            }
            exception is FirebaseAuthWeakPasswordException -> {
                log(
                    level = LogLevel.WARN,
                    tag = "WeakPassword",
                    message = "Password rejected as too weak by Firebase.",
                    details = exception.reason ?: rawMessage,
                    actionableResolution = "Prompt user for a stronger password with at least 6 characters."
                )
            }
            exception is FirebaseAuthUserCollisionException -> {
                log(
                    level = LogLevel.WARN,
                    tag = "UserCollision",
                    message = "An account with email '$email' already exists in Firebase Authentication.",
                    details = rawMessage,
                    actionableResolution = "Prompt user to sign in instead of registering."
                )
            }
            exception is FirebaseAuthInvalidUserException -> {
                log(
                    level = LogLevel.INFO,
                    tag = "UserNotFound",
                    message = "No account found in Firebase matching '$email'.",
                    details = rawMessage,
                    actionableResolution = "Prompt user to create an account or verify email spelling."
                )
            }
            exception is FirebaseAuthInvalidCredentialsException -> {
                log(
                    level = LogLevel.WARN,
                    tag = "InvalidCredentials",
                    message = "Invalid credentials entered for '$email'.",
                    details = rawMessage,
                    actionableResolution = "Check password correctness."
                )
            }
            else -> {
                log(
                    level = LogLevel.ERROR,
                    tag = "AuthError",
                    message = "$operation failed: $rawMessage",
                    details = exception.stackTraceToString(),
                    actionableResolution = "Inspect network connectivity and Firebase project configuration."
                )
            }
        }
    }

    private fun updateReport(auth: FirebaseAuth) {
        val app = FirebaseApp.getInstance()
        val options = app.options
        val isPlaceholder = options.apiKey.contains("Placeholder", ignoreCase = true) ||
                options.apiKey.length < 20 ||
                !options.apiKey.startsWith("AIza")

        val user = auth.currentUser
        _latestReport.value = DiagnosticAuthReport(
            isFirebaseInitialized = isInitialized,
            projectId = options.projectId ?: "unknown",
            applicationId = options.applicationId,
            apiKeyMasked = if (options.apiKey.length > 8) "${options.apiKey.take(6)}...${options.apiKey.takeLast(4)}" else options.apiKey,
            isPlaceholderApiKey = isPlaceholder,
            currentUserId = user?.uid,
            currentUserEmail = user?.email,
            isEmailVerified = user?.isEmailVerified == true,
            statusSummary = if (isPlaceholder) {
                "⚠️ Using Placeholder API Key. Download official google-services.json to sync live Firebase Console users."
            } else {
                "✅ Firebase Configured with valid API Key. Connected to '${options.projectId}'."
            }
        )
    }

    private fun log(
        level: LogLevel,
        tag: String,
        message: String,
        details: String? = null,
        actionableResolution: String? = null
    ): AuthDiagnosticLog {
        val entry = AuthDiagnosticLog(
            tag = tag,
            level = level,
            message = message,
            details = details,
            actionableResolution = actionableResolution
        )

        when (level) {
            LogLevel.DEBUG -> Log.d("$TAG/$tag", message)
            LogLevel.INFO -> Log.i("$TAG/$tag", message)
            LogLevel.WARN -> Log.w("$TAG/$tag", "$message ${details ?: ""}")
            LogLevel.ERROR, LogLevel.CRITICAL -> Log.e("$TAG/$tag", "$message ${details ?: ""}")
        }

        val updated = _diagnosticLogs.value + entry
        // Retain latest 100 entries
        _diagnosticLogs.value = if (updated.size > 100) updated.takeLast(100) else updated
        return entry
    }

    fun clearLogs() {
        _diagnosticLogs.value = emptyList()
    }
}

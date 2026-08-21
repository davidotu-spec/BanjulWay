package com.example.utils

import android.util.Patterns

/**
 * Password strength levels for user feedback during registration.
 */
enum class PasswordStrength(val label: String, val score: Int) {
    TOO_SHORT("Too Short (< 6 chars)", 0),
    WEAK("Weak", 1),
    MEDIUM("Medium", 2),
    STRONG("Strong", 3)
}

/**
 * Result of password validation with granular policy checks.
 */
data class PasswordValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val strength: PasswordStrength = PasswordStrength.TOO_SHORT,
    val hasMinLength: Boolean = false,
    val hasLetter: Boolean = false,
    val hasDigitOrSymbol: Boolean = false
)

/**
 * General validation result for forms.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * AuthValidator provides a dedicated validation layer for checking email formats,
 * password complexity rules (meeting Firebase 6+ character requirement), and full registration payloads
 * before invoking Firebase Authentication operations.
 */
object AuthValidator {

    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    /**
     * Validates email format according to standard email RFC patterns.
     */
    fun validateEmail(email: String): ValidationResult {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return ValidationResult(false, "Email address cannot be empty.")
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return ValidationResult(false, "Please enter a complete email address (e.g. name@domain.com).")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() && !cleanEmail.matches(EMAIL_REGEX)) {
            return ValidationResult(false, "The email format is invalid. Please check for typos.")
        }
        return ValidationResult(true)
    }

    /**
     * Validates password requirements before sending to Firebase createUserWithEmailAndPassword.
     * Enforces Firebase's hard requirement of >= 6 characters and calculates strength.
     */
    fun validatePassword(password: String): PasswordValidationResult {
        val cleanPass = password.trim()

        if (cleanPass.isEmpty()) {
            return PasswordValidationResult(
                isValid = false,
                errorMessage = "Password cannot be empty.",
                strength = PasswordStrength.TOO_SHORT,
                hasMinLength = false
            )
        }

        val hasMinLength = cleanPass.length >= 6
        val hasLetter = cleanPass.any { it.isLetter() }
        val hasDigit = cleanPass.any { it.isDigit() }
        val hasSpecial = cleanPass.any { !it.isLetterOrDigit() }
        val hasDigitOrSymbol = hasDigit || hasSpecial

        if (!hasMinLength) {
            return PasswordValidationResult(
                isValid = false,
                errorMessage = "Password must be at least 6 characters (Firebase requirement).",
                strength = PasswordStrength.TOO_SHORT,
                hasMinLength = false,
                hasLetter = hasLetter,
                hasDigitOrSymbol = hasDigitOrSymbol
            )
        }

        // Calculate strength
        val score = when {
            hasMinLength && hasLetter && hasDigit && hasSpecial && cleanPass.length >= 8 -> PasswordStrength.STRONG
            hasMinLength && hasLetter && (hasDigit || hasSpecial) -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }

        return PasswordValidationResult(
            isValid = true,
            errorMessage = null,
            strength = score,
            hasMinLength = true,
            hasLetter = hasLetter,
            hasDigitOrSymbol = hasDigitOrSymbol
        )
    }

    /**
     * Complete sign-up validation checking email, password, name, and optional driver requirements.
     */
    fun validateSignUp(
        email: String,
        password: String,
        name: String? = null,
        isDriver: Boolean = false,
        vehiclePlate: String? = null
    ): ValidationResult {
        if (!name.isNullOrBlank() && name.trim().length < 2) {
            return ValidationResult(false, "Please enter your full name (at least 2 characters).")
        }

        val emailResult = validateEmail(email)
        if (!emailResult.isValid) {
            return emailResult
        }

        val passResult = validatePassword(password)
        if (!passResult.isValid) {
            return ValidationResult(false, passResult.errorMessage)
        }

        if (isDriver && vehiclePlate.isNullOrBlank()) {
            return ValidationResult(false, "Please enter your vehicle license plate.")
        }

        return ValidationResult(true)
    }
}

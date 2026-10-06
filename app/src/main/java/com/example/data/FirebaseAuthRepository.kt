package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class RegistrationResult(
    val verificationEmailSent: Boolean
)

class EmailNotVerifiedException(val email: String) : Exception()

object FirebaseAuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        termsAccepted: Boolean
    ): RegistrationResult {
        require(termsAccepted) { "Accept the terms before creating an account." }
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Firebase did not return the newly created user.")

        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(fullName.trim())
                .build()
        ).await()

        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("metadata")
                .document("account")
                .set(
                    mapOf(
                        "email" to (user.email ?: email.trim()),
                        "displayName" to fullName.trim(),
                        "termsAccepted" to true,
                        "termsAcceptedAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            runCatching { user.delete().await() }
            throw exception
        }

        val verificationEmailSent = try {
            user.sendEmailVerification().await()
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            false
        }

        return RegistrationResult(verificationEmailSent)
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Firebase did not return the signed-in user.")

        user.reload().await()
        if (!user.isEmailVerified) {
            throw EmailNotVerifiedException(user.email ?: email)
        }
        user.getIdToken(true).await()

        return user
    }

    suspend fun sendVerificationEmail() {
        val user = auth.currentUser ?: error("No signed-in account is available to verify.")
        if (!user.isEmailVerified) {
            user.sendEmailVerification().await()
        }
    }

    suspend fun isCurrentUserEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false
        user.reload().await()
        val refreshedUser = auth.currentUser ?: return false
        if (!refreshedUser.isEmailVerified) return false
        refreshedUser.getIdToken(true).await()
        return true
    }

    suspend fun refreshCurrentUser(): FirebaseUser? {
        val user = auth.currentUser ?: return null
        user.reload().await()
        val refreshedUser = auth.currentUser ?: return null
        if (refreshedUser.isEmailVerified) refreshedUser.getIdToken(true).await()
        return refreshedUser
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
        } catch (exception: FirebaseAuthInvalidUserException) {
            if (exception.errorCode != "ERROR_USER_NOT_FOUND") throw exception
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

fun authErrorMessage(error: Throwable): String {
    if (error is EmailNotVerifiedException) {
        return "Verify your email before signing in. You can resend the verification email."
    }

    return when ((error as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists for this email. Try signing in."
        "ERROR_WEAK_PASSWORD" -> "Choose a stronger password with at least 6 characters."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" ->
            "The email or password is incorrect."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a while and try again."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Check your internet connection and try again."
        else -> "We couldn't complete that request. Please try again."
    }
}

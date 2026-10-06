package com.example.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object AccountDeletionRepository {
    private val functions by lazy { FirebaseFunctions.getInstance("us-central1") }

    suspend fun sendVerificationCode(reason: String) {
        call("sendAccountDeletionOtp", mapOf("reason" to reason))
    }

    suspend fun deleteAccount(otp: String) {
        call("completeAccountDeletion", mapOf("otp" to otp))
    }

    private suspend fun call(name: String, data: Map<String, String>) {
        try {
            functions.getHttpsCallable(name).call(data).await()
        } catch (exception: CancellationException) {
            throw exception
        }
    }
}

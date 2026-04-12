package com.sebo.timelog.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthService private constructor(
    private val firebaseAuth: FirebaseAuth
) {

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser).isSuccess
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser).isSuccess
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

    suspend fun login(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    companion object {
        fun create(): AuthService? {
            return try {
                AuthService(FirebaseAuth.getInstance())
            } catch (_: IllegalStateException) {
                null
            }
        }
    }
}


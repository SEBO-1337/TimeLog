package com.sebo.timelog.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.sebo.timelog.data.model.UserRole
import com.sebo.timelog.data.model.toUserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthService private constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore?
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
        // Erstelle Benutzer-Dokument mit NEW Rolle
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (firestore != null) {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "email" to email,
                    "role" to UserRole.NEW.name,
                    "allowedProjectIds" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun getUserRole(): UserRole {
        val uid = firebaseAuth.currentUser?.uid ?: return UserRole.NEW
        if (firestore == null) return UserRole.NEW

        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val roleStr = doc.getString("role") ?: UserRole.NEW.name
            roleStr.toUserRole()
        } catch (e: Exception) {
            UserRole.NEW
        }
    }

    suspend fun getAllowedProjectIds(): List<String> {
        val uid = firebaseAuth.currentUser?.uid ?: return emptyList()
        if (firestore == null) return emptyList()

        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val raw = doc.get("allowedProjectIds") as? List<*> ?: return emptyList()
            raw.mapNotNull {
                when (it) {
                    is Long -> it.toString()
                    is Int -> it.toString()
                    is Double -> it.toLong().toString()
                    is String -> it
                    else -> null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun observeUserRole(): Flow<UserRole> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null || firestore == null) {
            trySend(UserRole.NEW)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(UserRole.NEW)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val roleStr = snapshot.getString("role") ?: UserRole.NEW.name
                    trySend(roleStr.toUserRole())
                } else {
                    trySend(UserRole.NEW)
                }
            }
        awaitClose { listener.remove() }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    companion object {
        fun create(): AuthService? {
            return try {
                val auth = FirebaseAuth.getInstance()
                val firestore = try {
                    FirebaseFirestore.getInstance()
                } catch (_: Exception) {
                    null
                }
                AuthService(auth, firestore)
            } catch (_: IllegalStateException) {
                null
            }
        }
    }
}


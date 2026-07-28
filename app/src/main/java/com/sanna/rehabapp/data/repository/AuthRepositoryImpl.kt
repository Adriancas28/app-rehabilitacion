package com.sanna.rehabapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.sanna.rehabapp.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override val uidActual: String?
        get() = firebaseAuth.currentUser?.uid

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}

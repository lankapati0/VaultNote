package com.lankeshwar.vaultnote.data.repository.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
){

    fun getCurrentUser(): FirebaseUser?{
        return firebaseAuth.currentUser
    }

    fun isUserLoggedIn(): Boolean{
        return firebaseAuth.currentUser != null
    }

    // register new account
    suspend fun register(email:String,password: String): FirebaseUser{
        val result = firebaseAuth.createUserWithEmailAndPassword(email,password)
            .await()
        return result.user ?: throw IllegalStateException("Account creation failed")
    }

    // Sign in with an existing account
    suspend fun login(email: String,password: String): FirebaseUser{
        val result = firebaseAuth.signInWithEmailAndPassword(email,password).await()

        return result.user ?: throw IllegalStateException("Login failed")
    }

    // Signout
    fun logout(){
        firebaseAuth.signOut()
    }

    // Send password reset email
    suspend fun sendPasswordResetEmail(email: String){
        firebaseAuth.sendPasswordResetEmail(email).await()
    }
}
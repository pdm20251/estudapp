package com.example.estudapp.ui.feature.auth

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {
    private val auth : FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState : LiveData<AuthState> = _authState

    var user : FirebaseUser? = auth.currentUser

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus(){
        if(auth.currentUser == null){
            _authState.value = AuthState.Unauthenticated
        } else{
            _authState.value = AuthState.Autheticated
        }
    }


    suspend fun getUserJwt(forceRefresh: Boolean = false): String? {
        val currentUser = auth.currentUser
        return try {
            withContext(Dispatchers.IO) {
                currentUser?.getIdToken(forceRefresh)?.await()?.token
            }
        } catch (e: Exception) {
            println("Erro ao obter o ID Token: ${e.message}")
            null
        }
    }
    fun login(email: String, password: String){

        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email e senha não podem estar vazios")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    user = auth.currentUser
                    _authState.value = AuthState.Autheticated
                } else{
                    _authState.value = AuthState.Error(task.exception?.message ?: "Algo deu errado")
                }
            }
    }

    fun signup(name: String, email: String, password: String){
        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email e senha não podem estar vazios")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    user = auth.currentUser
                    changeName(name)
                    _authState.value = AuthState.Autheticated
                } else{
                    _authState.value = AuthState.Error(task.exception?.message ?: "Algo deu errado")
                }
            }
    }

    fun signout(){
        auth.signOut()
        user = null
        _authState.value = AuthState.Unauthenticated
    }

    fun changeName(name: String){
        val profileUpdates = userProfileChangeRequest {
            displayName = name
        }

        user!!.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User profile updated.")
                }
            }
    }
}

sealed class AuthState{
    object Autheticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message : String) : AuthState()
}
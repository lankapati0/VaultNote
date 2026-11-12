package com.lankeshwar.vaultnote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.data.repository.firebase.FirebaseAuthRepository
import com.lankeshwar.vaultnote.viewmodels.firebase.FirestoreSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudSyncUiState(
    val isLoggedIn:Boolean = false,
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val firestoreSyncRepository: FirestoreSyncRepository
): ViewModel(){
    private val _uiState = MutableStateFlow(
        CloudSyncUiState(
            isLoggedIn = authRepository.isUserLoggedIn(),
            userEmail = authRepository.getCurrentUser()?.email
        )
    )

    val uiState: StateFlow<CloudSyncUiState> = _uiState.asStateFlow()

    fun login(email:String, password:String){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val user = authRepository.login(email,password)
                _uiState.value = CloudSyncUiState(
                    isLoggedIn = true,
                    userEmail = user.email,
                    successMessage = "Login successful"
                )
            } catch (e: Exception){
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Login Failed"
                )
            }
        }
    }

    fun register(email:String, password: String){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val user = authRepository.register(email,password)
                _uiState.value = CloudSyncUiState(
                    isLoggedIn = true, userEmail = user.email, successMessage = "Account created successfully")
            }catch (e: Exception){
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun sendPasswordResetEmail(email: String){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                authRepository.sendPasswordResetEmail(email)
                _uiState.value = _uiState.value.copy(
                    isLoading = false, successMessage = "Password reset email sent")
            }catch (e: Exception){
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = e.localizedMessage ?: "Unable to send reset email")
            }
        }
    }

    fun logout(){
        authRepository.logout()
        _uiState.value = CloudSyncUiState(
            isLoggedIn = false, successMessage = "Logged out successfully")
    }

    fun refreshAuthState(){
        val user = authRepository.getCurrentUser()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = user != null,
            userEmail = user?.email
        )
    }

    fun syncPublicNotes(){
        if(_uiState.value.isLoading == true) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, errorMessage = null, successMessage = null
            )

            try {
                val uploadedCount = firestoreSyncRepository.syncPublicNotes()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "$uploadedCount public notes uploaded successfully"
                )
            }catch (e: Exception){
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = e.localizedMessage ?: "Cloud sync failed")
            }

        }
    }

    fun testFirestoreWrite() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val result = firestoreSyncRepository.testFirestoreWrite()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Firestore write test failed"
                )
            }
        }
    }

    fun downloadPublicNotes(){
        if(_uiState.value.isLoading) true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val downloadedCount = firestoreSyncRepository.downloadPublicNotes()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage =  "$downloadedCount public notes downloaded successfully"
                )
            }catch (e: Exception){
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = e.localizedMessage ?: "Cloud download failed")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null, successMessage = null)
    }
}
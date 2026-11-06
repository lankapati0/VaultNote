package com.lankeshwar.vaultnote.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.Utils.BackupManager
import com.lankeshwar.vaultnote.Utils.PinManager
import com.lankeshwar.vaultnote.data.repository.backup.BackupPreview
import com.lankeshwar.vaultnote.data.repository.backup.BackupRestoreRepository
import com.lankeshwar.vaultnote.screens.private_note_screen.PrivateNoteSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val backupRestoreRepository: BackupRestoreRepository,
    private val privateNoteSession: PrivateNoteSession,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isCreatingBackup = MutableStateFlow(false)
    val isCreatingBackup: StateFlow<Boolean> = _isCreatingBackup.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> =
        _isAuthenticating.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    // Keeps private-note access active while the file picker is open
    // and while the backup is being created.
    private var backupSessionLease: AutoCloseable? = null
    private var pendingRestorePassword: CharArray? = null
    private var pendingRestoreFile:File? = null
    private var authenticationJob: Job? = null

    private val _isRestoringBackup = MutableStateFlow(false)
    val isRestoringBackup: StateFlow<Boolean> = _isRestoringBackup.asStateFlow()

    private val _isRestoringMessage = MutableStateFlow<String?>(null)
    val isRestoringMessage: StateFlow<String?> = _isRestoringMessage.asStateFlow()

    private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
    val backupPreview: StateFlow<BackupPreview?> = _backupPreview.asStateFlow()

    /**
     * Checks whether the user has configured a real PIN.
     */
    fun requiresPinAuthentication(): Boolean {
        return PinManager.hasPin(context)
    }

    /**
     * Verifies the real PIN before allowing a backup.
     *
     * The decoy PIN is deliberately not accepted.
     */
    fun authenticateForBackup(
        enteredPin: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (_isAuthenticating.value || _isCreatingBackup.value || _isRestoringBackup.value) {
            return
        }

        val remainingLockTime = PinManager.remainingLockTime(context)

        if (remainingLockTime > 0L) {
            onResult(false, "Too many attempts. Try again later.")
            return
        }

        authenticationJob = viewModelScope.launch {
            _isAuthenticating.value = true

            try {
                val result = withContext(Dispatchers.Default) {

                    val isRealPin =
                        PinManager.verifyPin(context, enteredPin)

                    val isDecoyPin =
                        if (!isRealPin) {
                            PinManager.verifyDecoyPin(context, enteredPin)
                        } else {
                            false
                        }

                    Pair(isRealPin, isDecoyPin)
                }

                when {
                    result.first -> {
                        PinManager.resetFailedAttempts(context)

                        // Release any previous preparation lease.
                        backupSessionLease?.let {
                            privateNoteSession.lock()
                            it.close()
                        }
                        backupSessionLease = null

                        // Unlock only after real PIN verification.
                        privateNoteSession.unlock()

                        backupSessionLease =
                            privateNoteSession.acquireBackupAccess()

                        if (backupSessionLease == null) {
                            privateNoteSession.lock()
                            onResult(
                                false,
                                "Unable to access private notes. Please try again."
                            )
                        } else {
                            onResult(true, null)
                        }
                    }

                    result.second -> {
                        // Decoy PIN must never authorize a backup.
                        PinManager.resetFailedAttempts(context)
                        privateNoteSession.lock()

                        onResult(false, "PIN verification failed.")
                    }

                    else -> {
                        val attempts =
                            PinManager.recordFailedAttempt(context)

                        val lockTime =
                            PinManager.remainingLockTime(context)

                        val message =
                            if (lockTime > 0L) {
                                "Too many attempts. Try again in 30 seconds."
                            } else {
                                "Incorrect PIN. Failed attempts: $attempts of 5."
                            }

                        onResult(false, message)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "VaultNoteBackup",
                    "Backup PIN authentication failed",
                    e
                )

                onResult(
                    false,
                    "PIN verification failed. Please try again."
                )
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    /**
     * Called when the user cancels the file picker.
     */
    fun cancelBackupPreparation() {
        privateNoteSession.lock()

        backupSessionLease?.close()
        backupSessionLease = null
    }

    fun cancelRestorePreparation(){
        pendingRestorePassword?.fill('\u0000')
        pendingRestorePassword = null

        pendingRestoreFile?.delete()
        pendingRestoreFile = null

        _backupPreview.value = null
        _isRestoringMessage.value = null

        // Lock only if this operation acquired a session lease.
        backupSessionLease?.let { lease ->
            privateNoteSession.lock()
            lease.close()
            backupSessionLease = null
        }
    }

    fun createBackup(destinationUri: Uri, password: CharArray) {

        if (_isCreatingBackup.value) {
            password.fill('\u0000')
            return
        }

        // A configured PIN requires successful authentication first.
        if (requiresPinAuthentication() && backupSessionLease == null) {
            password.fill('\u0000')
            _backupMessage.value =
                "Verify your real PIN before creating a backup."
            return
        }

        viewModelScope.launch {
            _isCreatingBackup.value = true
            _backupMessage.value = null

            var temporaryFile: File? = null

            var backupSize = 0L
            var copiedBytes = 0L

            try {
                withContext(Dispatchers.IO) {

                    temporaryFile = File(
                        context.cacheDir,
                        "VaultNote_Temp_${System.currentTimeMillis()}.vnb"
                    )

                    // Step 1: Generate the encrypted backup.
                    backupManager.createBackup(
                        destination = temporaryFile!!,
                        password = password
                    )

                    val backupFile = temporaryFile!!

                    check(backupFile.exists() && backupFile.isFile) {
                        "Backup file was not created."
                    }

                    val expectedSize = backupFile.length()
                    backupSize = expectedSize

                    check(expectedSize > 0L) {
                        "Backup generation produced an empty file."
                    }
                    // Step 2: Calculate hash of the original encrypted backup.
                    val originalHash = backupFile.inputStream().buffered().use { input ->
                        calculateSha256(input)
                    }

                    // Step 3: Copy to the selected location.
                    val outputStream =
                        context.contentResolver.openOutputStream(
                            destinationUri,
                            "wt"
                        ) ?: throw IllegalStateException(
                            "Unable to open the selected backup location."
                        )

                    copiedBytes = outputStream.use { output ->
                        backupFile.inputStream().buffered().use { input ->
                            input.copyTo(output)
                        }
                    }

                    check(copiedBytes == expectedSize) {
                        "Backup copy incomplete. Expected $expectedSize bytes, copied $copiedBytes bytes."
                    }

                    val savedHash = context.contentResolver.openInputStream(destinationUri)?.buffered()?.use {
                        calculateSha256(it)
                    } ?: throw IllegalStateException("Unable to read the saved backup for verification.")

                    check(originalHash.contentEquals(savedHash)) {
                        "Backup integrity verification failed. The saved file does not match the original."
                    }
                }
                android.util.Log.d(
                    "VaultNoteBackup",
                    "SUCCESS: Backup copied to destination. Bytes: $copiedBytes"
                )
                _backupMessage.value =
                    "Backup created successfully. Saved $copiedBytes bytes."
            } catch (e: Exception) {

                android.util.Log.e(
                    "VaultNoteBackup",
                    "Backup creation failed",
                    e
                )
                _backupMessage.value =
                    e.message ?: "Failed to create backup."
            } finally {

                temporaryFile?.delete()
                password.fill('\u0000')

                // Request locking, then release our lease.
                privateNoteSession.lock()

                backupSessionLease?.close()
                backupSessionLease = null

                _isCreatingBackup.value = false
            }
        }
    }

    fun previewRestoreBackup(
        backupFile: File,
        password: CharArray
    ) {
        if (_isRestoringBackup.value) {
            password.fill('\u0000')
            return
        }

        pendingRestorePassword?.fill('\u0000')
        pendingRestorePassword = null
        pendingRestoreFile = null

        val savedPassword = password.copyOf()

        viewModelScope.launch {
            _isRestoringMessage.value = null
            _backupPreview.value = null

            var previewSuccessful = false

            try {
                val preview = withContext(Dispatchers.IO) {
                    backupRestoreRepository.previewBackup(
                        backupFile = backupFile,
                        password = password
                    )
                }

                pendingRestorePassword = savedPassword
                pendingRestoreFile = backupFile

                _backupPreview.value = preview
                _isRestoringMessage.value = "Backup verified successfully."

                previewSuccessful = true

            } catch (e: Exception) {
                android.util.Log.e(
                    "VaultNoteBackup",
                    "Backup preview failed",
                    e
                )

                _isRestoringMessage.value =
                    e.message ?: "Unable to read this backup. Check the password and file."

            } finally {
                password.fill('\u0000')

                if (!previewSuccessful) {
                    savedPassword.fill('\u0000')
                }
            }
        }
    }

    fun restoreConfirmedBackup(){
        if(_isRestoringBackup.value) return

        val backupFile = pendingRestoreFile
        val restorePassword = pendingRestorePassword

        if(backupFile == null || restorePassword == null){
            _isRestoringMessage.value = "Please select and verify a backup first."
            return
        }
        // Require real PIN authentication when a PIN is configured.
        if (requiresPinAuthentication() && backupSessionLease == null) {
            _isRestoringMessage.value = "Verify your real PIN before restoring."
            return
        }

        viewModelScope.launch {
            _isRestoringBackup.value = true
            _isRestoringMessage.value = "Restoring your backup..."

            try {
                withContext(Dispatchers.IO){
                    backupRestoreRepository.restoreBackup(backupFile = backupFile, password = restorePassword)
                }
                _isRestoringMessage.value = "Backup restored successfully."

                _backupPreview.value = null
            }catch (e: Exception){
                android.util.Log.e(
                    "VaultNoteBackup",
                    "Backup restoration failed",
                    e
                )

                _isRestoringMessage.value =
                    e.message ?: "Failed to restore backup."
            } finally {
                restorePassword.fill('\u0000')
                backupFile.delete()
                pendingRestorePassword = null
                pendingRestoreFile = null

                // Lock private-note access and release the session lease.
                privateNoteSession.lock()
                backupSessionLease?.close()
                backupSessionLease = null

                _isRestoringBackup.value = false
            }
        }

    }

    fun clearMessage() {
        _backupMessage.value = null
    }

    fun clearRestoreMessage(message: String? = null){
        _isRestoringMessage.value = message
    }

    override fun onCleared() {
        privateNoteSession.lock()

        backupSessionLease?.close()
        backupSessionLease = null
        pendingRestorePassword?.fill('\u0000')
        pendingRestorePassword = null
        pendingRestoreFile?.delete()
        pendingRestoreFile = null
        super.onCleared()
    }

    private fun calculateSha256(input: InputStream): ByteArray{
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64*1024)

        while(true){
            val count = input.read(buffer)
            if(count == -1)break
            digest.update(buffer,0,count)
        }
        return digest.digest()
    }
}
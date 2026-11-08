package com.lankeshwar.vaultnote.viewmodels.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.Utils.backup_trash.BackupManager
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
import java.io.IOException
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
    private val _localBackups = MutableStateFlow<List<File>>(emptyList())
    val localBackups: StateFlow<List<File>> = _localBackups.asStateFlow()
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
                Log.e(
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
                Log.d(
                    "VaultNoteBackup",
                    "SUCCESS: Backup copied to destination. Bytes: $copiedBytes"
                )
                _backupMessage.value =
                    "Backup created successfully. Saved $copiedBytes bytes."
            } catch (e: Exception) {

                Log.e(
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
                Log.e(
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
                Log.e(
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

    fun createLocalBackup(password: CharArray){
        if(isCreatingBackup.value || isRestoringBackup.value){
            password.fill('\u0000')
            return
        }
        if(PinManager.hasPin(context)&& backupSessionLease == null){
            password.fill('\u0000')
            _backupMessage.value = "Please authenticate before creating a backup."
            return
        }

        viewModelScope.launch {
            _isCreatingBackup.value = true
            val backupDirectory = File(context.filesDir,"local_backups")
            val backupFile = File(backupDirectory,"VaultNote_Local_${System.currentTimeMillis()}.vnb")

            try {
                withContext(Dispatchers.IO){
                    if(!backupDirectory.exists() && !backupDirectory.mkdirs()){
                        throw IOException("Could not create backup directory.")
                    }

                    backupManager.createBackup(destination = backupFile,password=password)
                    if (!backupFile.exists() || backupFile.length() <= 0L) {
                        throw IOException("Backup file was not created correctly.")
                    }
                }
                _backupMessage.value = "Local backup created successfully."
            }catch (e: Exception){
                backupFile.delete()
                _backupMessage.value = "Local backup failed: ${e.message}"
            } finally {
                password.fill('\u0000')
                _isCreatingBackup.value = false
                privateNoteSession.lock()
                backupSessionLease?.close()
                backupSessionLease = null
            }

        }
    }

    fun loadLocalBackups(){
        viewModelScope.launch {
            val backups = withContext(Dispatchers.IO){
                val directory = File(context.filesDir,"local_backups")
                directory.listFiles()?.filter { it.isFile && it.extension == "vnb" }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
            _localBackups.value = backups
        }
    }

    fun deleteLocalBackups(backupFile:File){
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO){
                    val directory = File(context.filesDir,"local_backups").canonicalFile

                    val target = backupFile.canonicalFile
                    require(target.parentFile == directory) {
                        "Invalid backup file."
                    }

                    if (target.exists() && !target.delete()) {
                        throw IOException("Could not delete backup.")
                    }
                }

                loadLocalBackups()
                _backupMessage.value = "Backup deleted successfully."
            } catch (e: Exception){
                _backupMessage.value = "Could not delete backup: ${e.message}"
            }
        }
    }

    fun preserveLocalBackup(backupFile: File){
        renameLocaleBackup(backupFile = backupFile, preserve = true)
    }

    fun unpreserveLocalBackup(backupFile: File){
        renameLocaleBackup(backupFile = backupFile, preserve = false)
    }

    private fun renameLocaleBackup(backupFile: File, preserve:Boolean){
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO){
                    val backupDirectory = File(context.filesDir,"local_backups")
                    val validDirectory = backupDirectory.canonicalFile
                    val source = backupFile.canonicalFile

                    if(source.parentFile != validDirectory || !source.isFile || source.extension != "vnb"){
                        throw IOException("Invalid backup file.")
                    }

                    val currentName = source.name
                    val isAlreadyPreserved = currentName.startsWith("Preserved_")
                    if(preserve == isAlreadyPreserved){
                        return@withContext
                    }

                    val newName = if(preserve){
                        "Preserved_$currentName"
                    } else {
                        currentName.removePrefix("Preserved_")
                    }

                    val destination = File(validDirectory,newName)
                    if (destination.exists()) {
                        throw IOException("A backup with this name already exists.")
                    }

                    if (!source.renameTo(destination)) {
                        throw IOException("Could not update backup preservation.")
                    }
                }
                _backupMessage.value = if(preserve){
                    "Backup preserved successfully."
                } else {
                    "Backup preservation removed."
                }
                loadLocalBackups()
            }catch (e: Exception){
                _backupMessage.value = e.message ?: "Unable to update backup preservation."
            }
        }
    }
}
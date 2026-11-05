package com.lankeshwar.vaultnote.screens.private_note_screen

import com.lankeshwar.vaultnote.viewmodels.NotesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PrivateNoteSession @Inject constructor() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var activeBackupCount = 0
    private var lockRequested = false

    @Synchronized
    fun unlock() {
        lockRequested = false
        _isUnlocked.value = true
    }

    @Synchronized
    fun lock() {
        if (activeBackupCount > 0) {
            lockRequested = true
        } else {
            _isUnlocked.value = false
        }
    }

    @Synchronized
    fun acquireBackupAccess(): AutoCloseable? {
        if (!_isUnlocked.value) return null

        activeBackupCount++

        var released = false

        return AutoCloseable {
            synchronized(this) {
                if (!released) {
                    released = true
                    activeBackupCount--

                    if (activeBackupCount == 0 && lockRequested) {
                        lockRequested = false
                        _isUnlocked.value = false
                    }
                }
            }
        }
    }
}

package com.lankeshwar.vaultnote.screens.private_note_screen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivateNoteSession @Inject constructor(){

    private val _isUnlocked = MutableStateFlow(false)

    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()
    fun unlock(){
        _isUnlocked.value = true
    }

    fun lock(){
        _isUnlocked.value = false
    }
}
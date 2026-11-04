package com.lankeshwar.vaultnote.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.Utils.AppLockPreferences
import com.lankeshwar.vaultnote.Utils.BiometricHelper
import com.lankeshwar.vaultnote.Utils.BiometricPreferences
import com.lankeshwar.vaultnote.Utils.PinManager
import com.lankeshwar.vaultnote.Utils.PrivateNoteEncryptionManager
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.screens.private_note_screen.PrivateNoteSession
import com.lankeshwar.vaultnote.viewmodels.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PrivateNotesScreen(
    notesViewModel: NotesViewModel = hiltViewModel(),
    onNoteClick: (Note) -> Unit
){
    val privateNotes by notesViewModel.privateNotesFlow.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isVerifyingPin by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val activity = context as? FragmentActivity

    val isSessionUnlocked by notesViewModel.isPrivateNoteUnlockd.collectAsStateWithLifecycle()

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isUnlocked by remember {
        mutableStateOf(false)
    }

    var isDecoyUnlocked by remember {
        mutableStateOf(false)
    }
    var isSettingPin by remember {
        mutableStateOf(!PinManager.hasPin(context))
    }
    var pinError by remember { mutableStateOf("") }
    var lockRemainingMs by remember {
        mutableLongStateOf(PinManager.remainingLockTime(context))
    }

    var fingerprintEnabled by remember {
        mutableStateOf(BiometricPreferences.isFingerprintEnabled(context))
    }

    var appLockEnabled by remember {
        mutableStateOf(AppLockPreferences.isAppLockEnabled(context))
    }

    LaunchedEffect(lockRemainingMs) {
        while (lockRemainingMs > 0L){
            delay(1000L)
            lockRemainingMs = PinManager.remainingLockTime(context)
        }
    }

    DisposableEffect (Unit){
        val observer = LifecycleEventObserver {_, event ->
            if(event == Lifecycle.Event.ON_RESUME){
                fingerprintEnabled = BiometricPreferences.isFingerprintEnabled(context)

                appLockEnabled = AppLockPreferences.isAppLockEnabled(context)
            }

            if(event == Lifecycle.Event.ON_STOP){
                if(AppLockPreferences.isAppLockEnabled(context)){
                    notesViewModel.lockPrivateNotes()
                    isDecoyUnlocked = false
                    isUnlocked = false
                    pin = ""
                    confirmPin = ""

                }
            }

        }
        lifecycleOwner.lifecycle.addObserver (observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            isUnlocked = false
            isDecoyUnlocked = false
            notesViewModel.lockPrivateNotes()
            pin = ""
            confirmPin = ""
        }
    }

    if(!isUnlocked){
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = if (isSettingPin) "🔒 Create Your PIN" else "🔐 Unlock Private Notes"
            )

            OutlinedTextField(value = pin,
                onValueChange = {
                    if(it.length <= 6 && it.all (Char::isDigit)){pin = it
                    pinError = ""}
                },
                label = {Text("Enter 4-6 digit PIN")},
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                singleLine = true
                )

            if(isSettingPin){
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if(it.length <= 6 && it.all(Char:: isDigit)){
                            confirmPin = it
                        }
                    },
                    label = {Text("Confirm PIN")},
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }

            if (pinError.isNotBlank()){
                Text(text=pinError)
            }

            Button(
                onClick = {
                    when {
                        lockRemainingMs > 0L -> {
                            pinError = "Too many attempts. Try again later."
                        }

                        pin.length !in 4..6 -> {
                            pinError = "PIN must contain 4–6 digits"
                        }

                        isSettingPin && pin != confirmPin -> {
                            pinError = "PINs do not match"
                        }

                        isSettingPin -> {
                            PinManager.savePin(context, pin)
                            PinManager.resetFailedAttempts(context)
                            isSettingPin = false
                            isUnlocked = true
                            notesViewModel.unlockPrivateNotes()
                            pin = ""
                            confirmPin = ""
                        }

                        else -> {
                            val enteredPin = pin
                            pin = ""
                            pinError = ""
                            isVerifyingPin = true
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    val isRealPin = PinManager.verifyPin(context, enteredPin)

                                    val isDecoyPin = if (!isRealPin) {
                                        PinManager.verifyDecoyPin(context, enteredPin)
                                    } else {
                                        false
                                    }
                                    Pair(isRealPin, isDecoyPin)
                                }
                                isVerifyingPin = false

                                when {
                                    result.first -> {
                                        PinManager.resetFailedAttempts(context)
                                        isDecoyUnlocked = false
                                        isUnlocked = true
                                        notesViewModel.unlockPrivateNotes()
                                    }

                                    result.second -> {
                                        PinManager.resetFailedAttempts(context)
                                        notesViewModel.lockPrivateNotes()
                                        isDecoyUnlocked = true
                                        isUnlocked = true
                                    }

                                    else -> {
                                        val attempts = PinManager.recordFailedAttempt(context)
                                        lockRemainingMs = PinManager.remainingLockTime(context)

                                        pinError = if (lockRemainingMs > 0L) {
                                            "Too many attempts. Try again in 30 seconds"
                                        } else {
                                            "Incorrect PIN. Failed attempts: $attempts of 5"
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = !isVerifyingPin && (isSettingPin || lockRemainingMs <= 0L)
            ){
                Text(
                    when {
                        isVerifyingPin -> "Checking..."
                        isSettingPin -> "Save PIN"
                        else -> "Unlock"
                    }
                )
            }

            if(isVerifyingPin){
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Checking your PIN securely...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            if(!isSettingPin && fingerprintEnabled && activity != null && BiometricHelper.canAuthenticate(activity)){
                Button(
                    onClick = {
                        BiometricHelper.authenticate(activity,
                            onSucess = {
                                isDecoyUnlocked = false
                                isUnlocked = true
                                notesViewModel.unlockPrivateNotes()
                                pin = ""
                            },
                            onError = { error ->
                                pinError = error
                            })
                    }, modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Use Fingerprint")
                }
            }
        }
        return
    }

    if(isDecoyUnlocked){
        Column(modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(
                text = "🔒 Private Notes",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No private notes yet.")
        }
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .padding(16.dp)) {
/*        Text(text = "🔒 Private Notes", style = MaterialTheme.typography.headlineMedium)*/

        OutlinedTextField(value = title,
            onValueChange = {title = it},
            label = {Text("Note title")},
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true

            )

        OutlinedTextField(value = content,
            onValueChange = {content = it},
            label = {Text("Private note content")},
            modifier = Modifier.padding(top = 8.dp))

        Button(
            onClick = {
                if(title.isBlank() && content.isBlank()){
                    Toast.makeText(context,"Please enter a title or note content", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                notesViewModel.addNote(
                    title = title,
                    content = content,
                    isPrivate = true,
                    onComplete = {

                    }
                )
                title = ""
                content = ""

                Toast.makeText(context,"Private note saved",Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Save Private Note")
        }

        if(privateNotes.isEmpty()){
            Text(text="No private notes yet.", modifier = Modifier.padding(top = 20.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(privateNotes, key = {it.id}){note ->
                    NoteCard(
                        noteId = note.id,
                        title = note.title,
                        preview = note.preview,
                        formattedContent = note.formattedContent,
                        date = note.date,
                        onClick = { onNoteClick(note) },
                        onDelete = { notesViewModel.deleteNotes(note) },
                        onTogglePin = { notesViewModel.togglePin(note) },
                        onFavoritePin = { notesViewModel.toggleFavorite(note) },
                        isPinned = note.isPinned,
                        isFavorite = note.isFavorite
                    )
                }
            }
        }
    }
}
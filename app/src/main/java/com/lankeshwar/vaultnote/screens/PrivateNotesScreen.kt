package com.lankeshwar.vaultnote.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.Utils.BiometricHelper
import com.lankeshwar.vaultnote.Utils.PinManager
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.viewmodels.NotesViewModel

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun PrivateNotesScreen(
    notesViewModel: NotesViewModel = hiltViewModel(),
    onNoteClick: (Note) -> Unit
){
    val privateNotes by notesViewModel.privateNotesFlow.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current

    val activity = content as? FragmentActivity

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isUnlocked by remember {
        mutableStateOf(false)
    }
    var isSettingPin by remember {
        mutableStateOf(!PinManager.hasPin(context))
    }
    var pinError by remember { mutableStateOf("") }

    if(!isUnlocked){
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
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
                        pin.length !in 4..6 -> {
                            pinError = "PIN must contain 4–6 digits"
                        }

                        isSettingPin && pin != confirmPin -> {
                            pinError = "PINs do not match"
                        }

                        isSettingPin -> {
                            PinManager.savePin(context, pin)
                            isSettingPin = false
                            isUnlocked = true
                            pin = ""
                            confirmPin = ""
                        }

                        PinManager.verifyPin(context, pin) -> {
                            isUnlocked = true
                            pin = ""
                        }

                        else -> {
                            pinError = "Incorrect PIN"
                            pin = ""

                        }
                    }
                }
            ) {
                Text(if(isSettingPin) "Save PIN" else "Unlock")
            }

        }
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "🔒 Private Notes", style = MaterialTheme.typography.headlineMedium)

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
                    isPrivate = true
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
                      title = note.title,
                        preview = note.preview,
                        date = note.date,
                        onClick = {onNoteClick(note)},
                        onDelete = {notesViewModel.deleteNotes(note)}
                    )
                }
            }
        }

        if(!isSettingPin && activity != null && BiometricHelper.canAuthenticate(activity)){
            Button(
                onClick = {
                    BiometricHelper.authenticate(activity,
                        onSucess = {
                            isUnlocked = true
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
}
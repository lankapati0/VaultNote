package com.lankeshwar.vaultnote.data.storage

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceRecorder(context: Context) {

    private val appContext = context.applicationContext
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording():File {
        check(mediaRecorder == null){
            "A recording is already in progress."
        }

        val directory = File(appContext.filesDir, "attachments").apply {
            if(!exists() && !mkdirs()){
                throw IllegalStateException("Could not create the attachments directory.")
            }
        }

        val file = File(directory,"voice_${System.currentTimeMillis()}.m4a")

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            currentFile = file

            return file
        }catch (e: Exception){
            recorder.release()
            file.delete()
            throw e
        }
    }

    fun stopRecording(): File? {
        val recorder = mediaRecorder ?: return null
        val file = currentFile

        return try {
            recorder.stop()
            file
        }catch (e: RuntimeException){
            file?.delete()
            null
        } finally {
            recorder.release()
            mediaRecorder = null
            currentFile = null
        }
    }

    fun cancelRecording(){
        val recorder = mediaRecorder ?: return
        val file = currentFile

        try {
            recorder.stop()
        } catch (e: RuntimeException){
            Log.e("AudioException",e.message.toString())
        } finally {
            recorder.release()
            file?.delete()
            mediaRecorder = null
            currentFile = null
        }
    }

    fun release(){
        cancelRecording()
    }

}
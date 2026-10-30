package com.lankeshwar.vaultnote.Utils

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.lankeshwar.vaultnote.MainActivity
import com.lankeshwar.vaultnote.data.repository.NotesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: NotesRepository
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId",-1)
        val title = intent.getStringExtra("title") ?: "VaultNote reminder"
        val repeat = intent.getStringExtra("reminderRepeat") ?: "NONE"

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("noteId",noteId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        if(noteId == -1) return

        val channelId = "note_reminders"

        val contentPendingIntent = PendingIntent.getActivity(context,noteId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId,"Note reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }


        val notification = NotificationCompat.Builder(context,channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("VaultNote reminder")
            .setContentText(title)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(noteId,notification)

        if(repeat == "NONE"){
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.disableOneTimeReminder(noteId)
                } finally {
                    pendingResult.finish()
                }
            }
        }

        val nextTime = getNextReminderTime(repeat)

        if(nextTime != null){
            ReminderScheduler(context).scheduleReminder(
                noteId= noteId, title = title, reminderTime = nextTime, reminderRepeat = repeat
            )
        }
    }

    private fun getNextReminderTime(repeat: String):Long? {
        val calendar = Calendar.getInstance()
        when(repeat){
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR,1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR,1)
            "MONTHLY" -> calendar.add(Calendar.MONTH,1)
            else -> return null
        }
        return calendar.timeInMillis
    }
}
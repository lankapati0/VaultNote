package com.lankeshwar.vaultnote.Utils

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlin.jvm.java

class ReminderScheduler(private val context: Context){
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleReminder(
        noteId:Int,
        title: String,
        reminderTime: Long,
        reminderRepeat: String
    ){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId",noteId)
            putExtra("title",title)
            putExtra("reminderRepeat",reminderRepeat)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,noteId,intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()){
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, reminderTime,pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,reminderTime,pendingIntent
            )
        }
    }

    fun cancelReminder(noteId:Int){
        val intent = Intent(context,ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)

    }
}
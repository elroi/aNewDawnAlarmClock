package com.elroi.alarmpal.domain.manager

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarEvent(
    val title: String,
    val startTime: Long
)

@Singleton
class CalendarManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getUpcomingEvents(): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        if (!hasPermission()) return emptyList()

        // Fetch events for the ENTIRE current day (00:00 to 23:59)
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        queryEvents(startOfDay, endOfDay).forEach { events.add(it) }
        return events
    }
    
    fun getFirstEventOfNextDay(): CalendarEvent? {
        if (!hasPermission()) return null
        
        val now = Calendar.getInstance()
        val startOfNextDay = now.clone() as Calendar
        startOfNextDay.add(Calendar.DAY_OF_YEAR, 1)
        startOfNextDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfNextDay.set(Calendar.MINUTE, 0)
        
        val endOfNextDay = now.clone() as Calendar
        endOfNextDay.add(Calendar.DAY_OF_YEAR, 1)
        endOfNextDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfNextDay.set(Calendar.MINUTE, 59)
        
        val events = queryEvents(startOfNextDay.timeInMillis, endOfNextDay.timeInMillis)
        return events.firstOrNull()
    }

    private fun hasPermission(): Boolean {
         return androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun queryEvents(start: Long, end: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val contentResolver: ContentResolver = context.contentResolver
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, start)
        android.content.ContentUris.appendId(builder, end)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN
        )

        val cursor: Cursor? = contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            CalendarContract.Instances.BEGIN + " ASC"
        )

        cursor?.use {
            val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            while (it.moveToNext()) {
                val title = it.getString(titleIndex)
                val begin = it.getLong(beginIndex)
                events.add(CalendarEvent(title, begin))
            }
        }
        return events
    }
}

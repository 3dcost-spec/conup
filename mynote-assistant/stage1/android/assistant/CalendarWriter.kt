package com.cmx.mynote.assistant

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

/**
 * 승인된 일정을 폰 캘린더(CalendarContract)에 쓴다. 네이버(DAVx⁵)·구글 공통.
 * 같은 UID 가 이미 있으면 새로 만들지 않고 그 이벤트를 갱신한다(중복 방지).
 */
class CalendarWriter(private val context: Context) {

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** 반환: 폰 캘린더 Events._ID. 실패 시 예외 대신 null + 사유 */
    fun upsert(e: CalendarEvent, calendarId: Long): Pair<Long?, String?> {
        if (!hasPermission()) return null to "캘린더 권한 없음"
        val cr = context.contentResolver
        val v = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, e.title)
            put(CalendarContract.Events.EVENT_LOCATION, e.location)
            put(CalendarContract.Events.DESCRIPTION, buildString {
                append("마이노트 비서 등록")
                e.caseNo?.let { append(" · 사건 ").append(it) }
            })
            put(CalendarContract.Events.DTSTART, e.dtStart)
            put(CalendarContract.Events.DTEND, e.dtEnd)
            put(CalendarContract.Events.ALL_DAY, if (e.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, if (e.allDay) "UTC" else TimeZone.getDefault().id)
            put(CalendarContract.Events.UID_2445, e.uid)
        }
        val existing = findByUid(e.uid)
        val id = try {
            if (existing != null) { cr.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existing), v, null, null); existing }
            else cr.insert(CalendarContract.Events.CONTENT_URI, v)?.let { ContentUris.parseId(it) }
        } catch (t: Throwable) { return null to ("캘린더 쓰기 실패: " + t.javaClass.simpleName) }
        if (id != null && existing == null && e.alarmMinutes != null) runCatching {
            cr.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, id); put(CalendarContract.Reminders.MINUTES, e.alarmMinutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            })
        }
        return id to null
    }

    fun delete(providerEventId: Long): Boolean = runCatching {
        context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, providerEventId), null, null) == 1
    }.getOrDefault(false)

    private fun findByUid(uid: String): Long? =
        context.contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.UID_2445}=? AND ${CalendarContract.Events.DELETED}=0", arrayOf(uid), null
        )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    /** 폰 캘린더 기존 일정과 겹침 검사 (내용은 제목만 읽어 경고에 씀) */
    fun conflicts(from: Long, to: Long): List<String> {
        if (!hasPermission()) return emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().appendPath(from.toString()).appendPath(to.toString()).build()
        val out = mutableListOf<String>()
        context.contentResolver.query(uri, arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN), null, null, null)?.use {
            while (it.moveToNext()) out += (it.getString(0) ?: "(제목 없음)")
        }
        return out
    }
}

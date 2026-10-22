package com.cmx.mynote.assistant

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 비서 일정. 기존 cards/reports 테이블은 건드리지 않는 가산형 테이블.
 * status: proposed(비서 제안, 아직 캘린더에 없음) → confirmed(형이 승인) → synced(폰 캘린더에 기록됨)
 *         rejected(형이 거절) / cancelled(취소, 폰 캘린더에서 삭제)
 */
@Entity(tableName = "calendar_events", indices = [Index("status"), Index("dtStart"), Index(value = ["dedupKey"], unique = true)])
data class CalendarEvent(
    @PrimaryKey val uid: String,                 // "mynote-<uuid>" = iCal UID = CalendarContract UID_2445
    val dedupKey: String,                        // 날짜+시각+정규화 제목. 같은 일정 중복 제안 방지
    val title: String,
    val location: String? = null,
    val dtStart: Long,                           // epoch ms (Asia/Seoul 기준으로 만든 값)
    val dtEnd: Long,
    val allDay: Boolean = false,
    val alarmMinutes: Int? = 30,
    val caseNo: String? = null,
    val sourceCardIds: String = "",              // 근거 카드 ID, 쉼표 구분
    val sourceReportKey: String? = null,         // 예: "daily:2026-10-22"
    val status: String = Status.PROPOSED,
    val targetCalendarId: Long? = null,          // CalendarContract.Calendars._ID (네이버 DAVx⁵ 등)
    val providerEventId: Long? = null,           // 폰 캘린더에 쓴 Events._ID
    val ambiguity: String = "",                  // 해석기가 남긴 '되물을 이유'
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    object Status {
        const val PROPOSED = "proposed"; const val CONFIRMED = "confirmed"; const val SYNCED = "synced"
        const val REJECTED = "rejected"; const val CANCELLED = "cancelled"
    }
}

@Dao
interface CalendarEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(e: CalendarEvent): Long
    @Update suspend fun update(e: CalendarEvent)
    @Query("SELECT * FROM calendar_events WHERE uid = :uid") suspend fun get(uid: String): CalendarEvent?
    @Query("SELECT * FROM calendar_events WHERE status = 'proposed' ORDER BY dtStart") fun observeProposals(): Flow<List<CalendarEvent>>
    @Query("SELECT * FROM calendar_events WHERE status = 'confirmed' ORDER BY dtStart") suspend fun pendingWrites(): List<CalendarEvent>
    @Query("SELECT * FROM calendar_events WHERE status IN ('confirmed','synced') AND dtStart < :to AND dtEnd > :from ORDER BY dtStart")
    suspend fun overlapping(from: Long, to: Long): List<CalendarEvent>
}

/**
 * Room 마이그레이션: 현재 DB 버전 N → N+1 에 그대로 붙인다. (코덱스: N 은 code19 AppDatabase 의 version 값)
 * 기존 테이블 변경 없음. 실패해도 기존 데이터는 그대로다.
 */
const val CREATE_CALENDAR_EVENTS_SQL = """
CREATE TABLE IF NOT EXISTS `calendar_events` (
  `uid` TEXT NOT NULL, `dedupKey` TEXT NOT NULL, `title` TEXT NOT NULL, `location` TEXT,
  `dtStart` INTEGER NOT NULL, `dtEnd` INTEGER NOT NULL, `allDay` INTEGER NOT NULL, `alarmMinutes` INTEGER,
  `caseNo` TEXT, `sourceCardIds` TEXT NOT NULL, `sourceReportKey` TEXT, `status` TEXT NOT NULL,
  `targetCalendarId` INTEGER, `providerEventId` INTEGER, `ambiguity` TEXT NOT NULL,
  `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`uid`))
"""
val CALENDAR_INDEX_SQL = listOf(
    "CREATE INDEX IF NOT EXISTS `index_calendar_events_status` ON `calendar_events` (`status`)",
    "CREATE INDEX IF NOT EXISTS `index_calendar_events_dtStart` ON `calendar_events` (`dtStart`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_calendar_events_dedupKey` ON `calendar_events` (`dedupKey`)",
)

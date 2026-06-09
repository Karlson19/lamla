package app.lamla.data.local.dao

import androidx.room.*
import app.lamla.data.local.entities.AttendanceRecordEntity
import app.lamla.data.local.entities.VenueLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AttendanceRecordEntity): Long

    @Delete
    suspend fun delete(record: AttendanceRecordEntity)

    @Query("SELECT * FROM attendance_records ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE courseId = :courseId ORDER BY dateEpochDay DESC")
    fun observeForCourse(courseId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE classSessionId = :sessionId AND dateEpochDay = :day LIMIT 1")
    suspend fun forOccurrence(sessionId: Long, day: Long): AttendanceRecordEntity?

    @Query("DELETE FROM attendance_records WHERE classSessionId = :sessionId AND dateEpochDay = :day")
    suspend fun clearOccurrence(sessionId: Long, day: Long)

    @Query("SELECT * FROM attendance_records")
    suspend fun all(): List<AttendanceRecordEntity>
}

@Dao
interface VenueLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(venue: VenueLocationEntity): Long

    @Delete
    suspend fun delete(venue: VenueLocationEntity)

    @Query("SELECT * FROM venue_locations ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<VenueLocationEntity>>

    @Query("SELECT * FROM venue_locations WHERE venueKey = :key LIMIT 1")
    suspend fun getByKey(key: String): VenueLocationEntity?

    @Query("DELETE FROM venue_locations WHERE venueKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT * FROM venue_locations")
    suspend fun all(): List<VenueLocationEntity>
}

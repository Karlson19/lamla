package app.lamla.data.local.dao

import androidx.room.*
import app.lamla.data.local.entities.ClassSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

@Dao
interface ClassSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ClassSessionEntity): Long

    @Update
    suspend fun update(session: ClassSessionEntity)

    @Delete
    suspend fun delete(session: ClassSessionEntity)

    @Query("SELECT * FROM class_sessions WHERE id = :id")
    suspend fun get(id: Long): ClassSessionEntity?

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek, startMinutes")
    fun observeAll(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :day ORDER BY startMinutes")
    fun observeForDay(day: DayOfWeek): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE courseId = :courseId ORDER BY dayOfWeek, startMinutes")
    fun observeForCourse(courseId: Long): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions")
    suspend fun all(): List<ClassSessionEntity>
}

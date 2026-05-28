package app.lamla.data.local.dao

import androidx.room.*
import app.lamla.data.local.entities.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: CourseEntity): Long

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY code COLLATE NOCASE")
    fun observeForSemester(semesterId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY code COLLATE NOCASE")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    fun observe(id: Long): Flow<CourseEntity?>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun get(id: Long): CourseEntity?

    @Query("SELECT * FROM courses WHERE lecturerId = :lecturerId")
    fun observeForLecturer(lecturerId: Long): Flow<List<CourseEntity>>
}

package com.example.fatecskip.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fatecskip.data.local.entities.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY dayOfWeek ASC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE semester = :semester ORDER BY dayOfWeek ASC, name ASC")
    fun getSubjectsBySemester(semester: Int): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE semester = :semester ORDER BY dayOfWeek ASC, name ASC")
    suspend fun getSubjectsBySemesterList(semester: Int): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE semester = :semester AND dayOfWeek = :dayOfWeek")
    suspend fun getSubjectsBySemesterAndDay(semester: Int, dayOfWeek: Int): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET currentAbsences = :absences WHERE id = :id")
    suspend fun updateAbsences(id: Long, absences: Int)

    @Query("SELECT * FROM subjects WHERE isCustomized = 1 ORDER BY semester ASC, name ASC")
    fun getCustomizedSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isCustomized = 1")
    suspend fun getCustomizedSubjectsList(): List<SubjectEntity>

    @Query("DELETE FROM subjects WHERE semester = :semester AND isCustomized = 0")
    suspend fun clearSubjectsBySemester(semester: Int)

    @Query("DELETE FROM subjects WHERE isCustomized = 0")
    suspend fun clearAllNonCustomizedSubjects()

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubjectById(subjectId: Long)
}


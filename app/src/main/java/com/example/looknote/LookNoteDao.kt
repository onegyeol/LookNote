package com.example.looknote

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LookNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: LookNoteEntity)

    @Query("SELECT * FROM look_note ORDER BY id DESC")
    suspend fun getAll(): List<LookNoteEntity>

    @Query("DELETE FROM look_note")
    suspend fun deleteAll()

    // 날짜를 기준으로 해당 룩 노트가 있는지 없는지 조회
    @Query("SELECT * FROM look_note WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): LookNoteEntity?

    @Query("DELETE FROM look_note WHERE date = :date")
    suspend fun deleteByDate(date: String)

    // 전체 데이터를 Flow 형태로 실시간 관찰 (LiveData와 비슷하게 동작)
    @Query("SELECT * FROM look_note ORDER BY id DESC")
    fun getAllFlow(): Flow<List<LookNoteEntity>>

    // 수정 시 업데이트 위함
    @Update
    suspend fun updateNote(note: LookNoteEntity)
}

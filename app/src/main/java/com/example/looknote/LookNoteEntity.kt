package com.example.looknote
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "look_note")
data class LookNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val imageUri: String? = null,
    val top: String = "",
    val bottom: String = "",
    val shoes: String = "",
    val etc: String = "",
    val memo: String = ""
)

package com.example.looknote

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LookNoteEntity::class], version = 1)
abstract class LookNoteDB : RoomDatabase() {
    abstract fun lookNoteDao(): LookNoteDao

    companion object {
        @Volatile private var INSTANCE: LookNoteDB? = null

        fun getInstance(context: Context): LookNoteDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LookNoteDB::class.java,
                    "look_note_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.FileTransferDao
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ContactEntity
import com.example.data.local.entities.FileTransferEntity

@Database(
    entities = [
        ContactEntity::class,
        ChatMessageEntity::class,
        FileTransferEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(PeerTypeConverters::class)
abstract class PeerDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun fileTransferDao(): FileTransferDao

    companion object {
        @Volatile
        private var INSTANCE: PeerDatabase? = null

        fun getInstance(context: Context): PeerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PeerDatabase::class.java,
                    "peerflow_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

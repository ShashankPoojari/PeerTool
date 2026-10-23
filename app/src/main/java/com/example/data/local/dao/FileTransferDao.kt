package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.FileTransferEntity
import com.example.data.model.TransferStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FileTransferDao {
    @Query("SELECT * FROM file_transfers ORDER BY timestamp DESC")
    fun getAllTransfersFlow(): Flow<List<FileTransferEntity>>

    @Query("SELECT * FROM file_transfers WHERE peerId = :peerId ORDER BY timestamp DESC")
    fun getTransfersForPeerFlow(peerId: String): Flow<List<FileTransferEntity>>

    @Query("SELECT * FROM file_transfers WHERE id = :transferId LIMIT 1")
    suspend fun getTransfer(transferId: String): FileTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: FileTransferEntity)

    @Update
    suspend fun updateTransfer(transfer: FileTransferEntity)

    @Query("UPDATE file_transfers SET status = :status WHERE id = :transferId")
    suspend fun updateStatus(transferId: String, status: TransferStatus)

    @Query("UPDATE file_transfers SET localFilePath = :path, status = :status WHERE id = :transferId")
    suspend fun updateFilePathAndStatus(transferId: String, path: String, status: TransferStatus)
}

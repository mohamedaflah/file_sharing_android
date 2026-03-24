package com.sharefast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sharefast.data.local.entity.TransferRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferRecordDao {
    @Query("SELECT * FROM transfer_records ORDER BY timestampEpochMs DESC LIMIT 200")
    fun observeAll(): Flow<List<TransferRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransferRecordEntity): Long
}

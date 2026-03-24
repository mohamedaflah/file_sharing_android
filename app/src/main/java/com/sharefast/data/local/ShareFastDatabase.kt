package com.sharefast.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.sharefast.data.local.entity.TransferRecordEntity
import com.sharefast.domain.model.TransferDirection

object Converters {
    @JvmStatic
    @TypeConverter
    fun fromDirection(d: TransferDirection): String = d.name

    @JvmStatic
    @TypeConverter
    fun toDirection(s: String): TransferDirection = TransferDirection.valueOf(s)
}

@Database(
    entities = [TransferRecordEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ShareFastDatabase : RoomDatabase() {
    abstract fun transferRecordDao(): TransferRecordDao
}

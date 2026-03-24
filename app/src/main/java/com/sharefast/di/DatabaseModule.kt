package com.sharefast.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sharefast.data.local.ChatMessageDao
import com.sharefast.data.local.ShareFastDatabase
import com.sharefast.data.local.TransferRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transfer_records ADD COLUMN storageUri TEXT")
        }
    }
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`peerKey` TEXT NOT NULL, " +
                    "`peerName` TEXT NOT NULL, " +
                    "`body` TEXT NOT NULL, " +
                    "`isOutgoing` INTEGER NOT NULL, " +
                    "`timestampEpochMs` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_peerKey` ON `chat_messages` (`peerKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_timestampEpochMs` ON `chat_messages` (`timestampEpochMs`)")
        }
    }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ShareFastDatabase =
        Room.databaseBuilder(context, ShareFastDatabase::class.java, "sharefast.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun transferDao(db: ShareFastDatabase): TransferRecordDao = db.transferRecordDao()

    @Provides
    fun chatDao(db: ShareFastDatabase): ChatMessageDao = db.chatMessageDao()
}

package com.firebase.loginauth.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.firebase.loginauth.database.dao.*
import com.firebase.loginauth.database.entities.*

@Database(
    entities = [
        StockEntity::class,
        OrderEntity::class,
        DeliveryEntity::class,
        SellEntity::class,
        DueAccountEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun stockDao(): StockDao
    abstract fun orderDao(): OrderDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun sellDao(): SellDao
    abstract fun dueAccountDao(): DueAccountDao
    abstract fun syncQueueDao(): SyncQueueDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mah_shagor_enterprise_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

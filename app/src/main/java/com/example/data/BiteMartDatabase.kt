package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AddressDao
import com.example.data.dao.CartDao
import com.example.data.dao.FavoriteDao
import com.example.data.dao.OrderDao
import com.example.data.dao.UserDao
import com.example.data.entity.AddressEntity
import com.example.data.entity.CartEntity
import com.example.data.entity.FavoriteEntity
import com.example.data.entity.OrderEntity
import com.example.data.entity.OtpRecordEntity
import com.example.data.entity.UserEntity

@Database(
    entities = [
        CartEntity::class,
        OrderEntity::class,
        AddressEntity::class,
        FavoriteEntity::class,
        UserEntity::class,
        OtpRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BiteMartDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun addressDao(): AddressDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: BiteMartDatabase? = null

        fun getInstance(context: Context): BiteMartDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BiteMartDatabase::class.java,
                    "bitemart_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

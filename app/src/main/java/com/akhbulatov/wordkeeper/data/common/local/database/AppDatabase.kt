package com.akhbulatov.wordkeeper.data.common.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.akhbulatov.wordkeeper.data.common.local.database.category.CategoryDao
import com.akhbulatov.wordkeeper.data.common.local.database.category.CategoryDbModel
import com.akhbulatov.wordkeeper.data.common.local.database.word.WordDao
import com.akhbulatov.wordkeeper.data.common.local.database.word.WordDbModel

@Database(
    entities = [WordDbModel::class, CategoryDbModel::class],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun categoryDao(): CategoryDao

    companion object {}
}
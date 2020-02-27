package com.akhbulatov.wordkeeper.data.common.local.database.word

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WordDbModel(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "category") val category: String
)
package com.akhbulatov.wordkeeper.domain.common.models

import android.database.Cursor
import com.akhbulatov.wordkeeper.database.DatabaseContract.CategoryEntry
import java.util.ArrayList

data class Category(
    val id: Long,
    val name: String
) {

    companion object {
        @JvmStatic
        fun getCategories(cursor: Cursor): List<Category> {
            val categories: MutableList<Category> =
                ArrayList(cursor.count)
            while (!cursor.isAfterLast) {
                val id = cursor.getLong(cursor.getColumnIndex(CategoryEntry._ID))
                val name = cursor.getString(cursor.getColumnIndex(CategoryEntry.COLUMN_NAME))
                val category = Category(id, name)
                categories.add(category)
                cursor.moveToNext()
            }
            return categories
        }
    }
}
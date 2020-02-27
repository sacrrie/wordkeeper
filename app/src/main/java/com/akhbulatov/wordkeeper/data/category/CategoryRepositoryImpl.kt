package com.akhbulatov.wordkeeper.data.category

import com.akhbulatov.wordkeeper.database.dao.CategoryDao
import com.akhbulatov.wordkeeper.domain.common.repositories.CategoryRepository
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
}
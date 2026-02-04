package com.hazron.sequencetimer.data.repository

import com.hazron.sequencetimer.data.local.CategoryDao
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getAllCategoriesSync(): List<Category> = categoryDao.getAllCategoriesSync()

    suspend fun getCategory(id: Long): Category? = categoryDao.getCategory(id)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(id: Long): Boolean {
        // Cannot delete default categories
        val category = categoryDao.getCategory(id)
        if (category?.isDefault == true) return false

        return categoryDao.deleteCategoryById(id) > 0
    }

    suspend fun ensureDefaultCategories() {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            categoryDao.insertCategories(DefaultCategories.categories)
        }
    }

    suspend fun createCategory(name: String, icon: String? = null, color: Long? = null): Long {
        val maxOrder = categoryDao.getMaxSortOrder() ?: -1
        val category = Category(
            name = name,
            icon = icon,
            color = color,
            sortOrder = maxOrder + 1,
            isDefault = false
        )
        return categoryDao.insertCategory(category)
    }

    suspend fun reorderCategories(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            categoryDao.updateSortOrder(id, index)
        }
    }
}

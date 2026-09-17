package com.souspantry.app.ui.plancook

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.souspantry.app.data.models.SuggestedMeal
import java.util.UUID

/**
 * One past Discover generation — the batch of recipes suggested at a point in
 * time, shown in the History sheet. Mirrors iOS MealHistorySession.
 */
@Entity(tableName = "meal_history_sessions")
data class MealHistorySession(
    @PrimaryKey val id : String       = UUID.randomUUID().toString(),
    val createdAt       : Long        = System.currentTimeMillis(),
    val cuisines         : List<String> = emptyList(),
    val meals            : List<SuggestedMeal> = emptyList(),
)

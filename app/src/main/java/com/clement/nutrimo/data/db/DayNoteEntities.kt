// "Ma journée": the mood of a day (1 to 5) and a few lines of journal (gratitude, victory of the day, free note).
@file:UseSerializers(LocalDateSerializer::class)

package com.clement.nutrimo.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

const val MOOD_MIN = 1
const val MOOD_MAX = 5

/** One per day; every field optional, a row with nothing left in it is deleted. */
@Serializable
@Entity(tableName = "day_notes")
data class DayNoteEntity(
    @PrimaryKey val date: LocalDate,
    /** [MOOD_MIN] (hard day) to [MOOD_MAX] (great day). */
    val mood: Int? = null,
    val gratitude: String = "",
    val win: String = "",
    val note: String = "",
    val updatedAt: Long = now(),
) {
    val isEmpty: Boolean get() = mood == null && gratitude.isBlank() && win.isBlank() && note.isBlank()
}

@Dao
interface DayNoteDao {
    @Query("SELECT * FROM day_notes WHERE date = :date")
    fun observe(date: LocalDate): Flow<DayNoteEntity?>

    @Query("SELECT * FROM day_notes WHERE date = :date")
    suspend fun get(date: LocalDate): DayNoteEntity?

    @Query("SELECT * FROM day_notes")
    suspend fun all(): List<DayNoteEntity>

    @Upsert
    suspend fun upsert(note: DayNoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<DayNoteEntity>)

    @Query("DELETE FROM day_notes WHERE date = :date")
    suspend fun delete(date: LocalDate)
}

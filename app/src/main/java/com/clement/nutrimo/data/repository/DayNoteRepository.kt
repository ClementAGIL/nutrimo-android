// "Ma journée" of the journal: mood and short journal of each day.
package com.clement.nutrimo.data.repository

import com.clement.nutrimo.data.db.DayNoteEntity
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.now
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DayNoteRepository(db: NutrimoDatabase) {
    private val dao = db.dayNoteDao()

    fun day(date: LocalDate): Flow<DayNoteEntity?> = dao.observe(date)

    suspend fun on(date: LocalDate): DayNoteEntity? = dao.get(date)

    /** Saves [note], trimmed; an empty one is deleted rather than kept as a blank row. */
    suspend fun save(note: DayNoteEntity) {
        val clean = note.copy(gratitude = note.gratitude.trim(), win = note.win.trim(), note = note.note.trim(), updatedAt = now())
        if (clean.isEmpty) dao.delete(note.date) else dao.upsert(clean)
    }
}

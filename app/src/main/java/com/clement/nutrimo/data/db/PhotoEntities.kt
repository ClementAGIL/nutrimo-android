// Progress photos: one row per photo (the image itself is a private file, see PhotoRepository) and its DAO.
@file:UseSerializers(LocalDateSerializer::class)

package com.clement.nutrimo.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.clement.nutrimo.domain.model.PhotoPose
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

/**
 * One photo per day and pose: a new one replaces it. Linked to the day's weigh-in and measurements by [date].
 * Not part of the JSON backup (only described in the ZIP export, next to the image files).
 */
@Serializable
@Entity(tableName = "photos", indices = [Index(value = ["date", "pose"], unique = true)])
data class PhotoEntity(
    @PrimaryKey val id: String = newId(),
    val date: LocalDate,
    val pose: PhotoPose,
    /** The image is stored upright and not mirrored, whichever camera took it. */
    val frontCamera: Boolean,
    val note: String? = null,
    val updatedAt: Long = now(),
)

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY date, pose")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY date, pose")
    suspend fun all(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun byId(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE date = :date AND pose = :pose")
    suspend fun byDayAndPose(date: LocalDate, pose: PhotoPose): PhotoEntity?

    @Query("SELECT * FROM photos WHERE pose = :pose ORDER BY date DESC LIMIT 1")
    suspend fun latest(pose: PhotoPose): PhotoEntity?

    @Query("SELECT MAX(date) FROM photos")
    suspend fun latestDate(): LocalDate?

    @Upsert
    suspend fun upsert(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM photos")
    suspend fun deleteAll()
}

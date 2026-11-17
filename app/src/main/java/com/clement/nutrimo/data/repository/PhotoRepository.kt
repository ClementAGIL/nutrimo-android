// Progress photos: resized JPEGs + thumbnails in the app's private folder (never in the gallery), rows in Room.
package com.clement.nutrimo.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.data.db.now
import com.clement.nutrimo.domain.model.PhotoPose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

private const val PHOTO_DIR = "photos"
private const val THUMB_DIR = "thumbs"
/** Long side of a stored photo: sharp when zoomed, ~0.5–1 MB. */
private const val PHOTO_SIDE = 2500
private const val PHOTO_QUALITY = 90
private const val THUMB_SIDE = 400
private const val THUMB_QUALITY = 80

data class PhotoStorage(val count: Int, val bytes: Long)

class PhotoRepository(context: Context, db: NutrimoDatabase) {
    private val dao = db.photoDao()
    private val dir = File(context.filesDir, PHOTO_DIR)
    private val thumbs = File(dir, THUMB_DIR)

    /** Oldest first. */
    val photos: Flow<List<PhotoEntity>> = dao.observeAll()

    fun file(photo: PhotoEntity) = File(dir, "${photo.id}.jpg")

    fun thumbFile(photo: PhotoEntity) = File(thumbs, "${photo.id}.jpg")

    suspend fun all(): List<PhotoEntity> = dao.all()

    suspend fun byId(id: String): PhotoEntity? = dao.byId(id)

    /** The last photo of [pose], shown as a ghost in the camera to line up the next one. */
    suspend fun latest(pose: PhotoPose): PhotoEntity? = dao.latest(pose)

    suspend fun latestDate(): LocalDate? = dao.latestDate()

    /** Stores [upright] (already rotated, not mirrored) as the [pose] photo of [date], replacing that day's one. */
    suspend fun save(date: LocalDate, pose: PhotoPose, frontCamera: Boolean, note: String?, upright: Bitmap): PhotoEntity =
        withContext(Dispatchers.IO) {
            dao.byDayAndPose(date, pose)?.let { delete(it) }
            val photo = PhotoEntity(date = date, pose = pose, frontCamera = frontCamera, note = note?.trim()?.ifEmpty { null })
            thumbs.mkdirs()
            writeJpeg(upright.scaledTo(PHOTO_SIDE), file(photo), PHOTO_QUALITY)
            writeJpeg(upright.scaledTo(THUMB_SIDE), thumbFile(photo), THUMB_QUALITY)
            dao.upsert(photo)
            photo
        }

    suspend fun setNote(photo: PhotoEntity, note: String?) =
        dao.upsert(photo.copy(note = note?.trim()?.ifEmpty { null }, updatedAt = now()))

    suspend fun delete(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        file(photo).delete()
        thumbFile(photo).delete()
        dao.delete(photo.id)
    }

    /** Rows and files; also used after the database was wiped (files would otherwise stay orphaned). */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dir.deleteRecursively()
        dao.deleteAll()
    }

    suspend fun storage(): PhotoStorage = withContext(Dispatchers.IO) {
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        PhotoStorage(dao.all().size, files.sumOf { it.length() })
    }

    /** Decodes [file] with its long side close to [maxSide] (power-of-two subsampling, then exact scaling). */
    suspend fun load(file: File, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.scaledTo(maxSide)
    }
}

private fun Bitmap.scaledTo(maxSide: Int): Bitmap {
    val longest = max(width, height)
    if (longest <= maxSide) return this
    val ratio = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(this, (width * ratio).roundToInt(), (height * ratio).roundToInt(), true)
}

private fun writeJpeg(bitmap: Bitmap, file: File, quality: Int) {
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
}

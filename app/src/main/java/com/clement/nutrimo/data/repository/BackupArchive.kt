// File export as a ZIP: the JSON backup + every progress photo (named with its date, pose and weight) + photos.json.
// Import reads the JSON from such a ZIP or from a plain .json file; photos are not re-imported (yet).
package com.clement.nutrimo.data.repository

import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.data.db.points
import com.clement.nutrimo.domain.calc.kgAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_ENTRY = "backup.json"
private const val PHOTOS_ENTRY = "photos.json"
private const val PHOTO_FOLDER = "photos/"
/** First bytes of every ZIP file. */
private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B)

/** One line of photos.json: enough to re-import a photo later. */
@Serializable
data class PhotoExport(val file: String, val date: String, val pose: String, val frontCamera: Boolean, val note: String?, val kg: Double?)

class BackupArchive(private val backup: BackupRepository, private val photos: PhotoRepository, private val body: BodyRepository) {
    private val json = Json { prettyPrint = true }

    suspend fun write(out: OutputStream) {
        val data = backup.export()
        val list = photos.all()
        val weights = body.weightHistory.first().points()
        val exports = list.map { p -> p to exportOf(p, weights.kgAt(p.date)) }
        withContext(Dispatchers.IO) {
            ZipOutputStream(out).use { zip ->
                zip.text(BACKUP_ENTRY, data)
                zip.text(PHOTOS_ENTRY, json.encodeToString(exports.map { it.second }))
                exports.forEach { (photo, export) ->
                    val file = photos.file(photo).takeIf { it.exists() } ?: return@forEach
                    zip.putNextEntry(ZipEntry(PHOTO_FOLDER + export.file))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /** The JSON backup inside [bytes]: a ZIP export or a plain JSON file (older exports). */
    suspend fun backupText(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        if (bytes.size < 2 || bytes[0] != ZIP_MAGIC[0] || bytes[1] != ZIP_MAGIC[1]) return@withContext bytes.decodeToString()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.firstOrNull { it.name == BACKUP_ENTRY } ?: error("Fichier ZIP sans $BACKUP_ENTRY")
            zip.readBytes().decodeToString()
        }
    }
}

/** "2026-06-14_front_110,2kg_1a2b3c4d.jpg": readable in a file manager, unique thanks to the id prefix; the pose is
 * its enum name so file names don't depend on the display language. */
private fun exportOf(photo: PhotoEntity, kg: Double?): PhotoExport {
    val weight = kg?.let { "_" + String.format(Locale.FRANCE, "%.1fkg", it) }.orEmpty()
    val name = "${photo.date}_${photo.pose.name.lowercase(Locale.ROOT)}${weight}_${photo.id.take(8)}.jpg"
    return PhotoExport(name, photo.date.toString(), photo.pose.name, photo.frontCamera, photo.note, kg)
}

private fun ZipOutputStream.text(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray())
    closeEntry()
}

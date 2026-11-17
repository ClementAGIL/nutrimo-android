// Photo images loaded off the main thread: thumbnails for lists, larger versions for the comparison.
package com.clement.nutrimo.ui.photo

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.ui.common.appContainer

private const val THUMB_SIDE = 400
/** Enough for a 4× zoom on a phone screen without loading the whole 2,500 px file twice. */
const val COMPARE_SIDE = 2000

/** Thumbnail cropped to fill [modifier]'s size. */
@Composable
fun PhotoThumb(photo: PhotoEntity, modifier: Modifier) {
    val image by rememberPhoto(photo, THUMB_SIDE, thumbnail = true)
    val description = stringResource(photo.pose.label)
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        image?.let { Image(it, description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
fun rememberPhoto(photo: PhotoEntity?, maxSide: Int, thumbnail: Boolean = false) = run {
    val c = appContainer()
    produceState<ImageBitmap?>(null, photo?.id, photo?.updatedAt) {
        value = photo?.let { p -> c.photos.load(if (thumbnail) c.photos.thumbFile(p) else c.photos.file(p), maxSide)?.asImageBitmap() }
    }
}

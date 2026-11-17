// Demo progress photos: drawn silhouettes (front, side, back) that slim down with the demo weigh-ins.
package com.clement.nutrimo.data.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.points
import com.clement.nutrimo.domain.calc.kgAt
import com.clement.nutrimo.domain.model.PhotoPose
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private const val WIDTH = 900
private const val HEIGHT = 1200
/** Days ago of each photo session, spread over the demo history. */
private val SESSIONS = listOf(148L, 110L, 75L, 40L, 5L)
/** Weight drawn at 100 % width; lighter is narrower. */
private const val REFERENCE_KG = 95.0
private const val SKIN = 0xFFE0B49A.toInt()
private const val SHORTS = 0xFF3D6374.toInt()

class DemoPhotos(private val c: AppContainer, private val today: LocalDate) {
    suspend fun create() {
        val weights = c.body.weightHistory.first().points()
        SESSIONS.forEach { daysAgo ->
            val date = today.minusDays(daysAgo)
            val kg = weights.kgAt(date) ?: REFERENCE_KG
            PhotoPose.entries.forEach { pose ->
                c.photos.save(date, pose, frontCamera = false, note = if (daysAgo == SESSIONS.first()) "Photo de démo" else null, upright = draw(pose, kg))
            }
        }
    }

    private fun draw(pose: PhotoPose, kg: Double): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas)
        val girth = (kg / REFERENCE_KG).toFloat().let { 0.55f + 0.45f * it * it }
        when (pose) {
            PhotoPose.SIDE -> side(canvas, girth)
            else -> front(canvas, girth, back = pose == PhotoPose.BACK)
        }
        return bitmap
    }

    private fun background(canvas: Canvas) {
        val wall = Paint().apply { shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), 0xFFEFF3F1.toInt(), 0xFFCAD6D0.toInt(), Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), wall)
        canvas.drawRect(0f, HEIGHT * 0.88f, WIDTH.toFloat(), HEIGHT.toFloat(), Paint().apply { color = 0xFFB9A58F.toInt() })
    }

    /** Head, arms, torso as an oval whose width follows the weight, shorts and legs. */
    private fun front(canvas: Canvas, girth: Float, back: Boolean) {
        val skin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SKIN }
        val cx = WIDTH / 2f
        val torso = 170f * girth
        canvas.drawCircle(cx, 210f, 80f, skin)
        canvas.drawRoundRect(RectF(cx - torso - 70f, 330f, cx - torso + 5f, 760f), 40f, 40f, skin)
        canvas.drawRoundRect(RectF(cx + torso - 5f, 330f, cx + torso + 70f, 760f), 40f, 40f, skin)
        canvas.drawOval(RectF(cx - torso, 300f, cx + torso, 800f), skin)
        canvas.drawRoundRect(RectF(cx - torso * 0.85f, 700f, cx + torso * 0.85f, 860f), 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SHORTS })
        canvas.drawRoundRect(RectF(cx - torso * 0.8f, 840f, cx - 12f, 1060f), 40f, 40f, skin)
        canvas.drawRoundRect(RectF(cx + 12f, 840f, cx + torso * 0.8f, 1060f), 40f, 40f, skin)
        if (!back) canvas.drawCircle(cx, 560f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB98E78.toInt() })
    }

    /** Profile: the belly sticks out in front according to the weight. */
    private fun side(canvas: Canvas, girth: Float) {
        val skin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SKIN }
        val cx = WIDTH / 2f
        canvas.drawCircle(cx, 210f, 78f, skin)
        canvas.drawRoundRect(RectF(cx - 70f, 300f, cx + 60f, 800f), 60f, 60f, skin)
        canvas.drawOval(RectF(cx - 60f, 420f, cx + 60f + 170f * (girth - 0.55f) * 2f, 780f), skin)
        canvas.drawRoundRect(RectF(cx - 30f, 330f, cx + 30f, 740f), 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD4A68C.toInt() })
        canvas.drawRoundRect(RectF(cx - 75f, 700f, cx + 70f + 60f * girth, 860f), 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SHORTS })
        canvas.drawRoundRect(RectF(cx - 55f, 840f, cx + 55f, 1060f), 40f, 40f, skin)
    }
}

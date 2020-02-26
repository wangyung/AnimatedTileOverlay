package com.freddiew.animatedtile.app.tileprovider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.model.Tile
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class AnimatedTileProvider(context: Context) : TimedTileProvider {

    override var currentTimeStamp: Long = -1

    private val scaleFactor: Float = context.resources.displayMetrics.density * 0.6f
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        textSize = DEFAULT_TEXT_SIZE * scaleFactor
        color = Color.RED
    }

    private val baseBitmap: Bitmap = createBitmap(
        (TILE_SIZE * scaleFactor).toInt(),
        (TILE_SIZE * scaleFactor).toInt()
    )

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val bitmap = createTextBitmap()
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
        val tileData = byteArrayOutputStream.toByteArray()
        return Tile(TILE_SIZE, TILE_SIZE, tileData)
    }

    private fun createTextBitmap(): Bitmap = synchronized(baseBitmap) {
        baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }.applyCanvas {
        drawTileText()
    }

    private fun Canvas.drawTileText() {
        val index = currentTimeStamp % (TILE_SIZE - 4)
        val x = index * scaleFactor
        drawText("\uD83D\uDCA9\uD83D\uDCA9", x, DEFAULT_TEXT_SIZE.toFloat() * 2, textPaint)
    }

    companion object {
        private const val TAG = "AnimatedTileProvider"
        private const val TILE_SIZE = 256
        private const val DEFAULT_TEXT_SIZE = 20
    }
}

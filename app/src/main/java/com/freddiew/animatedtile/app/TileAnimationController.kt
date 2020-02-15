package com.freddiew.animatedtile.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import com.freddiew.animatedtile.app.tileprovider.TimeControl
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.freddiew.animatedtile.app.tileprovider.TimedTileProvider

class TileAnimationController(
    private val map: GoogleMap,
    private val timedTileProvider: TimedTileProvider,
    private val zIndex: Float = 99f
) : TimeControl {

    private var overlay1: TileOverlay? = null
    private var overlay2: TileOverlay? = null

    private var lock: Boolean = true

    private val handler: Handler = Handler(Looper.getMainLooper())

    override var currentTimeStamp: Long = 0
        @MainThread
        set(value) {
            field = value
            updateTile(value)
        }

    fun showTile() {
        overlay1 = map.addTileOverlay(
            TileOverlayOptions().tileProvider(timedTileProvider).zIndex(zIndex)
        )
        lock = false
    }

    fun hideTile() {
        overlay1?.remove()
        overlay2?.remove()
        lock = true
    }

    private fun updateTile(currentTimeStamp: Long) {
        ensureMainThread()
        timedTileProvider.currentTimeStamp = currentTimeStamp
        if (lock) {
            Log.d(TAG, "the swap is locked")
            return
        }

        swapTile()
        lock = true
    }

    private fun swapTile() {
        if (overlay1 != null) {
            overlay2 = map.addTileOverlay(
                TileOverlayOptions().tileProvider(timedTileProvider).fadeIn(false).zIndex(zIndex)
            )
            removeOverlay(overlay1) {
                overlay1 = null
            }
        } else {
            overlay1 = map.addTileOverlay(
                TileOverlayOptions().tileProvider(timedTileProvider).fadeIn(false).zIndex(zIndex)
            )
            removeOverlay(overlay2) {
                overlay2 = null
            }
        }
    }

    private fun removeOverlay(overlay: TileOverlay?, actionOnFinish: (() -> Unit)?) {
        handler.postDelayed({
            overlay?.remove()
            actionOnFinish?.invoke()
            lock = false
        }, SWAP_INTERVAL_MS)
    }

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalAccessException("Must run on main thread.")
        }
    }

    companion object {
        private const val TAG = "TimedTileAnimator"
        private const val SWAP_INTERVAL_MS = 150L
    }
}

package com.freddiew.animatedtile.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import com.freddiew.animatedtile.app.tileprovider.TimeControl
import com.freddiew.animatedtile.app.tileprovider.TimeControl.Companion.INVALID_TIMESTAMP
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.freddiew.animatedtile.app.tileprovider.TimedTileProvider
import kotlin.math.max

/**
 * The controller that decrease the flickers when updating overlays on Google Maps.
 *
 * Since we can't control the render of Google Maps directly, if we need to update the tile
 * overlay we need to remove current one and add the new one. Unfortunately, Google maps would be
 * updated immediately when the overlay is removed. It causes Map view flicker dramatically. In order
 * to decrease the flickers. We can add the next overlay first and remove current overlay later.
 * Even though, we can't avoid the flickers completely.
 */
@MainThread
class TileAnimationController(
    private val map: GoogleMap,
    private val timedTileProvider: TimedTileProvider,
    private val zIndex: Float = 99f
) : TimeControl {

    private var overlay1: TileOverlay? = null
    private var overlay2: TileOverlay? = null

    private var lock: Boolean = true

    private val handler: Handler = Handler(Looper.getMainLooper())

    private var pendingTimestamp: Long = INVALID_TIMESTAMP

    private val isTileVisible: Boolean
        get() = overlay1 != null || overlay2 != null

    override var currentTimeStamp: Long = 0
        set(value) {
            if (field != value) {
                field = max(value, 0)
                timedTileProvider.currentTimeStamp = value
                if (isTileVisible) {
                    updateTile(value)
                }
            }
        }
    /**
     * Show the tile overlay on google maps.
     */
    fun showTile() {
        overlay1 = map.addTileOverlay(
            TileOverlayOptions().tileProvider(timedTileProvider).zIndex(zIndex)
        )
        lock = false
    }

    /**
     * Hide the tile overlay on google maps.
     */
    fun hideTile() {
        overlay1?.remove()
        overlay2?.remove()
        overlay1 = null
        overlay2 = null
        lock = true
    }

    private fun updateTile(timestamp: Long) {
        ensureMainThread()
        timedTileProvider.currentTimeStamp = timestamp
        if (lock) {
            Log.d(TAG, "tile swapping is locked")
            pendingTimestamp = timestamp
            return
        }
        pendingTimestamp = INVALID_TIMESTAMP
        lock = true
        swapTiles()
    }

    private fun swapTiles() {
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
            if (pendingTimestamp != INVALID_TIMESTAMP) {
                Log.d(TAG, "Update pending timestamp: $pendingTimestamp")
                updateTile(pendingTimestamp)
            }
        }, SWAP_INTERVAL_MS)
    }

    private fun ensureMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalAccessException("Must run on main thread.")
        }
    }

    companion object {
        private const val TAG = "TimedTileAnimator"
        private const val SWAP_INTERVAL_MS = 100L
    }
}

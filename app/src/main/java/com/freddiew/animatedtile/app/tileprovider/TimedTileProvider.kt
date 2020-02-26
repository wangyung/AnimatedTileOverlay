package com.freddiew.animatedtile.app.tileprovider

import com.google.android.gms.maps.model.TileProvider

interface TimeControl {
    var currentTimeStamp: Long

    companion object {
        const val INVALID_TIMESTAMP = -1L
    }
}

interface TimedTileProvider : TileProvider, TimeControl

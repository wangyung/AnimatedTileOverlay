package com.freddiew.animatedtile.app.tileprovider

import com.google.android.gms.maps.model.TileProvider

interface TimeControl {
    var currentTimeStamp: Long
}

interface TimedTileProvider : TileProvider, TimeControl

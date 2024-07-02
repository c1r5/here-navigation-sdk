package com.cire.herenavigation.core

import com.here.sdk.core.Color
import com.here.sdk.core.GeoCircle
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoPolygon
import com.here.sdk.mapview.MapPolygon
import com.here.sdk.mapview.MapScene

fun MapScene.addCircle(center: GeoCoordinates, radius: Double, color: Int) {
    val geoCircle = GeoCircle(center, radius)
    val fillColor = Color.valueOf(color)
    val geoPolygon = GeoPolygon(geoCircle)
    val mapPolygon = MapPolygon(geoPolygon, fillColor)
    removeMapPolygon(mapPolygon)
    addMapPolygon(mapPolygon)
}
package com.cire.herenavigation.core

import com.here.sdk.mapview.MapImageFactory
import com.here.sdk.mapview.MapMarker
import com.here.sdk.mapview.MapView
import com.here.sdk.routing.Waypoint

fun Waypoint.addMarker(
    mapView: MapView,
    drawable: Int
) {
    val context = mapView.context
    val mapScene = mapView.mapScene
    val geoCoordinates = coordinates
    val mapMarker = MapMarker(geoCoordinates, MapImageFactory.fromResource(context.resources, drawable))
    mapScene.addMapMarker(mapMarker)
}
package com.cire.herenavigation.core

import com.here.sdk.core.Color
import com.here.sdk.mapview.LineCap
import com.here.sdk.mapview.MapMeasureDependentRenderSize
import com.here.sdk.mapview.MapPolyline
import com.here.sdk.mapview.MapScene
import com.here.sdk.mapview.RenderSize
import com.here.sdk.routing.Route

fun Route.addRoute(mapScene: MapScene, color: Color, width: Int) {
    val mapMeasureDependentRenderSize =  MapMeasureDependentRenderSize(RenderSize.Unit.PIXELS, width.toDouble())
    val mapPolylineRepresentation = MapPolyline.SolidRepresentation(mapMeasureDependentRenderSize, color, LineCap.ROUND)
    val mapPolyline = MapPolyline(geometry, mapPolylineRepresentation)
    mapScene.addMapPolyline(mapPolyline)
}
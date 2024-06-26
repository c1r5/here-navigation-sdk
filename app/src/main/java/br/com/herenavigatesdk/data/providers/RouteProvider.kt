package br.com.herenavigatesdk.data.providers

import android.util.Log
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import br.com.herenavigatesdk.data.dtos.RoutePoint
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.routing.Waypoint

class RouteProvider(
    points: List<RoutePoint>
) {
    init {
        Log.d(TAG, "RouteProvider: $points")
    }

    private val waypoints = points.map {
        val geoCoordinates = GeoCoordinates(it.lat, it.lng)
        Waypoint(geoCoordinates)
    }

    fun origin() = waypoints.first()
    fun destination() = waypoints.last()
    fun waypoints(): List<Waypoint> = waypoints.drop(1).dropLast(1)
}
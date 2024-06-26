package com.cire.herenavigation.core

import android.util.Log
import com.here.sdk.routing.CalculateRouteCallback
import com.here.sdk.routing.CarOptions
import com.here.sdk.routing.RoutingEngine
import com.here.sdk.routing.Waypoint

class RouteCalculator {

    companion object {
        private var routingEngine: RoutingEngine? = null

        @JvmStatic
        fun initRoutingEngine(onInitializationError: (error: InstantiationException?) -> Unit) {
            try {
                routingEngine = RoutingEngine()
            } catch (e: InstantiationException) {
                onInitializationError(e)
            }
        }

        fun calculateRoute(
            startWaypoint: Waypoint,
            destinationWaypoint: Waypoint,
            stopWaypoints: List<Waypoint>,
            calculateRouteCallback: CalculateRouteCallback
        ) {

            val waypoints = arrayListOf(
                startWaypoint,
                *stopWaypoints.toTypedArray(),
                destinationWaypoint
            )
            Log.d(
                RouteCalculator::class.java.name,
                "waypoints: ${startWaypoint.coordinates.latitude} / ${startWaypoint.coordinates.longitude}"
            )
            routingEngine?.calculateRoute(waypoints, CarOptions(), calculateRouteCallback)
                ?: throw IllegalStateException("Routing engine is not initialized.")
        }
    }


}
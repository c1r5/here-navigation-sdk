package br.com.herenavigatesdk.ui.heremodule

import com.here.sdk.routing.CalculateRouteCallback
import com.here.sdk.routing.CarOptions
import com.here.sdk.routing.RoutingEngine
import com.here.sdk.routing.Waypoint

class RouteCalculator {
    private var routingEngine: RoutingEngine? = null

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
            destinationWaypoint
        ).apply {
            addAll(1, stopWaypoints)
        }
        routingEngine?.calculateRoute(waypoints, CarOptions(), calculateRouteCallback)
    }
}
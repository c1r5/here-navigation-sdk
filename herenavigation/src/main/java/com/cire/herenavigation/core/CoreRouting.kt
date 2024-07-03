package com.cire.herenavigation.core

import com.here.sdk.routing.CalculateRouteCallback
import com.here.sdk.routing.CarOptions
import com.here.sdk.routing.Route
import com.here.sdk.routing.RoutingEngine

class CoreRouting {
    companion object {
        private var routingEngine: RoutingEngine? = null

        @JvmStatic
        fun init(): InstantiationException? {
            return try {
                routingEngine = RoutingEngine()
                null
            } catch (e: InstantiationException) {
                e
            }
        }

        @JvmStatic
        fun isInitialized(): Boolean {
            return routingEngine != null
        }

        @JvmStatic
        fun dispose() {
        }
    }

    fun calculateRoute(routeProvider: RouteProvider, calculateRouteCallback: CalculateRouteCallback) {
        val origin = routeProvider.origin()
        val destination = routeProvider.destination()
        val stopWaypoints = routeProvider.waypoints()

        val waypoints = arrayListOf(
            origin,
            *stopWaypoints.toTypedArray(),
            destination
        )

        routingEngine?.calculateRoute(waypoints, CarOptions(), calculateRouteCallback)
    }
}
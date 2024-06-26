package com.cire.herenavigation.core

import android.util.Log
import com.here.sdk.core.LocationListener
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.MapView
import com.here.sdk.navigation.LocationSimulator
import com.here.sdk.navigation.LocationSimulatorOptions
import com.here.sdk.navigation.VisualNavigator
import com.here.sdk.routing.Route


class RouteNavigation(private val mapView: MapView) {
    private var visualNavigator: VisualNavigator? = null
    private var calculatedRoute: Route? = null

    fun initVisualNavigator(onInitializationError: (error: InstantiationErrorException) -> Unit) {
        try {
            visualNavigator = VisualNavigator()
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization of VisualNavigator failed: " + e.error.name)
        }
    }

    fun setRoute(route: Route) = apply { this.calculatedRoute = route }

    fun startNavigation(
        onStartNavigationError: (startNavigationError: NullPointerException) -> Unit
    ) {
        if (calculatedRoute == null) return onStartNavigationError(NullPointerException("Route is not set"))
        if (visualNavigator == null) return onStartNavigationError(NullPointerException("VisualNavigator is not initialized"))

        visualNavigator?.apply {
            route = this.route
            startRendering(mapView)
            setEventTextListener { eventText ->
                Log.d("ManeuverNotifications", eventText.text)
            }
        }?.let {
            setupLocationSource(it, calculatedRoute!!)
        }
    }

    private fun setupLocationSource(locationListener: LocationListener, route: Route) {
        try {
            // Provides fake GPS signals based on the route geometry.
            LocationSimulator(route, LocationSimulatorOptions()).apply {
                listener = locationListener
                start()
            }
        } catch (e: InstantiationErrorException) {
            throw java.lang.RuntimeException("Initialization of LocationSimulator failed: " + e.error.name)
        }
    }
}
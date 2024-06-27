package com.cire.herenavigation.core

import android.util.Log
import com.here.sdk.core.LocationListener
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.MapView
import com.here.sdk.navigation.LocationSimulator
import com.here.sdk.navigation.LocationSimulatorOptions
import com.here.sdk.navigation.VisualNavigator
import com.here.sdk.routing.Route


class CoreNavigation(private val mapView: MapView) {

    companion object {
        private var visualNavigator: VisualNavigator? = null
        private var calculatedRoute: Route? = null

        fun init(): InstantiationErrorException? {
            return try {
                visualNavigator = VisualNavigator()
                null
            } catch (e: InstantiationErrorException) {
                e
            }
        }
    }

    fun setRoute(route: Route) = apply { calculatedRoute = route }

    fun startNavigation(): Result<Unit> {
        visualNavigator?.apply {
            route = this.route
            startRendering(mapView)
            setEventTextListener { eventText ->
                Log.d("ManeuverNotifications", eventText.text)
            }

            setupLocationSource(this)
        }?.let {
            return Result.success(Unit)
        } ?: run {
            return Result.failure(RuntimeException("VisualNavigator is null"))
        }
    }

    fun dispose() {
        visualNavigator?.stopRendering()
    }

    private fun setupLocationSource(locationListener: LocationListener) {
        calculatedRoute?.let {
            LocationSimulator(it, LocationSimulatorOptions()).apply {
                listener = locationListener
                start()
            }
        }
    }
}
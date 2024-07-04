package com.cire.herenavigation.core

import android.content.Context
import com.here.odnp.util.Log
import com.here.sdk.core.Color
import com.here.sdk.core.GeoCircle
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoPolygon
import com.here.sdk.core.Location
import com.here.sdk.core.LocationListener
import com.here.sdk.core.engine.SDKNativeEngine
import com.here.sdk.core.engine.SDKOptions
import com.here.sdk.mapview.LocationIndicator
import com.here.sdk.mapview.LocationIndicator.IndicatorStyle
import com.here.sdk.mapview.MapPolygon
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import com.here.sdk.routing.CalculateRouteCallback
import com.here.sdk.routing.Route
import com.here.sdk.routing.Waypoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date


class CoreSDK(
    private val mapView: MapView
): LocationListener {


    companion object {
        private val _onCoreError = MutableStateFlow<Throwable?>(null)

        @JvmStatic
        val isInitialized: Boolean get() = SDKNativeEngine.getSharedInstance() != null

        @JvmStatic
        fun toGeoCoordinates(latitude: Double, longitude: Double): GeoCoordinates {
            return GeoCoordinates(latitude, longitude)
        }

        @JvmStatic
        fun GeoCoordinates.toWayPoint(): Waypoint {
            return Waypoint(this)
        }

        @JvmStatic
        fun GeoCoordinates.toLocation(): Location {
            return Location(this).apply {
                time = Date()
                bearingInDegrees = Math.random() * 360
            }
        }

        fun init(
            context: Context,
            accessKeyID: String,
            accessKeySecret: String,
        ) {
            try {
                val sdkOptions = SDKOptions(accessKeyID, accessKeySecret)
                SDKNativeEngine.makeSharedInstance(context, sdkOptions)
            } catch (e: InstantiationException) {
                _onCoreError.tryEmit(e.cause)
            }
        }

        @JvmStatic
        fun dispose() {
            val sdkNativeEngine = SDKNativeEngine.getSharedInstance()
            if (sdkNativeEngine != null) {
                sdkNativeEngine.dispose()
                SDKNativeEngine.setSharedInstance(null)
            }
        }
    }

    private val mapScene = mapView.mapScene
    private val locationIndicator = LocationIndicator(mapView)
    private var location: Location? = null

    fun onCoreError(error: (Throwable?) -> Unit) = CoroutineScope(Dispatchers.Main).launch {
        _onCoreError.collect {
            it?.let { error(it) }
        }
    }
    fun indicator(geoCoordinates: GeoCoordinates) {
        location = Location(geoCoordinates).apply {
            time = Date()
            bearingInDegrees = Math.random() * 360
        }
        locationIndicator.apply {
            updateLocation(location!!)
            locationIndicatorStyle = IndicatorStyle.PEDESTRIAN

            if (!isActive) {
                enable(mapView)
            }
        }
    }
    fun loadScene() {
        mapScene.loadScene(MapScheme.NORMAL_DAY) { mapError ->
            mapError?.let {
                _onCoreError.tryEmit(Throwable(it.name))
            } ?: run {

            }
        }
    }

    fun showCircle(center: GeoCoordinates, radius: Double, color: Int) {
        mapScene.addCircle(center, radius, color)
    }

    fun showMarkers(
        routeProvider: RouteProvider,
        stopMarkerDrawable: Int,
        originMarkerDrawable: Int = stopMarkerDrawable,
        destinationMarkerDrawable: Int = stopMarkerDrawable
    ) {
        routeProvider.waypoints().forEach { waypoint -> waypoint.addMarker(mapView, stopMarkerDrawable) }
        routeProvider.origin().addMarker(mapView, originMarkerDrawable)
        routeProvider.destination().addMarker(mapView, destinationMarkerDrawable)
    }

    fun showRoute(routeProvider: RouteProvider, color: Int, width: Int) {
        if (CoreRouting.isInitialized()) {
            CoreRouting().calculateRoute(
                routeProvider
            ) { routingError, routes ->
                routingError?.let {
                    _onCoreError.tryEmit(ShowRouteException(it.name))
                } ?: run {
                    Log.d("HERENAVIGATESDK", "calculateRoute: $routes")
                    routes?.first()?.let {route: Route ->  mapScene.addRoute(route, Color.valueOf(color), width) }
                }
            }
        } else {
            _onCoreError.tryEmit(Throwable("Routing engine is not initialized."))
        }
    }

    override fun onLocationUpdated(p0: Location) {
        location = p0
        locationIndicator.updateLocation(p0)
    }

    class ShowRouteException(message: String) : Exception(message)
}
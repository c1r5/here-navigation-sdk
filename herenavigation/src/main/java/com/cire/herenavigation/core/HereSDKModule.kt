package com.cire.herenavigation.core

import android.content.Context
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoCoordinatesUpdate
import com.here.sdk.core.GeoOrientationUpdate
import com.here.sdk.core.Location
import com.here.sdk.core.engine.SDKNativeEngine
import com.here.sdk.core.engine.SDKOptions
import com.here.sdk.mapview.LineCap
import com.here.sdk.mapview.LocationIndicator
import com.here.sdk.mapview.LocationIndicator.IndicatorStyle
import com.here.sdk.mapview.MapCameraAnimationFactory
import com.here.sdk.mapview.MapError
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapMeasureDependentRenderSize
import com.here.sdk.mapview.MapPolyline
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import com.here.sdk.mapview.MapView.OnReadyListener
import com.here.sdk.mapview.RenderSize
import com.here.sdk.routing.Route
import com.here.time.Duration
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date


class HereSDKModule(
    private val mapView: MapView
) {
    companion object {
        private val _onInstantiationException = MutableStateFlow<InstantiationException?>(null)
        private val _onMapLoadError = MutableStateFlow<MapError?>(null)
        private val _onMapReady = MutableStateFlow<Unit?>(null)

        @JvmStatic
        val onMapReady: Flow<Unit?> = _onMapReady.asStateFlow()

        @JvmStatic
        val onInstantiationException = _onInstantiationException.asStateFlow()

        @JvmStatic
        val onMapLoadError: Flow<MapError?> = _onMapLoadError.asStateFlow()

        @JvmStatic
        fun initialize(
            context: Context,
            accessKeyID: String,
            accessKeySecret: String
        ) {
            val sdkOptions = SDKOptions(accessKeyID, accessKeySecret)

            try {
                SDKNativeEngine.makeSharedInstance(context, sdkOptions)
            } catch (e: InstantiationException) {
                MainScope().launch { _onInstantiationException.emit(e) }
            }
        }

        @JvmStatic
        fun dispose() {
            // Free HERE SDK resources before the application shuts down.
            // Usually, this should be called only on application termination.
            // Afterwards, the HERE SDK is no longer usable unless it is initialized again.
            val sdkNativeEngine = SDKNativeEngine.getSharedInstance()
            if (sdkNativeEngine != null) {
                sdkNativeEngine.dispose()
                // For safety reasons, we explicitly set the shared instance to null to avoid situations,
                // where a disposed instance is accidentally reused.
                SDKNativeEngine.setSharedInstance(null)
            }
        }

        @JvmStatic
        fun latlngToGeoCoordinates(latitude: Double, longitude: Double): GeoCoordinates {
            return GeoCoordinates(latitude, longitude)
        }
    }


    private lateinit var location: Location
    
    private val cameraFloated = false
    private val mapCamera = mapView.camera
    private val mapScene = mapView.mapScene

    private val defaultDistance = (1000 * 10).toDouble()
    private val defaultMeasure = MapMeasure(MapMeasure.Kind.DISTANCE, defaultDistance)
    private val locationIndicator = LocationIndicator(mapView)

    private val onMapReadyListener = OnReadyListener {
        MainScope().launch { _onMapReady.emit(Unit) }
    }

    init {
        mapScene.loadScene(MapScheme.NORMAL_DAY) { mapError ->
            mapError?.let {
                MainScope().launch { _onMapLoadError.emit(it) }
            } ?: run {
                mapView.setOnReadyListener(onMapReadyListener)
            }
        }

        locationIndicator.locationIndicatorStyle = IndicatorStyle.PEDESTRIAN
        locationIndicator.enable(mapView)

    }

    fun startNavigation() {

    }

    fun onLocationUpdated(geoCoordinates: GeoCoordinates) {
        location = Location(geoCoordinates)

        location.time = Date()
        location.coordinates = geoCoordinates

        if (!cameraFloated) {
            cameraFly(geoCoordinates)
        }

        locationIndicator.updateLocation(location)
    }

    fun recenter() {
        cameraFly(location.coordinates)
    }

    fun addRoute(route: Route, appearence: RouteAppearence = RouteAppearence.DEFAULT) {
        val geometry = route.geometry
        val mapPolylineRepresentation = MapPolyline.SolidRepresentation(
            MapMeasureDependentRenderSize(RenderSize.Unit.PIXELS, appearence.width.toDouble()),
            appearence.color,
            LineCap.ROUND
        )
        val mapPolyline = MapPolyline(geometry, mapPolylineRepresentation)
        mapScene.addMapPolyline(mapPolyline)
    }

    private fun cameraFly(geoCoordinates: GeoCoordinates) {
        val geoCoordinatesUpdate = GeoCoordinatesUpdate(geoCoordinates)
        val bowFactor = 1.0
        val animation =
            MapCameraAnimationFactory.flyTo(
                geoCoordinatesUpdate,
                defaultMeasure,
                bowFactor,
                Duration.ofSeconds(3)
            )
        mapCamera.startAnimation(animation)
    }

    private fun cameraNavigationTilt() {
        val bearingInDegress = 0.0
        val tiltInDegress = 45.0
        val orientation =
            GeoOrientationUpdate(bearingInDegress, tiltInDegress)
        val distanceInMeters = (1000 * 1).toDouble()
        val mapMeasureZoom = MapMeasure(MapMeasure.Kind.DISTANCE, distanceInMeters)
        cameraAnimation(
            geoOrientationUpdate = orientation,
            geoCoordinatesUpdate = GeoCoordinatesUpdate(location.coordinates),
            mapMeasure = mapMeasureZoom,
            bowFactor = 1.0,
            duration = Duration.ofSeconds(3)
        )
    }

    private fun cameraNormalTilt() {
        val bowFactor = 1.0
        val coordinatesUpdate = GeoCoordinatesUpdate(location.coordinates)
        cameraAnimation(
            geoCoordinatesUpdate = coordinatesUpdate,
            mapMeasure = defaultMeasure,
            bowFactor = bowFactor,
            duration = Duration.ofSeconds(3)
        )
    }

    private fun cameraAnimation(
        geoOrientationUpdate: GeoOrientationUpdate = GeoOrientationUpdate(0.0, 0.0),
        geoCoordinatesUpdate: GeoCoordinatesUpdate,
        mapMeasure: MapMeasure,
        bowFactor: Double,
        duration: Duration
    ) {
        val animation =
            MapCameraAnimationFactory.flyTo(
                geoCoordinatesUpdate,
                geoOrientationUpdate,
                mapMeasure,
                bowFactor,
                duration
            )
        mapCamera.startAnimation(animation)
    }

}
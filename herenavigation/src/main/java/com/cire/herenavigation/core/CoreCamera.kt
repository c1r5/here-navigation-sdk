package com.cire.herenavigation.core

import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoCoordinatesUpdate
import com.here.sdk.core.GeoOrientationUpdate
import com.here.sdk.mapview.MapCameraAnimationFactory
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapView
import com.here.time.Duration

class CoreCamera(private val mapView: MapView) {
    private val mapCamera = mapView.camera
    private val defaultDistance = (1000 * 250).toDouble()

    fun recenter(geoCoordinates: GeoCoordinates, distance: Double = defaultDistance) {
        val recenterMeasure = MapMeasure(MapMeasure.Kind.DISTANCE, distance)
        val geoCoordinatesUpdate = GeoCoordinatesUpdate(geoCoordinates)
        val bowFactor = 1.0

        cameraAnimation(
            geoCoordinatesUpdate = geoCoordinatesUpdate,
            mapMeasure = recenterMeasure,
            bowFactor = bowFactor,
            duration = Duration.ofSeconds(3)
        )
    }

    fun lookAt(geoCoordinates: GeoCoordinates, distance: Double = defaultDistance) {
        mapCamera.lookAt(geoCoordinates)
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
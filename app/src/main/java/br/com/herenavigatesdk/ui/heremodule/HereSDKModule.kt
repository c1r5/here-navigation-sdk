package br.com.herenavigatesdk.ui.heremodule

import android.content.Context
import br.com.herenavigatesdk.R
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoCoordinatesUpdate
import com.here.sdk.core.Location
import com.here.sdk.core.engine.SDKNativeEngine
import com.here.sdk.core.engine.SDKOptions
import com.here.sdk.mapview.LocationIndicator
import com.here.sdk.mapview.LocationIndicator.IndicatorStyle
import com.here.sdk.mapview.MapCameraAnimationFactory
import com.here.sdk.mapview.MapCameraUpdate
import com.here.sdk.mapview.MapCameraUpdateFactory
import com.here.sdk.mapview.MapError
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import com.here.sdk.mapview.MapView.OnReadyListener
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
        private val _indicatorStyle = MutableStateFlow<IndicatorStyle>(IndicatorStyle.PEDESTRIAN)

        @JvmStatic
        val onMapReady: Flow<Unit?> = _onMapReady.asStateFlow()

        @JvmStatic
        val onInstantiationException = _onInstantiationException.asStateFlow()

        @JvmStatic
        val onMapLoadError: Flow<MapError?> = _onMapLoadError.asStateFlow()

        @JvmStatic
        val indicatorStyle: Flow<IndicatorStyle> = _indicatorStyle.asStateFlow()

        @JvmStatic
        fun initialize(context: Context) {
            context.run {
                val accessKeyID = getString(R.string.HERE_ACCESS_KEY_ID)
                val accessKeySecret = getString(R.string.HERE_ACCESS_KEY_SECRET)
                val sdkOptions = SDKOptions(accessKeyID, accessKeySecret)

                try {
                    SDKNativeEngine.makeSharedInstance(this, sdkOptions)
                } catch (e: InstantiationException) {
                    MainScope().launch { _onInstantiationException.emit(e) }
                }
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

    private val cameraFloated = false
    private val defaultDistance = (1000 * 20).toDouble()
    private val mapMeasure = MapMeasure(MapMeasure.Kind.DISTANCE, defaultDistance)
    private val locationIndicator = LocationIndicator(mapView)
    private val mapCamera = mapView.camera
    private val onMapReadyListener = OnReadyListener {
        MainScope().launch { _onMapReady.emit(Unit) }
    }

    init {
        mapView.mapScene.loadScene(MapScheme.NORMAL_DAY) { mapError ->
            mapError?.let {
                MainScope().launch { _onMapLoadError.emit(it) }
            } ?: run {
                mapView.setOnReadyListener(onMapReadyListener)
            }
        }

        locationIndicator.locationIndicatorStyle = _indicatorStyle.value
        locationIndicator.enable(mapView)

    }

    fun onLocationUpdated(geoCoordinates: GeoCoordinates) {
        val mapCameraUpdate = cameraUpdate(geoCoordinates)
        val location = Location(geoCoordinates)

        location.time = Date()
        location.coordinates = geoCoordinates

        if (!cameraFloated) {
            cameraFly(geoCoordinates)
        } else {
            mapCamera.applyUpdate(mapCameraUpdate)
        }

        locationIndicator.updateLocation(location)
    }

    private fun cameraUpdate(geoCoordinates: GeoCoordinates): MapCameraUpdate {
        val geoCoordinatesUpdate = GeoCoordinatesUpdate(geoCoordinates)
        return MapCameraUpdateFactory.lookAt(geoCoordinatesUpdate, mapMeasure)
    }

    private fun cameraFly(geoCoordinates: GeoCoordinates) {
        val geoCoordinatesUpdate = GeoCoordinatesUpdate(geoCoordinates)
        val bowFactor = 1.0
        val animation =
            MapCameraAnimationFactory.flyTo(geoCoordinatesUpdate, bowFactor, Duration.ofSeconds(3))
        mapCamera.startAnimation(animation)
    }

}
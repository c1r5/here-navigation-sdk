package br.com.herenavigatesdk.ui.viewmodels

import android.location.Location
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import br.com.herenavigatesdk.BaseApp.Companion.mockedRoutePoints
import br.com.herenavigatesdk.R
import br.com.herenavigatesdk.data.providers.RouteProviderImpl
import com.cire.herenavigation.audio.VoiceAssistant
import com.cire.herenavigation.core.CoreCamera
import com.cire.herenavigation.core.CoreNavigation
import com.cire.herenavigation.core.CoreRouting
import com.cire.herenavigation.core.CoreSDK
import com.cire.herenavigation.core.CoreSDK.Companion.toWayPoint
import com.cire.herenavigation.core.addMarker
import com.cire.herenavigation.core.addRoute
import com.cire.herenavigation.provider.HEREPositioningSimulator
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.OnSuccessListener
import com.here.sdk.core.Color
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoOrientationUpdate
import com.here.sdk.core.LocationListener
import com.here.sdk.location.LocationAccuracy
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapView
import com.here.sdk.navigation.MilestoneStatus
import com.here.sdk.routing.CalculateRouteCallback
import com.here.sdk.routing.Route
import com.here.sdk.routing.Waypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.round

class MapActivityViewModel : ViewModel() {
    private var voiceAssistant: VoiceAssistant? = null

    private val herePositioningSimulator =
        HEREPositioningSimulator()



    private val calculatedRoute = MutableStateFlow<Route?>(null)
    private var coreLoader: CoreLoader? = null

    private val _isPositioning = MutableStateFlow(false)
    private val _mockAlertPoints = MutableStateFlow<MutableList<GeoCoordinates>>(mutableListOf())
    private val _isNavigating = MutableStateFlow(false)
    private val _onNavigationError = MutableStateFlow<Throwable?>(null)
    private val _lastAlert = MutableStateFlow<AlertType?>(null)
    private val _userZoomDistance = MutableStateFlow(1000 * 3)

    val onNavigationError = _onNavigationError.asStateFlow()
    val isNavigating = _isNavigating.asStateFlow()
    val isPositioning = _isPositioning.asStateFlow()
    val mockAlertPoints = _mockAlertPoints.asStateFlow()
    val userZoomDistance = _userZoomDistance.asStateFlow()

    companion object {
        class CoreLoader(
            val coreCamera: CoreCamera?,
            val coreRouting: CoreRouting?,
            val coreNavigation: CoreNavigation?,
            val coreSDK: CoreSDK?,
        ) {
            open class Builder(
                private var coreCamera: CoreCamera? = null,
                private var coreRouting: CoreRouting? = null,
                private var coreNavigation: CoreNavigation? = null,
                private var coreSDK: CoreSDK? = null
            ) {
                fun coreCamera(coreCamera: CoreCamera) = apply { this.coreCamera = coreCamera }
                fun coreRouting(coreRouting: CoreRouting) = apply { this.coreRouting = coreRouting }
                fun coreNavigation(coreNavigation: CoreNavigation) =
                    apply { this.coreNavigation = coreNavigation }

                fun coreSDK(coreSDK: CoreSDK) = apply { this.coreSDK = coreSDK }
                fun build() =
                    CoreLoader(coreCamera, coreRouting, coreNavigation, coreSDK)
            }
        }
    }

    val hereLocationAccuracy = MutableStateFlow(LocationAccuracy.BEST_AVAILABLE)
    val hereLocationListener: (mapview: MapView, coresdk: CoreSDK) -> LocationListener = { mapView, coreSDK ->
        LocationListener {
            coreSDK.updateLocation(it)
        }
    }

    fun setAccuray(accuracy: LocationAccuracy) {
        viewModelScope.launch {
            hereLocationAccuracy.emit(accuracy)
        }
    }

//    fun oneTimeLocationListener(
//        coreLoader: CoreLoader
//    ) = OnSuccessListener<Location> { location ->
//        geoCoordinates.value = GeoCoordinates(location.latitude, location.longitude)
//        this.coreLoader = coreLoader
//
//        val coreCamera = coreLoader.coreCamera!!
//        val coreSDK = coreLoader.coreSDK!!
//        val coreRouting = coreLoader.coreRouting!!
//        val coreNavigation = coreLoader.coreNavigation!!
//
//        routeProvider.waypoints = waypoints
//        routeProvider.origin = Waypoint(geoCoordinates.value!!)
//        viewModelScope.launch {
//            coreCamera.recenter(geoCoordinates.value!!)
//            coreSDK.indicator(geoCoordinates.value!!)
//            coreSDK.showMarkers(routeProvider, icWaypoint, icOrigin, icDestination)
//            setMockAlertPoint(coreSDK.mapView, GeoCoordinates(-26.92084457068429, -48.67563123438851))
//            setMockAlertPoint(coreSDK.mapView, GeoCoordinates(-26.919312614575023, -48.67259518613555))
//            setMockAlertPoint(coreSDK.mapView, GeoCoordinates(-26.91415802068359, -48.66482667903886))
//            mockedRoutePoints.forEach {
//                coreSDK.showCircle(CoreSDK.toGeoCoordinates(it.lat, it.lng), it.raio, R.color.blue_A700)
//            }
//            coreRouting.calculateRoute(
//                routeProvider,
//                calculateRouteCallback(coreSDK.mapView, coreNavigation)
//            )
//        }
//    }
//
//    fun onRecenterButtonPressed() {
//        geoCoordinates.value?.let {
//            coreLoader?.coreCamera?.recenter(it, _userZoomDistance.value.toDouble())
//        }
//    }
//
//    fun stopNavigationEvent() {
//        viewModelScope.launch {
//            coreLoader?.coreNavigation?.dispose()
//            coreLoader?.coreCamera?.recenter(geoCoordinates.value!!, _userZoomDistance.value.toDouble())
//            herePositioningSimulator.stopLocating()
//            _isNavigating.emit(false)
//        }
//    }
//
//    fun startNavigationEvent() {
//        coreLoader?.coreNavigation?.voiceAssistant?.let {
//            voiceAssistant = it
//        }
//        viewModelScope.launch {
//
//            coreLoader
//                ?.coreNavigation?.setMilestoneStatusListener { milestone, milestoneStatus ->
//                    when (milestoneStatus) {
//                        MilestoneStatus.REACHED -> {
//
//                        }
//                        MilestoneStatus.MISSED -> {
//                            Log.d(TAG, "MISSED")
//                        }
//                    }
//                }?.setDestinationReachedListener {
//
//                }?.startNavigation {locationListener ->
//
//                    _isNavigating.tryEmit(true)
//
//                    if (calculatedRoute.value == null){
//                        _onNavigationError.tryEmit(Throwable("Route is null"))
//                        return@startNavigation
//                    }
//                    onSecurityBeltAlert()
//                    herePositioningSimulator.startLocating({location ->
//                        locationListener.onLocationUpdated(location)
//
//                        val distance1 = round(_mockAlertPoints.value[1].distanceTo(location.coordinates)).toInt()
//                        val distance2 = round(_mockAlertPoints.value[2].distanceTo(location.coordinates)).toInt()
//
//                        if (distance1 in 0..100) {
//                            if (_lastAlert.value != AlertType.SMOKING) {
//                                onSmokingAlert()
//                            }
//                        }
//
//                        if (distance2 in 0..100) {
//                            if (_lastAlert.value != AlertType.SPEED) {
//                                onSpeedAlert()
//                            }
//                        }
//
//                        val distanceToWaypoint = location.coordinates.distanceTo(routeProvider.waypoints().first().coordinates)
//                        val isNearWaypoint = round(distanceToWaypoint).toInt() in 100..300
//                        if (isNearWaypoint) {
//                            if (_lastAlert.value != AlertType.NEAR_DESTINATION) {
//                                speak("Você está proximo do destino.")
//                                _lastAlert.value = AlertType.NEAR_DESTINATION
//                            }
//                        }
//                    }, calculatedRoute.value!!)
//                }
//        }
//    }
//
//    fun onAudioToggleButtonPressed() {
//        ttsState()?.let { isPaused ->
//            if (isPaused) {
//                coreLoader?.coreNavigation?.resumeTTS()
//            } else {
//                coreLoader?.coreNavigation?.pauseTTS()
//            }
//        }
//    }

//    fun ttsState() = coreLoader?.coreNavigation?.ttsState()
//
//    fun onLocationChangedCallback() = object : LocationCallback() {
//        override fun onLocationResult(result: LocationResult) {
//            _onLocationChanged.tryEmit(result.lastLocation)
//            result.lastLocation?.let {
//                geoCoordinates.value = GeoCoordinates(it.latitude, it.longitude)
//            }
//        }
//
//        override fun onLocationAvailability(result: LocationAvailability) {
//
//        }
//    }

//    private fun calculateRouteCallback(mapView: MapView, coreNavigation: CoreNavigation) =
//        CalculateRouteCallback { error, routes ->
//            error?.let {
//                Log.d(TAG, "failed to calculate route: ${error.name}")
//            } ?: run {
//                val route = routes?.first()
//                calculatedRoute.value = route
//                val color = mapView.context.getColor(R.color.blue_A700)
//                route?.addRoute(mapView.mapScene, Color.valueOf(color), 10)
//                route?.let { coreNavigation.setRoute(route) }
//            }
//        }

    private fun setMockAlertPoint(mapView: MapView, geoCoordinates: GeoCoordinates) {
        _mockAlertPoints.value.add(geoCoordinates)
        geoCoordinates.toWayPoint().addMarker(mapView, R.drawable.ic_pin)
    }

    private fun onSecurityBeltAlert() {
        _lastAlert.value = AlertType.SECURITY_BELT
        speak("Por favor verifique seu cinto de segurança.")
    }

    private fun onSmokingAlert() {
        _lastAlert.value = AlertType.SMOKING
        speak("Por favor evite fumar na direção.")
    }

    private fun onSpeedAlert() {
        _lastAlert.value = AlertType.SPEED
        speak("Por favor reduza a velocidade.")
    }

    private fun speak(text: String) {
        viewModelScope.launch {
            Handler(Looper.getMainLooper()).postDelayed({
                if (voiceAssistant?.textToSpeech?.isSpeaking == false) {
                    voiceAssistant?.textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }, 500)
        }
    }

    enum class AlertType {
        SECURITY_BELT, SMOKING, SPEED, NEAR_DESTINATION
    }
}
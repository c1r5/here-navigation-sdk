package br.com.herenavigatesdk.ui.viewmodels

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import br.com.herenavigatesdk.BaseApp.Companion.mockedRoutePoints
import br.com.herenavigatesdk.R
import br.com.herenavigatesdk.data.providers.RouteProviderImpl
import com.cire.herenavigation.core.CoreCamera
import com.cire.herenavigation.core.CoreNavigation
import com.cire.herenavigation.core.CoreRouting
import com.cire.herenavigation.core.CoreSDK
import com.cire.herenavigation.core.addRoute
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.OnSuccessListener
import com.here.sdk.core.Color
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.mapview.MapView
import com.here.sdk.routing.CalculateRouteCallback
import com.here.sdk.routing.Waypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapActivityViewModel : ViewModel() {
    private val mockedRoutePoints = mockedRoutePoints()
    private val routeProvider: RouteProviderImpl = RouteProviderImpl(mockedRoutePoints)
    private val waypoints = routeProvider.waypoints().toMutableList()
    private val geoCoordinates = MutableStateFlow<GeoCoordinates?>(null)

    private val icWaypoint = R.drawable.ic_waypoint
    private val icOrigin = R.drawable.ic_origin
    private val icDestination = R.drawable.ic_destination

    private var coreLoader: CoreLoader? = null

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating = _isNavigating.asStateFlow()

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

    init {
        CoreRouting.init()?.let {
            Log.d(TAG, "initializeRoutingError: $it")
            return@let
        }

        CoreNavigation.init()?.let {
            Log.d(TAG, "initializeNavigationError: $it")
            return@let
        }

        waypoints.add(0, routeProvider.origin())

    }


    fun oneTimeLocationListener(
        coreLoader: CoreLoader
    ) = OnSuccessListener<Location> { location ->
        geoCoordinates.value = GeoCoordinates(location.latitude, location.longitude)
        this.coreLoader = coreLoader

        val coreCamera = coreLoader.coreCamera!!
        val coreSDK = coreLoader.coreSDK!!
        val coreRouting = coreLoader.coreRouting!!
        val coreNavigation = coreLoader.coreNavigation!!

        routeProvider.waypoints = waypoints
        routeProvider.origin = Waypoint(geoCoordinates.value!!)
        viewModelScope.launch {
            coreCamera.recenter(geoCoordinates.value!!)
            coreSDK.indicator(geoCoordinates.value!!)
            coreSDK.showMarkers(routeProvider, icWaypoint, icOrigin, icDestination)
            coreRouting.calculateRoute(
                routeProvider,
                calculateRouteCallback(coreSDK.mapView, coreNavigation)
            )
        }
    }

    fun onRecenterButtonPressed() = geoCoordinates.value?.let {
        coreLoader?.coreCamera?.recenter(it)
    }

    fun onStartNavigationButtonPressed() {
        viewModelScope.launch {
            if (isNavigating.value) {
                coreLoader?.coreNavigation?.dispose()
                coreLoader?.coreCamera?.recenter(geoCoordinates.value!!)
                _isNavigating.emit(false)
            } else {
                coreLoader?.coreNavigation?.startNavigation()
                    ?.onSuccess {
                        _isNavigating.emit(true)
                    }?.onFailure { Log.d(TAG, "failed to start navigation: $it") }
            }
        }
    }

    fun onAudioToggleButtonPressed() {
        ttsState()?.let { isPaused ->
            if (isPaused) {
                coreLoader?.coreNavigation?.resumeTTS()
            } else {
                coreLoader?.coreNavigation?.pauseTTS()
            }
        }
    }

    fun ttsState() = coreLoader?.coreNavigation?.ttsState()

    fun onLocationChangedCallback() = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {

        }

        override fun onLocationAvailability(result: LocationAvailability) {

        }
    }

    private fun calculateRouteCallback(mapView: MapView, coreNavigation: CoreNavigation) =
        CalculateRouteCallback { error, routes ->
            error?.let {
                Log.d(TAG, "failed to calculate route: ${error.name}")
            } ?: run {
                val route = routes?.first()
                val color = mapView.context.getColor(R.color.blue_A700)
                route?.addRoute(mapView.mapScene, Color.valueOf(color), 10)
                route?.let { coreNavigation.setRoute(route) }
            }
        }
}
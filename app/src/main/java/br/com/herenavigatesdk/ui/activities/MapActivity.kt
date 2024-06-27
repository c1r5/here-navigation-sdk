package br.com.herenavigatesdk.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import br.com.herenavigatesdk.BaseApp.Companion.mockedRoutePoints
import br.com.herenavigatesdk.R
import br.com.herenavigatesdk.data.dtos.RoutePoint
import br.com.herenavigatesdk.data.providers.GeolocationProvider
import br.com.herenavigatesdk.data.providers.RouteProviderImpl
import br.com.herenavigatesdk.databinding.ActivityMapBinding
import br.com.herenavigatesdk.ui.viewmodels.MapActivityViewModel
import br.com.herenavigatesdk.usecase.hasLocationPermission
import br.com.herenavigatesdk.usecase.locationRequestCode
import br.com.herenavigatesdk.usecase.requestLocationPermission
import com.cire.herenavigation.core.CoreCamera
import com.cire.herenavigation.core.CoreNavigation
import com.cire.herenavigation.core.CoreRouting
import com.cire.herenavigation.core.CoreSDK
import com.cire.herenavigation.core.RouteProvider
import com.cire.herenavigation.core.addRoute
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.here.sdk.core.Color
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.mapview.MapView
import com.here.sdk.mapview.MapView.OnReadyListener
import com.here.sdk.routing.Waypoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class MapActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    private val activityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private val mapActivityViewModel by viewModels<MapActivityViewModel>()

    private lateinit var mapView: MapView
    private lateinit var fabStartNavigation: ExtendedFloatingActionButton
    private lateinit var fabRecenter: FloatingActionButton

    private lateinit var geolocationProvider: GeolocationProvider
    private lateinit var coreSDK: CoreSDK
    private lateinit var coreCamera: CoreCamera
    private lateinit var coreRouting: CoreRouting
    private lateinit var coreNavigation: CoreNavigation

    private lateinit var routeProvider: RouteProviderImpl

    private val icWaypoint = R.drawable.ic_waypoint
    private val icOrigin = R.drawable.ic_origin
    private val icDestination = R.drawable.ic_destination

    private val geoCoordinates = MutableStateFlow<GeoCoordinates?>(null)
    private val mockedRoutePoints = mockedRoutePoints()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMapBinding.root)

        mapView = activityMapBinding.mapview
        mapView.onCreate(savedInstanceState)

        fabStartNavigation = activityMapBinding.fabStartNavigation
        fabRecenter = activityMapBinding.fabRecenter

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }


        coreSDK = CoreSDK(mapView)
        coreCamera = CoreCamera(mapView)
        coreRouting = CoreRouting()
        coreNavigation = CoreNavigation(mapView)

        geolocationProvider = GeolocationProvider(this)
        routeProvider = RouteProviderImpl(mockedRoutePoints)

        val originalWaypoints = routeProvider.waypoints().toMutableList()
        originalWaypoints.add(0, routeProvider.origin())

        geolocationProvider.startLocationUpdates {
            it?.let {location ->
                geoCoordinates.value = CoreSDK.toGeoCoordinates(location.latitude, location.longitude)

                routeProvider.waypoints = originalWaypoints
                routeProvider.origin = Waypoint(geoCoordinates.value!!)

                coreCamera.recenter(geoCoordinates.value!!)
                coreSDK.indicator(geoCoordinates.value!!)

                coreSDK.showMarkers(routeProvider, icWaypoint, icOrigin, icDestination)
                coreRouting.calculateRoute(routeProvider) {error, routes ->
                    error?.let {
                        Log.d(TAG, "failed to calculate route: ${error.name}")
                    } ?: run {
                        val route = routes?.first()

                        route?.addRoute(mapView.mapScene, Color.valueOf(getColor(R.color.blue_A700)), 10)
                        route?.let { coreNavigation.setRoute(route) }
                    }
                }
            }
        }

        geolocationProvider.onLocationChanged(mapActivityViewModel.onLocationCallback(coreSDK))

        CoroutineScope(Dispatchers.IO).launch {
            coreSDK.onCoreError.collect {
                Log.d(TAG, "onCoreError: $it")
            }
        }

        fabRecenter.setOnClickListener { coreCamera.recenter(geoCoordinates.value!!) }

        fabStartNavigation.setOnClickListener {
            if (mapActivityViewModel.isNavigating.value) {
                coreNavigation.dispose()
                mapActivityViewModel.isNavigating.value = false
                activityMapBinding.fabStartNavigation.text = "INICIAR NAVEGAÇÃO"
            } else {
                coreNavigation.startNavigation()
                    .onSuccess {
                        mapActivityViewModel.isNavigating.value = true
                        activityMapBinding.fabStartNavigation.text = "PARAR NAVEGAÇÃO"
                    }.onFailure {
                        Log.d(TAG, "failed to start navigation: $it")
                    }

            }
        }

    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onResume() {
        mapView.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        CoreSDK.dispose()
        coreNavigation.dispose()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode && !hasLocationPermission()) {
            showPermissionDeniedDialog()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Location permission is required to use this app.")
            .setPositiveButton("OK") { _, _ -> requestLocationPermission() }
            .setOnCancelListener { finish() }
            .show()
    }
}
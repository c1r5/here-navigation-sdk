package br.com.herenavigatesdk.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import br.com.herenavigatesdk.BaseApp
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import br.com.herenavigatesdk.data.providers.GeolocationProvider
import br.com.herenavigatesdk.data.providers.RouteProvider
import br.com.herenavigatesdk.databinding.ActivityMapBinding
import br.com.herenavigatesdk.usecase.hasLocationPermission
import br.com.herenavigatesdk.usecase.locationRequestCode
import br.com.herenavigatesdk.usecase.requestLocationPermission
import com.cire.herenavigation.core.HereSDKModule
import com.cire.herenavigation.core.RouteCalculator
import com.cire.herenavigation.core.RouteNavigation
import com.here.sdk.mapview.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MapActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    private val activityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private lateinit var mapView: MapView
    private lateinit var hereSDKModule: HereSDKModule
    private lateinit var geolocationProvider: GeolocationProvider
    private lateinit var routeProvider: RouteProvider
    private lateinit var routeNavigation: RouteNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMapBinding.root)
        mapView = activityMapBinding.mapview
        mapView.onCreate(savedInstanceState)

        hereSDKModule = HereSDKModule(mapView)
        geolocationProvider = GeolocationProvider(this)
        routeProvider = RouteProvider(BaseApp.mockedRoutePoints()!!)
        routeNavigation = RouteNavigation(mapView)

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }

        CoroutineScope(Dispatchers.IO).launch {
            GeolocationProvider.getLastLocation.collect {
                it?.let {
                    val geoCoordinates = HereSDKModule.latlngToGeoCoordinates(
                        it.latitude,
                        it.longitude
                    )
                    hereSDKModule.onLocationUpdated(geoCoordinates)
                }
            }
        }

        activityMapBinding.fabRecenter.setOnClickListener {
            hereSDKModule.recenter()
        }

        activityMapBinding.fabStartNavigation.setOnClickListener {
            routeNavigation.startNavigation {
                Log.d(TAG, "startNavigationError: $it")
            }
        }

        routeNavigation.initVisualNavigator {
            Log.d(TAG, "initVisualNavigatorError: $it")
        }
        RouteCalculator.initRoutingEngine { exception ->
            exception?.printStackTrace()
        }

        RouteCalculator.calculateRoute(
            routeProvider.origin(),
            routeProvider.destination(),
            routeProvider.waypoints(),
        ) { error, routes ->
            error?.let {
                Log.d(TAG, "failed to calculate route: ${it.name}")
            } ?: run {
                routes?.first()?.let {
                    Log.d(TAG, "calculateRoute: $it")
                    hereSDKModule.addRoute(it)
                    routeNavigation.setRoute(it)
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
        HereSDKModule.dispose()
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
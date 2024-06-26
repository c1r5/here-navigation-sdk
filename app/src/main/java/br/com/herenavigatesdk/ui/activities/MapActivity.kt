package br.com.herenavigatesdk.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import br.com.herenavigatesdk.databinding.ActivityMapBinding
import br.com.herenavigatesdk.providers.GeolocationProvider
import br.com.herenavigatesdk.ui.heremodule.HereSDKModule
import br.com.herenavigatesdk.usecase.hasLocationPermission
import br.com.herenavigatesdk.usecase.locationRequestCode
import br.com.herenavigatesdk.usecase.requestLocationPermission
import com.here.sdk.mapview.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MapActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    private val activityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private lateinit var mapView: MapView
    private lateinit var hereSDKModule: HereSDKModule
    private lateinit var geolocationProvider: GeolocationProvider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMapBinding.root)
        mapView = activityMapBinding.mapview
        mapView.onCreate(savedInstanceState)

        hereSDKModule = HereSDKModule(mapView)
        geolocationProvider = GeolocationProvider(this)

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }

        CoroutineScope(Dispatchers.IO).launch {
            GeolocationProvider.getLastLocation.collect {
                it?.let {
                    val geoCoordinates =
                        HereSDKModule.latlngToGeoCoordinates(it.latitude, it.longitude)
                    hereSDKModule.onLocationUpdated(geoCoordinates)
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
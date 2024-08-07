package br.com.herenavigatesdk.ui.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import br.com.herenavigatesdk.BaseApp
import br.com.herenavigatesdk.BaseApp.Companion
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import br.com.herenavigatesdk.BaseApp.Companion.mockedRoutePoints
import br.com.herenavigatesdk.R
import br.com.herenavigatesdk.data.providers.GeolocationProvider
import br.com.herenavigatesdk.data.providers.RouteProviderImpl
import br.com.herenavigatesdk.databinding.ActivityMapBinding
import br.com.herenavigatesdk.ui.viewmodels.MapActivityViewModel
import br.com.herenavigatesdk.ui.viewmodels.MapActivityViewModel.Companion.CoreLoader
import br.com.herenavigatesdk.usecase.hasLocationPermission
import com.cire.herenavigation.audio.VoiceAssistant
import com.cire.herenavigation.helper.PermissionsRequestor
import com.cire.herenavigation.core.CoreCamera
import com.cire.herenavigation.core.CoreNavigation
import com.cire.herenavigation.core.CoreRouting
import com.cire.herenavigation.core.CoreSDK
import com.cire.herenavigation.provider.HEREPositioningProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.here.posclient.ConsentState
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.mapview.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class MapActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    private val activityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private val mapActivityViewModel by viewModels<MapActivityViewModel>()

    private lateinit var mapView: MapView
    private lateinit var fabStartNavigation: ExtendedFloatingActionButton
    private lateinit var fabRecenter: FloatingActionButton
    private lateinit var fabToggleAudio: FloatingActionButton

    private lateinit var coreSDK: CoreSDK
    private lateinit var coreCamera: CoreCamera
    private lateinit var coreNavigation: CoreNavigation
    private lateinit var permissionsRequestor: PermissionsRequestor
    private lateinit var herePositioningProvider: HEREPositioningProvider

    private var lastKnownLocation = MutableStateFlow<GeoCoordinates?>(null)

    private val icWaypoint = R.drawable.ic_waypoint
    private val icOrigin = R.drawable.ic_origin
    private val icDestination = R.drawable.ic_destination

    private val mockedRoutePoints = mockedRoutePoints()
    private val routeProvider: RouteProviderImpl = RouteProviderImpl(mockedRoutePoints)
    private val waypoints = routeProvider.waypoints().toMutableList()

    init {
        waypoints.add(0, routeProvider.origin())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!CoreSDK.isInitialized) {
            return showNotInitializedDialog()
        }

        CoreRouting.init()?.let {
            Log.d(TAG, "initializeRoutingError: $it")
        }

        CoreNavigation.init()?.let {
            Log.d(TAG, "initializeNavigationError: $it")
        }

        setContentView(activityMapBinding.root)

        mapView = activityMapBinding.mapview
        mapView.onCreate(savedInstanceState)

        fabStartNavigation = activityMapBinding.fabStartNavigation
        fabRecenter = activityMapBinding.fabRecenter
        fabToggleAudio = activityMapBinding.fabToggleAudio

        permissionsRequestor = PermissionsRequestor(this)
        herePositioningProvider = HEREPositioningProvider()
        lastKnownLocation.value = herePositioningProvider.lastKnownLocation?.coordinates

        coreSDK = CoreSDK(mapView).apply { loadScene() }

        coreNavigation = CoreNavigation(mapView)
        coreCamera = CoreCamera(mapView)

        centerInLocation()

        fabRecenter.setOnClickListener {
            lastKnownLocation.value?.let { geoCoordinates -> coreCamera.recenter(geoCoordinates, mapActivityViewModel.userZoomDistance.value.toDouble())}
        }

        onLocationState {
            fabStartNavigation.isEnabled = it
            fabToggleAudio.isEnabled = it
            fabRecenter.isEnabled = it
        }

        coreSDK.onCoreError {
            Log.d(TAG, "coreError: $it")
        }

        if(hasLocationPermission()) {
            return startPositioning()
        }

        requestLocationPermission()
    }

    private fun onLocationState(state: (Boolean) -> Unit) {
        MainScope().launch {
            lastKnownLocation.collect {
                state(it != null)
            }
        }
    }

    private fun centerInLocation() {
        Handler(Looper.getMainLooper()).postDelayed({
            lastKnownLocation.value?.let { geoCoordinates ->
                coreSDK.showRoute(routeProvider, R.color.blue_A700, 10)
                coreSDK.indicator(geoCoordinates)
                coreSDK.showMarkers(routeProvider, icWaypoint, icOrigin, icDestination)
                coreCamera.recenter(geoCoordinates)
            }
        }, 1000)
    }
    private fun requestLocationPermission() {
        permissionsRequestor.request(object : PermissionsRequestor.ResultListener {
            override fun permissionsGranted() {
                startPositioning()
            }

            override fun permissionsDenied() {
                showPermissionDeniedDialog {
                    requestLocationPermission()
                }
            }
        })
    }
    private fun startPositioning() {
        MainScope().launch {
            mapActivityViewModel.hereLocationAccuracy.collect {accuracy ->
                herePositioningProvider.stopLocating()

                Handler(Looper.getMainLooper()).postDelayed({
                    herePositioningProvider.startLocating(
                        mapActivityViewModel.hereLocationListener(mapView, coreSDK),
                        accuracy
                    )
                }, 500)
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
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun showPermissionDeniedDialog(onPositiveClick: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Permissão negada!")
            .setMessage("Permissão de localização é obrigatória para esta aplicação. Caso você não aceite, a aplicação será finalizada.")
            .setPositiveButton("OK") { _, _ -> onPositiveClick() }
                .setNegativeButton("Não") { _, _ -> finish() }
                    .setOnCancelListener { finish() }
            .show()
    }

    private fun showNotInitializedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Erro no mapa")
            .setMessage("Erro ao carregar mapa!")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setOnCancelListener { finish() }
    }


}
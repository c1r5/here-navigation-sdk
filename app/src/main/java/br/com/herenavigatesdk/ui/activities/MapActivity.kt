package br.com.herenavigatesdk.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import br.com.herenavigatesdk.R
import br.com.herenavigatesdk.data.providers.GeolocationProvider
import br.com.herenavigatesdk.databinding.ActivityMapBinding
import br.com.herenavigatesdk.ui.viewmodels.MapActivityViewModel
import br.com.herenavigatesdk.ui.viewmodels.MapActivityViewModel.Companion.CoreLoader
import br.com.herenavigatesdk.usecase.hasLocationPermission
import com.cire.herenavigation.audio.PermissionsRequestor
import com.cire.herenavigation.core.CoreCamera
import com.cire.herenavigation.core.CoreNavigation
import com.cire.herenavigation.core.CoreRouting
import com.cire.herenavigation.core.CoreSDK
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.here.sdk.mapview.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class MapActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    private val activityMapBinding by lazy { ActivityMapBinding.inflate(layoutInflater) }
    private val mapActivityViewModel by viewModels<MapActivityViewModel>()

    private lateinit var mapView: MapView
    private lateinit var fabStartNavigation: ExtendedFloatingActionButton
    private lateinit var fabRecenter: FloatingActionButton
    private lateinit var fabToggleAudio: FloatingActionButton
    private lateinit var geolocationProvider: GeolocationProvider
    private lateinit var coreSDK: CoreSDK
    private lateinit var coreCamera: CoreCamera
    private lateinit var coreRouting: CoreRouting
    private lateinit var coreNavigation: CoreNavigation
    private lateinit var coreLoader: CoreLoader
    private lateinit var permissionsRequestor: PermissionsRequestor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!CoreSDK.isInitialized) {
            return showNotInitializedDialog()
        }

        setContentView(activityMapBinding.root)
        mapView = activityMapBinding.mapview
        mapView.onCreate(savedInstanceState)

        fabStartNavigation = activityMapBinding.fabStartNavigation
        fabRecenter = activityMapBinding.fabRecenter
        fabToggleAudio = activityMapBinding.fabToggleAudio

        permissionsRequestor = PermissionsRequestor(this)
        geolocationProvider = GeolocationProvider(this)

        coreSDK = CoreSDK(mapView).apply { loadScene() }
        coreNavigation = CoreNavigation(mapView)
        coreRouting = CoreRouting()
        coreCamera = CoreCamera(mapView)
        coreLoader = CoreLoader.Builder()
            .coreCamera(coreCamera)
            .coreRouting(coreRouting)
            .coreNavigation(coreNavigation)
            .coreSDK(coreSDK)
            .build()

        if (!hasLocationPermission()) {
            permissionsRequestor.request(object : PermissionsRequestor.ResultListener {
                override fun permissionsGranted() {
                    geolocationProvider.oneTimeLocation(
                        mapActivityViewModel.oneTimeLocationListener(
                            coreLoader
                        )
                    )
                }

                override fun permissionsDenied() {
                    showPermissionDeniedDialog()
                }
            })
        }

        fabRecenter.setOnClickListener {
            mapActivityViewModel.onRecenterButtonPressed()
        }

        fabStartNavigation.setOnClickListener {
            mapActivityViewModel.onStartNavigationButtonPressed()
        }

        fabToggleAudio.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                mapActivityViewModel.onAudioToggleButtonPressed()
                mapActivityViewModel.ttsState()?.let {
                    if (it) {
                        fabToggleAudio.setImageResource(R.drawable.ic_audio_off)
                    } else {
                        fabToggleAudio.setImageResource(R.drawable.ic_audio_on)
                    }
                }
            }
        }

        MainScope().launch {
            mapActivityViewModel.isNavigating.collect {
                if (it) {
                    fabStartNavigation.text = getString(R.string.stop_navigation)
                } else {
                    fabStartNavigation.text = getString(R.string.start_navigation)
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            coreSDK.onCoreError.collect {
                Log.d(TAG, "onCoreError: $it")
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

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissão negada!")
            .setMessage("Permissão de localização é obrigatória para esta aplicação. Caso você não aceite, a aplicação será finalizada.")
            .setPositiveButton("OK") { _, _ -> finish() }
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
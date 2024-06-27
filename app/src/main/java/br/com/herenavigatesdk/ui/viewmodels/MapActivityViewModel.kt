package br.com.herenavigatesdk.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.herenavigatesdk.BaseApp.Companion.TAG
import com.cire.herenavigation.core.CoreNavigation
import com.cire.herenavigation.core.CoreRouting
import com.cire.herenavigation.core.CoreSDK
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapActivityViewModel: ViewModel() {
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating = _isNavigating

    init {
        CoreRouting.init()?.let {
            Log.d(TAG, "initializeRoutingError: $it")
            return@let
        }

        CoreNavigation.init()?.let {
            Log.d(TAG, "initializeNavigationError: $it")
            return@let
        }
    }

    fun onLocationCallback(coreSDK: CoreSDK) = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {location ->
                coreSDK.indicator(CoreSDK.toGeoCoordinates(location.latitude, location.longitude))
            }
        }

        override fun onLocationAvailability(p0: LocationAvailability) {

        }
    }
}
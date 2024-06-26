package br.com.herenavigatesdk.data.providers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class GeolocationProvider(private val context: Context) {
    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private val _getLastLocation = MutableStateFlow<Location?>(null)

        @JvmStatic
        val getLastLocation = _getLastLocation.asStateFlow()
    }

    init {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            _getLastLocation.tryEmit(it)
        }
    }
}
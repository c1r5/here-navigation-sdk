package br.com.herenavigatesdk.data.providers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class GeolocationProvider(private val context: Context) {
    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = LocationRequest.Builder(1000).setPriority(
        Priority.PRIORITY_HIGH_ACCURACY
    ).build()

    private var locationCallback: LocationCallback? = null

    fun startLocationUpdates(onLastLocationReceived: (Location?) -> Unit) {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            onLastLocationReceived(it)
        }
    }

    fun onLocationChanged(locationCallback: LocationCallback) {
        this.locationCallback = locationCallback
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
    }
}
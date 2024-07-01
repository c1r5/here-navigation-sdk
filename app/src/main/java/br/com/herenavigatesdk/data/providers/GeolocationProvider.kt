package br.com.herenavigatesdk.data.providers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnSuccessListener

@SuppressLint("MissingPermission")
class GeolocationProvider(private val context: Context) {
    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = LocationRequest.Builder(1000).setPriority(
        Priority.PRIORITY_HIGH_ACCURACY
    ).build()

    private var locationCallback: LocationCallback? = null

    fun oneTimeLocation(onSuccessListener: OnSuccessListener<Location>) {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener(
            (context as Activity),
            onSuccessListener
        )
    }

    fun onLocationChanged(locationCallback: LocationCallback) {
        this.locationCallback = locationCallback
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
    }
}
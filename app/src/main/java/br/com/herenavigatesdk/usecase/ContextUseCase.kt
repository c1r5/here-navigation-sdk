package br.com.herenavigatesdk.usecase

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import br.com.herenavigatesdk.BaseApp

val permissions = BaseApp.permissions
val locationRequestCode = BaseApp.LOCATION_PERMISSION_REQUEST_CODE

fun Context.requestLocationPermission() {
    ActivityCompat.requestPermissions(this as Activity, permissions, locationRequestCode)
}

fun Context.hasLocationPermission(): Boolean =
    permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
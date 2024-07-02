package br.com.herenavigatesdk

import android.Manifest
import android.app.Application
import br.com.herenavigatesdk.data.dtos.RoutePoint
import com.cire.herenavigation.core.CoreSDK
import com.google.gson.Gson

class BaseApp : Application() {


    companion object {
        private var mockedRoutePoints: List<RoutePoint> = emptyList()

        @JvmStatic
        val TAG = "HERENAVIGATESDK"

        @JvmStatic
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        @JvmStatic
        val LOCATION_PERMISSION_REQUEST_CODE = 100

        @JvmStatic
        fun gson() = Gson()

        @JvmStatic
        fun mockedRoutePoints() = mockedRoutePoints
    }

    override fun onCreate() {
        super.onCreate()
        mockedRoutePoints = assets.open("waypoints.json").bufferedReader().use {
            gson().fromJson(it, Array<RoutePoint>::class.java).toList()
        }
        CoreSDK.init(
            this,
            getString(R.string.HERE_ACCESS_KEY_ID),
            getString(R.string.HERE_ACCESS_KEY_SECRET)
        )
    }
}
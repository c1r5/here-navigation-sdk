package br.com.herenavigatesdk

import android.Manifest
import android.app.Application
import br.com.herenavigatesdk.ui.heremodule.HereSDKModule

class BaseApp : Application() {

    companion object {
        @JvmStatic
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        @JvmStatic
        val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate() {
        super.onCreate()
        HereSDKModule.initialize(this)


    }


}
package com.cire.herenavigation.core

import android.speech.tts.TextToSpeech
import com.cire.herenavigation.audio.LanguageCodeConverter
import com.cire.herenavigation.audio.VoiceAssistant
import com.cire.herenavigation.provider.HEREPositioningProvider
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.LanguageCode
import com.here.sdk.core.Location
import com.here.sdk.core.LocationListener
import com.here.sdk.core.UnitSystem
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.MapView
import com.here.sdk.navigation.DestinationReachedListener
import com.here.sdk.navigation.EventTextListener
import com.here.sdk.navigation.EventTextOptions
import com.here.sdk.navigation.LocationSimulator
import com.here.sdk.navigation.LocationSimulatorOptions
import com.here.sdk.navigation.ManeuverNotificationOptions
import com.here.sdk.navigation.MilestoneStatusListener
import com.here.sdk.navigation.VisualNavigator
import com.here.sdk.routing.Route
import com.here.services.location.inject.InjectLocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class CoreNavigation(private val mapView: MapView): LocationListener {
    private var languageCode: LanguageCode = LanguageCode.PT_BR
    private var calculatedRoute: Route? = null
    private var voiceAssistant = VoiceAssistant(mapView.context)
    private var ttsIsPaused = MutableStateFlow(false)
    private var onMilestoneStatusListener: MilestoneStatusListener? = null
    private var onDestinationReachedListener: DestinationReachedListener? = null

    private var _coreNavigationError = MutableStateFlow<Throwable?>(null)
    val coreNavigationError = _coreNavigationError.asStateFlow()

    fun onCoreNavigationError(error: (Throwable?) -> Unit) = CoroutineScope(Dispatchers.Main).launch {
        _coreNavigationError.collect {
            it?.let { error(it) }
        }
    }

    companion object {
        private var visualNavigator: VisualNavigator? = null

        fun init(): InstantiationErrorException? {
            return try {
                visualNavigator = VisualNavigator()
                null
            } catch (e: InstantiationErrorException) {
                e
            }
        }
    }

    fun pauseTTS() {
        ttsIsPaused.value = true
    }

    fun resumeTTS() {
        ttsIsPaused.value = false
    }

    fun ttsState() = ttsIsPaused.value

    fun setRoute(route: Route) = apply { calculatedRoute = route }

    fun setVoiceLanguage(languageCode: LanguageCode) = apply {
        this.languageCode = languageCode
    }

    fun setMilestoneStatusListener (listener: MilestoneStatusListener) = apply {
        onMilestoneStatusListener = listener

    }
    fun setDestinationReachedListener (listener: DestinationReachedListener) = apply {
        onDestinationReachedListener = listener
    }

    fun startNavigation(
        locationProvider: HEREPositioningProvider? = null,
    ) = apply {
        voiceAssistant.setLanguage(LanguageCodeConverter.getLocale(languageCode))
        visualNavigator?.apply {
            startRendering(mapView)

            onMilestoneStatusListener?.let {
                milestoneStatusListener = it
            }
            onDestinationReachedListener?.let {
                destinationReachedListener = it
            }

            route = calculatedRoute
            maneuverNotificationOptions = ManeuverNotificationOptions(languageCode, UnitSystem.METRIC)
            eventTextOptions = EventTextOptions().apply { enableSpatialAudio = true }
            eventTextListener = EventTextListener { eventText ->
                val text = eventText.text
                if (!ttsIsPaused.value && !voiceAssistant.textToSpeech.isSpeaking) {
                    voiceAssistant.textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            locationProvider?.startMockLocating(this, calculatedRoute)

        } ?: run {
            _coreNavigationError.tryEmit(Throwable("Visual navigator is not initialized."))
        }
    }


    fun dispose() = apply {
        visualNavigator?.stopRendering()
        voiceAssistant.textToSpeech.stop()
    }

    override fun onLocationUpdated(p0: Location) {
        visualNavigator?.onLocationUpdated(p0)
    }
}
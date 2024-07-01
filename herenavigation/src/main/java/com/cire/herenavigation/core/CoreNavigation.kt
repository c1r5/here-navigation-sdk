package com.cire.herenavigation.core

import android.speech.tts.TextToSpeech
import com.cire.herenavigation.audio.LanguageCodeConverter
import com.cire.herenavigation.audio.VoiceAssistant
import com.here.sdk.core.LanguageCode
import com.here.sdk.core.LocationListener
import com.here.sdk.core.UnitSystem
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.MapView
import com.here.sdk.navigation.EventTextListener
import com.here.sdk.navigation.EventTextOptions
import com.here.sdk.navigation.LocationSimulator
import com.here.sdk.navigation.LocationSimulatorOptions
import com.here.sdk.navigation.ManeuverNotificationOptions
import com.here.sdk.navigation.VisualNavigator
import com.here.sdk.routing.Route
import kotlinx.coroutines.flow.MutableStateFlow


class CoreNavigation(private val mapView: MapView) {
    private var languageCode: LanguageCode = LanguageCode.PT_BR
    private var calculatedRoute: Route? = null
    private var voiceAssistant = VoiceAssistant(mapView.context)
    private var ttsIsPaused = MutableStateFlow(false)

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

    fun startNavigation(): Result<Unit> {
        voiceAssistant.setLanguage(LanguageCodeConverter.getLocale(languageCode))
        visualNavigator?.apply {
            route = calculatedRoute
            maneuverNotificationOptions =
                ManeuverNotificationOptions(languageCode, UnitSystem.METRIC)
            eventTextOptions = EventTextOptions().apply { enableSpatialAudio = true }
            eventTextListener = EventTextListener { eventText ->
                val text = eventText.text
                if (!ttsIsPaused.value) {
                    voiceAssistant.textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            setupLocationSource(this)
            startRendering(mapView)
        }?.let {
            return Result.success(Unit)
        } ?: run {
            return Result.failure(RuntimeException("VisualNavigator is null"))
        }
    }


    fun dispose() = apply {
        visualNavigator?.stopRendering()
        if (voiceAssistant.textToSpeech.isSpeaking) {
            voiceAssistant.textToSpeech.stop()
        }
    }

    private fun setupLocationSource(locationListener: LocationListener) {
        calculatedRoute?.let {
            LocationSimulator(it, LocationSimulatorOptions()).apply {
                listener = locationListener
                start()
            }
        }
    }


}
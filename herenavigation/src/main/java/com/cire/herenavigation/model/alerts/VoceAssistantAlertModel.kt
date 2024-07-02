package com.cire.herenavigation.model.alerts

import com.cire.herenavigation.audio.VoiceAssistant

interface VoceAssistantAlertModel {
    var voiceAssistant: VoiceAssistant?
    fun onSecurityBeltAlert()
    fun onSmokingAlert()
    fun onSpeedAlert()
    fun onRouteDeviationAlert()
}
package com.cire.herenavigation.core

import com.here.sdk.routing.Waypoint

interface RouteProvider {
    fun origin(): Waypoint
    fun destination(): Waypoint
    fun waypoints(): List<Waypoint>
}
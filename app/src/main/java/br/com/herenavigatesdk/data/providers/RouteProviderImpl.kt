package br.com.herenavigatesdk.data.providers

import br.com.herenavigatesdk.data.dtos.RoutePoint
import com.cire.herenavigation.core.CoreSDK
import com.cire.herenavigation.core.RouteProvider
import com.here.sdk.routing.Waypoint

class RouteProviderImpl(private val routePoints: List<RoutePoint>): RouteProvider {
    private val _origin = Waypoint(CoreSDK.toGeoCoordinates(routePoints.first().lat, routePoints.first().lng))
    private val _destination = Waypoint(CoreSDK.toGeoCoordinates(routePoints.last().lat, routePoints.last().lng))
    private val _waypoints = routePoints.drop(1).dropLast(1).map {
        Waypoint(CoreSDK.toGeoCoordinates(it.lat, it.lng))
    }

    var mockedRoutePoints = routePoints

    var origin = _origin
    var destination = _destination
    var waypoints = _waypoints

    override fun origin(): Waypoint {
        return origin
    }

    override fun destination(): Waypoint {
        return destination
    }

    override fun waypoints(): List<Waypoint> {
        return waypoints
    }
}
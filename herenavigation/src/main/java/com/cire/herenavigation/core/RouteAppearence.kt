package com.cire.herenavigation.core

import com.here.sdk.core.Color

class RouteAppearence(
    val color: Color,
    val width: Int,
) {
    companion object {
        val DEFAULT = RouteAppearence(Color.valueOf(android.graphics.Color.BLUE), 5)
    }

    override fun toString(): String {
        return "RouteAppearence(color=$color, width=$width)"
    }

    data class Builder(
        var color: Color = DEFAULT.color,
        var width: Int = DEFAULT.width
    ) {
        fun color(color: Color) = apply { this.color = color }
        fun width(width: Int) = apply { this.width = width }
        fun build() = RouteAppearence(color, width)
    }

}
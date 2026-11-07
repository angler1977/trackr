package com.trackr.ui.components

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.trackr.domain.model.BusLineDetail
import com.trackr.domain.model.BusStop

/**
 * Route overview on an actual AMap map: station pins, route polyline and live bus pins.
 * Coordinates in the app model are WGS-84; AMap overlays require GCJ-02.
 */
@Composable
fun BusLineMapView(
    detail: BusLineDetail,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    val amap = remember(mapView) { mapView.map }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxWidth().height(280.dp),
        update = { view ->
            val map = view.map
            map.clear()
            val validStops = detail.stops.filter { it.wgsLat in -90.0..90.0 && it.wgsLng in -180.0..180.0 && (it.wgsLat != 0.0 || it.wgsLng != 0.0) }
            val routePoints = validStops.map { it.toGcjLatLng() }
            if (routePoints.size >= 2) {
                map.addPolyline(PolylineOptions().addAll(routePoints).width(8f))
            }
            validStops.forEach { stop ->
                map.addMarker(MarkerOptions().position(stop.toGcjLatLng()).title("${stop.order}. ${stop.name}"))
            }
            detail.buses.filter { it.wgsLat != 0.0 || it.wgsLng != 0.0 }
                .filter { it.wgsLat in -90.0..90.0 && it.wgsLng in -180.0..180.0 }
                .forEach { bus ->
                    map.addMarker(MarkerOptions().position(bus.toGcjLatLng()).title(bus.friendlyPlate).snippet("实时公交"))
                }
            routePoints.firstOrNull()?.let { map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 13.5f)) }
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true
        }
    )
}

private fun BusStop.toGcjLatLng(): LatLng = wgs84ToGcj02(wgsLat, wgsLng)
private fun com.trackr.domain.model.LiveBus.toGcjLatLng(): LatLng = wgs84ToGcj02(wgsLat, wgsLng)

/** Standard mainland-China WGS-84 -> GCJ-02 transform. Outside China coordinates pass through. */
private fun wgs84ToGcj02(lat: Double, lng: Double): LatLng {
    if (lng !in 72.004..137.8347 || lat !in 0.8293..55.8271) return LatLng(lat, lng)
    val a = 6378245.0
    val ee = 0.00669342162296594323
    fun transformLat(x: Double, y: Double): Double {
        var v = -100.0 + 2.0*x + 3.0*y + 0.2*y*y + 0.1*x*y + 0.2*kotlin.math.sqrt(kotlin.math.abs(x))
        v += (20.0*kotlin.math.sin(6.0*x*kotlin.math.PI) + 20.0*kotlin.math.sin(2.0*x*kotlin.math.PI))*2.0/3.0
        v += (20.0*kotlin.math.sin(y*kotlin.math.PI) + 40.0*kotlin.math.sin(y/3.0*kotlin.math.PI))*2.0/3.0
        v += (160.0*kotlin.math.sin(y/12.0*kotlin.math.PI) + 320.0*kotlin.math.sin(y*kotlin.math.PI/30.0))*2.0/3.0
        return v
    }
    fun transformLng(x: Double, y: Double): Double {
        var v = 300.0 + x + 2.0*y + 0.1*x*x + 0.1*x*y + 0.1*kotlin.math.sqrt(kotlin.math.abs(x))
        v += (20.0*kotlin.math.sin(6.0*x*kotlin.math.PI) + 20.0*kotlin.math.sin(2.0*x*kotlin.math.PI))*2.0/3.0
        v += (20.0*kotlin.math.sin(x*kotlin.math.PI) + 40.0*kotlin.math.sin(x/3.0*kotlin.math.PI))*2.0/3.0
        v += (150.0*kotlin.math.sin(x/12.0*kotlin.math.PI) + 300.0*kotlin.math.sin(x/30.0*kotlin.math.PI))*2.0/3.0
        return v
    }
    val dLat = transformLat(lng-105.0, lat-35.0)
    val dLng = transformLng(lng-105.0, lat-35.0)
    val radLat = lat/180.0*kotlin.math.PI
    val magic = 1.0-ee*kotlin.math.sin(radLat)*kotlin.math.sin(radLat)
    val sqrtMagic = kotlin.math.sqrt(magic)
    val mgLat = lat + dLat*180.0/((a*(1.0-ee))/(magic*sqrtMagic)*kotlin.math.PI)
    val mgLng = lng + dLng*180.0/(a/sqrtMagic*kotlin.math.cos(radLat)*kotlin.math.PI)
    return LatLng(mgLat, mgLng)
}

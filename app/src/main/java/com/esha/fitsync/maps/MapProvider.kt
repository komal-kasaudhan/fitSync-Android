package com.esha.fitsync.maps

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.esha.fitsync.R
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

interface MapProvider {
    fun createMapView(context: Context): View
    fun setupMap(view: View, lat: Double, lng: Double, zoom: Double = 15.0, isInteractive: Boolean = true)
    fun setCenter(view: View, lat: Double, lng: Double, zoom: Double? = null)
    fun getCenter(view: View): Pair<Double, Double>
    fun addMarker(
        view: View,
        id: String,
        lat: Double,
        lng: Double,
        title: String,
        snippet: String? = null,
        icon: Drawable? = null,
        onClick: (() -> Unit)? = null
    )
    fun clearMarkers(view: View)
    fun setOnCenterChangedListener(view: View, onCenterChanged: (lat: Double, lng: Double) -> Unit)
    fun onResume(view: View)
    fun onPause(view: View)
    fun onDetach(view: View)
}

object OsmMapProvider : MapProvider {

    override fun createMapView(context: Context): View {
        val mapView = MapView(context)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapView.setMultiTouchControls(true)
        return mapView
    }

    override fun setupMap(view: View, lat: Double, lng: Double, zoom: Double, isInteractive: Boolean) {
        val mapView = view as? MapView ?: return
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(isInteractive)

        if (!isInteractive) {
            mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            mapView.setOnTouchListener { _, event ->
                // Disable map dragging/scrolling on static preview cards
                if (event.action == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
                true
            }
        }

        val center = GeoPoint(lat, lng)
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(center)
    }

    override fun setCenter(view: View, lat: Double, lng: Double, zoom: Double?) {
        val mapView = view as? MapView ?: return
        if (zoom != null) {
            mapView.controller.setZoom(zoom)
        }
        mapView.controller.animateTo(GeoPoint(lat, lng))
    }

    override fun getCenter(view: View): Pair<Double, Double> {
        val mapView = view as? MapView ?: return Pair(0.0, 0.0)
        val center = mapView.mapCenter
        return Pair(center.latitude, center.longitude)
    }

    override fun addMarker(
        view: View,
        id: String,
        lat: Double,
        lng: Double,
        title: String,
        snippet: String?,
        icon: Drawable?,
        onClick: (() -> Unit)?
    ) {
        val mapView = view as? MapView ?: return
        val marker = Marker(mapView)
        marker.id = id
        marker.position = GeoPoint(lat, lng)
        marker.title = title
        marker.snippet = snippet
        if (icon != null) {
            marker.icon = icon
        } else {
            val defaultIcon = ContextCompat.getDrawable(view.context, R.drawable.ic_location)
            if (defaultIcon != null) marker.icon = defaultIcon
        }

        if (onClick != null) {
            marker.setOnMarkerClickListener { _, _ ->
                onClick()
                true
            }
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    override fun clearMarkers(view: View) {
        val mapView = view as? MapView ?: return
        mapView.overlays.removeAll { it is Marker }
        mapView.invalidate()
    }

    override fun setOnCenterChangedListener(view: View, onCenterChanged: (lat: Double, lng: Double) -> Unit) {
        val mapView = view as? MapView ?: return
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val center = mapView.mapCenter
                onCenterChanged(center.latitude, center.longitude)
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val center = mapView.mapCenter
                onCenterChanged(center.latitude, center.longitude)
                return false
            }
        })
    }

    override fun onResume(view: View) {
        (view as? MapView)?.onResume()
    }

    override fun onPause(view: View) {
        (view as? MapView)?.onPause()
    }

    override fun onDetach(view: View) {
        (view as? MapView)?.onDetach()
    }
}

package com.dinofit.app.feature.walk

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dinofit.app.core.designsystem.DinoColors
import com.dinofit.app.core.designsystem.DinoType
import com.dinofit.app.core.model.LatLng
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

/**
 * Carte OpenStreetMap avec le trajet en orange, un point vert au départ et un point orange à l'arrivée.
 * [follow] = true pendant une marche : la carte suit la dernière position.
 */
@Composable
fun RouteMap(points: List<LatLng>, follow: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val shape = RoundedCornerShape(22.dp)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            controller.setZoom(16.5)
            // Glisser sur la carte déplace la carte, pas la page.
            setOnTouchListener { v, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN) v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }
    val route = remember {
        Polyline(mapView).apply {
            outlinePaint.apply {
                color = DinoColors.Amber.toArgb()
                strokeWidth = 14f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
        }
    }
    val ends = remember { EndpointsOverlay() }
    val state = remember { MapState() }

    // La carte suit le cycle de vie de l'écran (téléchargement des tuiles en pause en arrière-plan).
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier.clip(shape)) {
        AndroidView(
            factory = {
                mapView.overlays.add(route)
                mapView.overlays.add(ends)
                mapView
            },
            update = { map ->
                val geo = points.map { GeoPoint(it.lat, it.lon) }
                route.setPoints(geo)
                ends.start = geo.firstOrNull()
                ends.end = if (geo.size > 1) geo.last() else null
                when {
                    geo.isEmpty() -> Unit
                    follow -> {
                        if (!state.centered) {
                            map.controller.setZoom(17.0)
                            map.controller.setCenter(geo.last())
                            state.centered = true
                        } else {
                            map.controller.animateTo(geo.last())
                        }
                    }
                    !state.fitted && geo.size > 1 -> {
                        // Cadre tout le trajet une fois la carte mesurée.
                        val box = BoundingBox.fromGeoPointsSafe(geo)
                        map.post { map.zoomToBoundingBox(box.increaseByScale(1.25f), false, 48) }
                        state.fitted = true
                    }
                    !state.fitted -> {
                        map.controller.setCenter(geo.first())
                        state.fitted = true
                    }
                }
                map.invalidate()
            },
        )
        if (points.isEmpty()) {
            Text(
                if (follow) "En attente du signal GPS…" else "Aucun point GPS pour ce trajet",
                style = DinoType.Small, color = DinoColors.Ink,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .background(Color.White, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Text(
            "© OpenStreetMap",
            style = DinoType.Label, color = DinoColors.InkDim,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        )
    }
}

private class MapState {
    var centered = false
    var fitted = false
}

/** Point de départ (vert) et position actuelle / arrivée (orange), cerclés de blanc. */
private class EndpointsOverlay : Overlay() {
    var start: GeoPoint? = null
    var end: GeoPoint? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    private val p = Point()

    override fun draw(c: Canvas, osmv: MapView, shadow: Boolean) {
        if (shadow) return
        val pj = osmv.projection
        start?.let { dot(c, pj.toPixels(it, p), DinoColors.Green.toArgb()) }
        end?.let { dot(c, pj.toPixels(it, p), DinoColors.AmberInk.toArgb()) }
    }

    private fun dot(c: Canvas, at: Point, color: Int) {
        c.drawCircle(at.x.toFloat(), at.y.toFloat(), 16f, ring)
        fill.color = color
        c.drawCircle(at.x.toFloat(), at.y.toFloat(), 11f, fill)
    }
}

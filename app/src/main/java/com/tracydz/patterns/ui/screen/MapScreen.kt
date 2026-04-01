package com.tracydz.patterns.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.tracydz.patterns.api.RetrofitClient
import com.tracydz.patterns.calc.PatternCalculator
import com.tracydz.patterns.data.CanopyStore
import com.tracydz.patterns.model.Canopy
import com.tracydz.patterns.model.WindData
import com.tracydz.patterns.ui.components.CanopyEditForm
import com.tracydz.patterns.ui.components.CanopySelector
import com.tracydz.patterns.ui.components.PatternOverlay
import com.tracydz.patterns.ui.components.WindBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val DEFAULT_CENTER = LatLng(37.731464, -121.334519)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // -- Canopy state --
    val canopyStore = remember { CanopyStore(context) }
    val canopies by canopyStore.canopies.collectAsState(initial = emptyList())
    val activeCanopyId by canopyStore.activeCanopyId.collectAsState(initial = null)
    val activeCanopy = canopies.find { it.id == activeCanopyId }

    // -- Wind state --
    var wind by remember { mutableStateOf(WindData()) }
    var isWindLoading by remember { mutableStateOf(false) }

    // -- Target / heading state --
    var isTargetMode by remember { mutableStateOf(false) }
    var hasTarget by remember { mutableStateOf(false) }
    val targetMarkerState = rememberMarkerState(position = DEFAULT_CENTER)
    val headingMarkerState = rememberMarkerState(position = DEFAULT_CENTER)

    // -- Swipe gesture state --
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    // -- Camera --
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTER, 16f)
    }

    // -- UI state --
    var showCanopySheet by remember { mutableStateOf(false) }
    var editingCanopy by remember { mutableStateOf<Canopy?>(null) }

    // -- DZ coordinates for wind fetch (map center) --
    val dzCoords = cameraPositionState.position.target

    // -- Pattern calculation --
    val patternResult = if (hasTarget && activeCanopy != null) {
        val heading = PatternCalculator.bearingBetween(
            headingMarkerState.position, targetMarkerState.position
        )
        PatternCalculator.calculate(activeCanopy, wind, targetMarkerState.position, heading)
    } else null

    // ---------- UI ----------

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.SATELLITE),
            uiSettings = MapUiSettings(
                compassEnabled = true,
                zoomControlsEnabled = false,
                zoomGesturesEnabled = true,
                scrollGesturesEnabled = true,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = true,
                mapToolbarEnabled = false
            ),
            onMapClick = { }
        ) {
            if (hasTarget) {
                // Approach direction line (thin white line from heading marker → target)
                Polyline(
                    points = listOf(headingMarkerState.position, targetMarkerState.position),
                    color = Color.White,
                    width = 4f
                )

                Marker(
                    state = targetMarkerState,
                    draggable = true,
                    title = "Landing Target",
                    snippet = "Drag to reposition"
                )

                Marker(
                    state = headingMarkerState,
                    draggable = true,
                    title = "Approach From",
                    snippet = "Drag to change heading",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }

            patternResult?.let { result ->
                PatternOverlay(result)
            }

            // Wind direction arrow on the map
            if (wind.speedKts > 0 && wind.directionFrom > 0) {
                val windAnchor = if (hasTarget) targetMarkerState.position
                    else cameraPositionState.position.target
                val windTowardRad = Math.toRadians(wind.directionFrom + 180.0)
                val arrowLen = 300.0 // feet
                val arrowTip = PatternCalculator.offsetLatLng(
                    windAnchor,
                    arrowLen * sin(windTowardRad),
                    arrowLen * cos(windTowardRad)
                )
                // Shaft
                Polyline(
                    points = listOf(windAnchor, arrowTip),
                    color = Color(0xFF4FC3F7),
                    width = 6f
                )
                // Left barb
                val barbLen = 80.0
                val barbAngle1 = windTowardRad + Math.toRadians(150.0)
                val barb1 = PatternCalculator.offsetLatLng(
                    arrowTip, barbLen * sin(barbAngle1), barbLen * cos(barbAngle1)
                )
                Polyline(
                    points = listOf(arrowTip, barb1),
                    color = Color(0xFF4FC3F7),
                    width = 5f
                )
                // Right barb
                val barbAngle2 = windTowardRad - Math.toRadians(150.0)
                val barb2 = PatternCalculator.offsetLatLng(
                    arrowTip, barbLen * sin(barbAngle2), barbLen * cos(barbAngle2)
                )
                Polyline(
                    points = listOf(arrowTip, barb2),
                    color = Color(0xFF4FC3F7),
                    width = 5f
                )
            }
        }

        // -- Swipe overlay for target placement --
        if (isTargetMode) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStart = offset
                                dragEnd = offset
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragEnd = change.position
                            },
                            onDragEnd = {
                                val start = dragStart
                                val end = dragEnd
                                val projection = cameraPositionState.projection
                                if (start != null && end != null && projection != null) {
                                    val startPt = android.graphics.Point(start.x.toInt(), start.y.toInt())
                                    val endPt = android.graphics.Point(end.x.toInt(), end.y.toInt())
                                    val startLatLng = projection.fromScreenLocation(startPt)
                                    val endLatLng = projection.fromScreenLocation(endPt)

                                    val dx = end.x - start.x
                                    val dy = end.y - start.y
                                    val swipeLen = sqrt(dx * dx + dy * dy)

                                    hasTarget = true
                                    targetMarkerState.position = startLatLng

                                    if (swipeLen > 50f) {
                                        val heading = PatternCalculator.bearingBetween(startLatLng, endLatLng)
                                        val behindRad = Math.toRadians(heading + 180.0)
                                        headingMarkerState.position = PatternCalculator.offsetLatLng(
                                            startLatLng,
                                            500.0 * sin(behindRad),
                                            500.0 * cos(behindRad)
                                        )
                                    } else {
                                        headingMarkerState.position =
                                            PatternCalculator.offsetLatLng(startLatLng, 0.0, -500.0)
                                    }
                                    isTargetMode = false
                                }
                                dragStart = null
                                dragEnd = null
                            },
                            onDragCancel = {
                                dragStart = null
                                dragEnd = null
                            }
                        )
                    }
            ) {
                val s = dragStart
                val e = dragEnd
                if (s != null && e != null) {
                    drawCircle(color = Color.Red, radius = 10f, center = s)
                    drawLine(
                        color = Color.White, start = s, end = e,
                        strokeWidth = 5f, cap = StrokeCap.Round
                    )
                    drawCircle(color = Color.White, radius = 8f, center = e)
                }
            }
        }

        // -- Wind bar (top center) --
        WindBar(
            wind = wind,
            onWindChange = { wind = it },
            isLoading = isWindLoading,
            onAutoFetch = {
                isWindLoading = true
                doAutoFetchWind(scope, dzCoords,
                    onResult = { w -> wind = w; isWindLoading = false },
                    onError = {
                        isWindLoading = false
                        Toast.makeText(context, "Could not fetch wind", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp)
        )

        // -- Canopy selector (top left) --
        CanopySelector(
            canopies = canopies,
            activeCanopy = activeCanopy,
            onSelectCanopy = { c -> scope.launch { canopyStore.setActiveCanopy(c.id) } },
            onAddClick = {
                editingCanopy = null
                showCanopySheet = true
            },
            onEditClick = { c ->
                editingCanopy = c
                showCanopySheet = true
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 104.dp)
        )

        // -- Target mode FAB (bottom right) --
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { isTargetMode = !isTargetMode },
                containerColor = if (isTargetMode) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Set Target"
                )
            }
        }

        // -- Target mode hint --
        if (isTargetMode) {
            Text(
                text = "Swipe: start = target, direction = heading",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // -- Pattern legend --
        if (patternResult != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 24.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                LegendItem(color = com.tracydz.patterns.ui.theme.LegDownwind, label = "Downwind 1000→600ft")
                LegendItem(color = com.tracydz.patterns.ui.theme.LegBase, label = "Base 600→300ft")
                LegendItem(color = com.tracydz.patterns.ui.theme.LegFinal, label = "Final 300→0ft")
            }
        }
    }

    // -- Canopy edit bottom sheet --
    if (showCanopySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCanopySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CanopyEditForm(
                canopy = editingCanopy,
                onSave = { canopy ->
                    scope.launch {
                        canopyStore.saveCanopy(canopy)
                        canopyStore.setActiveCanopy(canopy.id)
                    }
                    showCanopySheet = false
                },
                onDelete = if (editingCanopy != null) {
                    {
                        scope.launch { canopyStore.deleteCanopy(editingCanopy!!.id) }
                        showCanopySheet = false
                    }
                } else null
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

private fun doAutoFetchWind(
    scope: CoroutineScope,
    dzCoords: LatLng,
    onResult: (WindData) -> Unit,
    onError: () -> Unit
) {
    scope.launch {
        try {
            val response = RetrofitClient.openMeteoService.getCurrentWind(
                dzCoords.latitude, dzCoords.longitude
            )
            val speed = response.current?.wind_speed_10m ?: 0.0
            val dir = response.current?.wind_direction_10m ?: 0.0
            onResult(WindData(speed, dir))
        } catch (e: Exception) {
            onError()
        }
    }
}

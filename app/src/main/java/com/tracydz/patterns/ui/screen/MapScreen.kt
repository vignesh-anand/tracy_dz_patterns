package com.tracydz.patterns.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
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

    // -- Camera --
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTER, 16f)
    }

    // -- UI state --
    var showCanopySheet by remember { mutableStateOf(false) }
    var editingCanopy by remember { mutableStateOf<Canopy?>(null) }

    // -- Location --
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            doAutoFetchWind(fusedClient, scope, cameraPositionState,
                onResult = { w -> wind = w; isWindLoading = false },
                onError = {
                    isWindLoading = false
                    Toast.makeText(context, "Could not fetch wind", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            isWindLoading = false
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

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
            onMapClick = { latLng ->
                if (isTargetMode) {
                    hasTarget = true
                    targetMarkerState.position = latLng
                    // Place heading marker 500ft south (default northerly heading)
                    headingMarkerState.position =
                        PatternCalculator.offsetLatLng(latLng, 0.0, -500.0)
                    isTargetMode = false
                }
            }
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
        }

        // -- Wind bar (top center) --
        WindBar(
            wind = wind,
            onWindChange = { wind = it },
            isLoading = isWindLoading,
            onAutoFetch = {
                isWindLoading = true
                val hasPerm = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    doAutoFetchWind(fusedClient, scope, cameraPositionState,
                        onResult = { w -> wind = w; isWindLoading = false },
                        onError = {
                            isWindLoading = false
                            Toast.makeText(context, "Could not fetch wind", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
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
                text = "Tap map to set landing target",
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

@SuppressLint("MissingPermission")
private fun doAutoFetchWind(
    fusedClient: FusedLocationProviderClient,
    scope: CoroutineScope,
    cameraPositionState: CameraPositionState,
    onResult: (WindData) -> Unit,
    onError: () -> Unit
) {
    fusedClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLng(
                        LatLng(location.latitude, location.longitude)
                    )
                )
                scope.launch {
                    try {
                        val response = RetrofitClient.openMeteoService.getCurrentWind(
                            location.latitude, location.longitude
                        )
                        val speed = response.current?.wind_speed_10m ?: 0.0
                        val dir = response.current?.wind_direction_10m ?: 0.0
                        onResult(WindData(speed, dir))
                    } catch (e: Exception) {
                        onError()
                    }
                }
            } else {
                onError()
            }
        }
        .addOnFailureListener { onError() }
}

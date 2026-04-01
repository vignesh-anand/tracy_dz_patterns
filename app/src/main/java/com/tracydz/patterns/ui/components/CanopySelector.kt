package com.tracydz.patterns.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tracydz.patterns.model.Canopy

@Composable
fun CanopySelector(
    canopies: List<Canopy>,
    activeCanopy: Canopy?,
    onSelectCanopy: (Canopy) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Canopy) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = {
            if (canopies.isEmpty()) onAddClick() else expanded = true
        }) {
            Text(
                text = activeCanopy?.name ?: "Add Canopy",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
        }

        if (activeCanopy != null) {
            IconButton(
                onClick = { onEditClick(activeCanopy) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit, contentDescription = "Edit canopy",
                    tint = Color.White, modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            canopies.forEach { canopy ->
                DropdownMenuItem(
                    text = { Text("${canopy.name} (${canopy.airspeedKts.toInt()}kts, ${canopy.glideRatio}:1)") },
                    onClick = {
                        onSelectCanopy(canopy)
                        expanded = false
                    },
                    leadingIcon = {
                        if (canopy.id == activeCanopy?.id) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Add New...") },
                onClick = {
                    expanded = false
                    onAddClick()
                },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }
    }
}

@Composable
fun CanopyEditForm(
    canopy: Canopy?,
    onSave: (Canopy) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(canopy?.name ?: "") }
    var airspeed by remember { mutableStateOf(canopy?.airspeedKts?.let { "%.1f".format(it) } ?: "") }
    var glideRatio by remember { mutableStateOf(canopy?.glideRatio?.let { "%.1f".format(it) } ?: "") }

    val isValid = name.isNotBlank()
            && (airspeed.toDoubleOrNull() ?: 0.0) > 0
            && (glideRatio.toDoubleOrNull() ?: 0.0) > 0

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (canopy != null) "Edit Canopy" else "Add Canopy",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name (e.g. Sabre3 170)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = airspeed,
            onValueChange = { airspeed = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Forward airspeed (kts)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        OutlinedTextField(
            value = glideRatio,
            onValueChange = { glideRatio = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Glide ratio (e.g. 2.5)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            } else {
                Spacer(Modifier)
            }

            Button(
                onClick = {
                    onSave(
                        Canopy(
                            id = canopy?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            airspeedKts = airspeed.toDouble(),
                            glideRatio = glideRatio.toDouble()
                        )
                    )
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

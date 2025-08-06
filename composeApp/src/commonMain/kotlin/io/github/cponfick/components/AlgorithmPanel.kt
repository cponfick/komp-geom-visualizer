package io.github.cponfick.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.cponfick.algorithms.AlgorithmRegistry
import io.github.cponfick.state.CanvasState
import io.github.cponfick.state.GeometryAlgorithm

@Composable
fun AlgorithmPanel(
  canvasState: CanvasState,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.widthIn(Dp.Unspecified, 450.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "Algorithm Tools",
        style = MaterialTheme.typography.titleMedium
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = { canvasState.toggleSelectionMode() },
          colors = if (canvasState.isSelectionMode) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          } else {
            ButtonDefaults.outlinedButtonColors()
          }
        ) {
          Text(if (canvasState.isSelectionMode) "Exit Selection" else "Selection Mode")
        }

        if (canvasState.isSelectionMode) {
          Button(
            onClick = { canvasState.selectAllPoints() },
            modifier = Modifier.height(36.dp)
          ) {
            Text("Select All")
          }

          Button(
            onClick = { canvasState.clearSelection() },
            modifier = Modifier.height(36.dp)
          ) {
            Text("Clear Selection")
          }
        }
      }

      // Show selected points count
      if (canvasState.selectedPoints.isNotEmpty()) {
        Text(
          text = "${canvasState.selectedPoints.size} points selected",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }

      HorizontalDivider()

      Text(
        text = "Available Algorithms",
        style = MaterialTheme.typography.titleSmall
      )

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(200.dp)
      ) {
        items(AlgorithmRegistry.getAllAlgorithms()) { algorithm ->
          AlgorithmItem(
            algorithm = algorithm,
            canExecute = canvasState.selectedPoints.size >= algorithm.getMinimumPoints(),
            onExecute = { canvasState.executeAlgorithm(algorithm) }
          )
        }
      }

      HorizontalDivider()

      // Algorithm results controls
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            canvasState.clearSelection()
            canvasState.clearAlgorithmResults()
            canvasState.clearPoints()
          },
        ) {
          Text("Clear Scene")
        }

        Button(
          onClick = { canvasState.clearAlgorithmResults() },
          enabled = canvasState.algorithmResults.isNotEmpty()
        ) {
          Text("Clear Results")
        }
      }

      // Show active results
      if (canvasState.algorithmResults.isNotEmpty()) {
        Text(
          text = "Active Results: ${canvasState.algorithmResults.keys.joinToString(", ")}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }

    HorizontalDivider()

    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Checkbox for showing labels

      var showLabels by remember { mutableStateOf(true) }

      LaunchedEffect(Unit) {
        canvasState.showLabels = showLabels
      }

      Checkbox(
        checked = showLabels,
        onCheckedChange = {
          showLabels = it
          canvasState.showLabels = showLabels
        },
      )
      Text(
        text = "Show Labels"
      )
    }
  }
}

@Composable
private fun AlgorithmItem(
  algorithm: GeometryAlgorithm,
  canExecute: Boolean,
  onExecute: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = algorithm.getName(),
          style = MaterialTheme.typography.bodyMedium
        )
        Text(
          text = "Requires ${algorithm.getMinimumPoints()} points minimum",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Button(
        onClick = onExecute,
        enabled = canExecute,
        modifier = Modifier.height(32.dp)
      ) {
        Text("Run")
      }
    }
  }
}

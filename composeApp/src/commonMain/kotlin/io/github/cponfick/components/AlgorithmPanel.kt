package io.github.cponfick.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.cponfick.algorithms.AlgorithmRegistry
import io.github.cponfick.components.utils.formatDouble
import io.github.cponfick.state.AlgorithmResult
import io.github.cponfick.state.CanvasState
import io.github.cponfick.state.GeometricAlgorithm

@Composable
fun AlgorithmPanel(canvasState: CanvasState, modifier: Modifier = Modifier) {
  // On phones the canvas is the primary surface: start as a small toolbar and
  // only open the drawer when the user explicitly asks for the tools.
  var expanded by remember { mutableStateOf(false) }
  var help by remember { mutableStateOf(false) }
  var confirmClear by remember { mutableStateOf(false) }
  var confirmMode by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier) {
    val compact = maxWidth < 600.dp
    if (compact && !expanded) {
      Button(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("☰  Tools") }
    } else {
      Card(
        modifier = Modifier
          .widthIn(max = if (compact) 380.dp else 450.dp)
          .heightIn(max = maxHeight * 0.82f),
        elevation = CardDefaults.cardElevation(4.dp)
      ) {
        Column(
          Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Algorithm Tools", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { help = true }) { Text("? Help") }
            if (compact) TextButton(onClick = { expanded = false }) { Text("Close") }
          }
          Text("Mode", style = MaterialTheme.typography.titleSmall)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { if (canvasState.isSelectionMode && canvasState.selectedPoints.isNotEmpty()) confirmMode = true else canvasState.applySelectionMode(false) }, modifier = Modifier.weight(1f), colors = if (!canvasState.isSelectionMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) { Text("Add points") }
            Button(onClick = { canvasState.applySelectionMode(true) }, modifier = Modifier.weight(1f), colors = if (canvasState.isSelectionMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) { Text("Select points") }
          }
          Text(if (canvasState.isSelectionMode) "Click points to toggle selection; algorithms use selected points." else "Click to add points. Drag to pan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { canvasState.selectAllPoints() }, enabled = canvasState.isSelectionMode && canvasState.points.isNotEmpty()) { Text("Select all") }
            OutlinedButton(onClick = { canvasState.clearSelection() }, enabled = canvasState.selectedPoints.isNotEmpty()) { Text("Clear selection") }
          }
          Text("${canvasState.selectedPoints.size} selected / ${canvasState.points.size} points", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
          HorizontalDivider()
          Text("Run on selected points", style = MaterialTheme.typography.titleSmall)
          AlgorithmRegistry.getAllAlgorithms().forEach { algorithm ->
            AlgorithmItem(algorithm, canvasState.selectedPoints.size >= algorithm.getMinimumPoints(), canvasState.algorithmResults[algorithm.getName()], { canvasState.executeAlgorithm(algorithm) }, { canvasState.removeAlgorithmResult(algorithm.getName()) })
          }
          if (canvasState.selectedPoints.size < 2) Text("Select points to enable algorithms.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          HorizontalDivider()
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { canvasState.undo() }, enabled = canvasState.canUndo) { Text("Undo") }
            OutlinedButton(onClick = { canvasState.redo() }, enabled = canvasState.canRedo) { Text("Redo") }
            OutlinedButton(onClick = { canvasState.resetView() }) { Text("Reset view") }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { confirmClear = true }, enabled = canvasState.points.isNotEmpty() || canvasState.algorithmResults.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Clear scene") }
          }
          OutlinedButton(onClick = { canvasState.clearAlgorithmResults() }, enabled = canvasState.algorithmResults.isNotEmpty()) { Text("Clear results") }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(canvasState.showLabels, { canvasState.showLabels = it })
            Text("Show coordinate labels")
          }
          Text("Zoom: ${canvasState.zoomPercent}%  •  Drag to pan • Wheel to zoom", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
  if (help) HelpDialog { help = false }
  if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("Clear scene?") }, text = { Text("This removes all points, selection, and algorithm results.") }, confirmButton = { TextButton(onClick = { canvasState.clearPoints(); confirmClear = false }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } })
  if (confirmMode) AlertDialog(onDismissRequest = { confirmMode = false }, title = { Text("Clear selection?") }, text = { Text("Switching to Add points clears the current selection and results.") }, confirmButton = { TextButton(onClick = { canvasState.applySelectionMode(false); confirmMode = false }) { Text("Switch mode") } }, dismissButton = { TextButton(onClick = { confirmMode = false }) { Text("Cancel") } })
}

@Composable private fun HelpDialog(onDismiss: () -> Unit) = AlertDialog(
  onDismissRequest = onDismiss,
  title = { Text("How to use the canvas") },
  text = {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      HelpSection("Create points", "Choose Add points, then click anywhere on the canvas.")
      HelpSection("Select points", "Choose Select points and click points to toggle them. Algorithms run only on selected points.")
      HelpSection("Navigate", "Click-drag to pan. Use the mouse wheel or trackpad to zoom around the pointer. Reset view returns to the original view.")
      HelpSection("Run algorithms", "Open Algorithm Tools, select enough points, and press Run. Results appear in the algorithm row and on the canvas.")
    }
  },
  confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
)

@Composable private fun HelpSection(title: String, description: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Text(description, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable private fun AlgorithmItem(algorithm: GeometricAlgorithm, canExecute: Boolean, result: AlgorithmResult?, onExecute: () -> Unit, onRemove: () -> Unit) {
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(algorithm.getName(), style = MaterialTheme.typography.bodyMedium)
          Text("Needs ${algorithm.getMinimumPoints()} points", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onExecute, enabled = canExecute, modifier = Modifier.height(36.dp)) { Text("Run") }
      }
      if (!canExecute) Text("Select at least ${algorithm.getMinimumPoints()} points", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      if (result != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(10.dp).background(resultColor(result), CircleShape))
          Spacer(Modifier.width(6.dp))
          Text(resultSummary(result), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
          TextButton(onClick = onRemove) { Text("Remove") }
        }
      }
    }
  }
}

private fun resultColor(result: AlgorithmResult): Color = when (result) {
  is AlgorithmResult.PointPair -> result.color
  is AlgorithmResult.Line -> result.color
  is AlgorithmResult.Points -> result.color
  is AlgorithmResult.Lines -> result.lines.firstOrNull()?.color ?: Color.Green
  is AlgorithmResult.Error -> Color.Red
}
private fun resultSummary(result: AlgorithmResult): String = when (result) {
  is AlgorithmResult.PointPair -> "Closest pair • d = ${formatDouble(result.distance)}"
  is AlgorithmResult.Line -> "Line overlay"
  is AlgorithmResult.Points -> "${result.points.size} point overlay"
  is AlgorithmResult.Lines -> "${result.lines.size} line overlay"
  is AlgorithmResult.Error -> "Error: ${result.message}"
}

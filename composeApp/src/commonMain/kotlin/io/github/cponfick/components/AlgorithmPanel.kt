package io.github.cponfick.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.cponfick.algorithms.AlgorithmRegistry
import io.github.cponfick.components.utils.formatDouble
import io.github.cponfick.state.*

@Composable fun AlgorithmPanel(canvasState: CanvasState, modifier: Modifier=Modifier) {
  var expanded by remember { mutableStateOf(false) }; var search by remember { mutableStateOf("") }; var confirmClear by remember { mutableStateOf(false) }
  BoxWithConstraints(modifier) {
    val compact=maxWidth<600.dp
    if(compact&&!expanded) Button({expanded=true},Modifier.heightIn(min=48.dp)){Text("☰  Tools")} else Card(Modifier.widthIn(max=450.dp).heightIn(max=maxHeight*.86f)) {
      Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row { Text("Algorithm Tools",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f)); if(compact)TextButton({expanded=false}){Text("Close")} }
        Text("Mode",style=MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
          Button({canvasState.changeInteractionMode(InteractionMode.ADD_POINTS)},Modifier.weight(1f),enabled=canvasState.interactionMode!=InteractionMode.ADD_POINTS){Text("Add points")}
          Button({canvasState.changeInteractionMode(InteractionMode.DRAW_SEGMENTS)},Modifier.weight(1f),enabled=canvasState.interactionMode!=InteractionMode.DRAW_SEGMENTS){Text("Draw segments")}
          Button({canvasState.changeInteractionMode(InteractionMode.SELECT)},Modifier.weight(1f),enabled=canvasState.interactionMode!=InteractionMode.SELECT){Text("Select")}
        }
        Text(when(canvasState.interactionMode){InteractionMode.ADD_POINTS->"Click to add points; drag to pan.";InteractionMode.DRAW_SEGMENTS->"Drag on the canvas to draw a segment.";InteractionMode.SELECT->"Click points or segments to toggle selection."},style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){OutlinedButton({canvasState.selectAllPoints()},enabled=canvasState.isSelectionMode&&canvasState.points.isNotEmpty()){Text("All points")};OutlinedButton({canvasState.selectAllSegments()},enabled=canvasState.isSelectionMode&&canvasState.segments.isNotEmpty()){Text("All segments")};OutlinedButton({canvasState.clearSelection()},enabled=canvasState.selectedPoints.isNotEmpty()||canvasState.selectedSegments.isNotEmpty()){Text("Clear")}}
        Text("${canvasState.selectedPoints.size} points • ${canvasState.selectedSegments.size} segments selected",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
        OutlinedButton({canvasState.deleteSelected()},enabled=canvasState.selectedPoints.isNotEmpty()||canvasState.selectedSegments.isNotEmpty()){Text("Delete selected")}
        HorizontalDivider(); Text("Tools",style=MaterialTheme.typography.titleSmall)
        OutlinedTextField(search,{search=it},label={Text("Search algorithms")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val query=search.trim().lowercase(); val groups=AlgorithmRegistry.byCategory()
        groups.forEach { (category,items) -> val visible=items.filter { query.isEmpty()||listOf(it.displayName,it.description,category.name,*it.tags.toTypedArray()).any { value->value.lowercase().contains(query)} }; if(visible.isNotEmpty()){Text(category.name.lowercase().replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.titleSmall);visible.forEach { descriptor ->
          val count=if(descriptor.input==AlgorithmInput.SEGMENTS)canvasState.selectedSegments.size else canvasState.selectedPoints.size; val result=canvasState.algorithmResults[descriptor.id]
          Card(Modifier.fillMaxWidth()){Column(Modifier.padding(8.dp)){Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(descriptor.displayName);Text("${descriptor.input.name.lowercase()} • needs ${descriptor.minimumItems}",style=MaterialTheme.typography.labelSmall)};Button({canvasState.executeAlgorithm(descriptor)},enabled=count>=descriptor.minimumItems){Text("Run")}};Text(descriptor.description,style=MaterialTheme.typography.bodySmall);result?.let{Text(summary(it),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary);TextButton({canvasState.removeAlgorithmResult(descriptor.id)}){Text("Remove")}}}}
        } } }
        HorizontalDivider();Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){OutlinedButton({canvasState.undo()},enabled=canvasState.canUndo){Text("Undo")};OutlinedButton({canvasState.redo()},enabled=canvasState.canRedo){Text("Redo")};OutlinedButton({canvasState.resetView()}){Text("Reset")}}
        Button({confirmClear=true},enabled=canvasState.points.isNotEmpty()||canvasState.segments.isNotEmpty(),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Clear scene")}
        OutlinedButton({canvasState.clearAlgorithmResults()},enabled=canvasState.algorithmResults.isNotEmpty()){Text("Clear results")}
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(canvasState.showLabels,{canvasState.showLabels=it});Text("Show labels")}
      }
    }
  }
  if(confirmClear)AlertDialog({confirmClear=false},title={Text("Clear scene?")},text={Text("This removes all points, segments, and results.")},confirmButton={TextButton({canvasState.clearScene();confirmClear=false}){Text("Clear")}},dismissButton={TextButton({confirmClear=false}){Text("Cancel")}})
}
private fun summary(result:AlgorithmResult)=when(result){is AlgorithmResult.BentleyOttmannResult->"${result.intersections.size} intersecting pairs";is AlgorithmResult.PointPair->"Closest pair • d = ${formatDouble(result.distance)}";is AlgorithmResult.Points->"${result.points.size} points";is AlgorithmResult.Lines->"${result.lines.size} lines";is AlgorithmResult.Line->"Line overlay";is AlgorithmResult.Error->"Error: ${result.message}"}

package io.github.cponfick.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationBar(
  onClearPoints: () -> Unit,
  onShowLabelsChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = onClearPoints,
        modifier = Modifier.padding(end = 8.dp)
      ) {
        Text("Clear Points")
      }
    }
    // Check box for showing labels
    Row(
      modifier = Modifier.padding(start = 8.dp),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.CenterVertically
    ) {
      var selected by remember { mutableStateOf(true) }

      LaunchedEffect(Unit) {
        onShowLabelsChanged(selected)
      }

      RadioButton(
        selected = selected,
        onClick = {
          selected = !selected
          onShowLabelsChanged(selected)
        },
      )
      Text(
        text = "Show Labels",
        modifier = Modifier
      )
    }
  }
}

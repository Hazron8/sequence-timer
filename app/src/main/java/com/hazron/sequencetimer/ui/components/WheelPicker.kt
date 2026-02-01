package com.hazron.sequencetimer.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val items = range.toList()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, items.size - 1)
    )
    val coroutineScope = rememberCoroutineScope()

    // Snap to center item when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex +
                (listState.firstVisibleItemScrollOffset / 56f).toInt()
            val newValue = items.getOrElse(centerIndex) { value }
            if (newValue != value) {
                onValueChange(newValue)
            }
        }
    }

    // Scroll to value when it changes externally
    LaunchedEffect(value) {
        val targetIndex = (value - range.first).coerceIn(0, items.size - 1)
        if (listState.firstVisibleItemIndex != targetIndex) {
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier.height(168.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selection indicator
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 28.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 56.dp),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                items(items.size) { index ->
                    val itemValue = items[index]
                    val isSelected = listState.firstVisibleItemIndex == index &&
                        listState.firstVisibleItemScrollOffset < 28

                    Text(
                        text = String.format("%02d", itemValue),
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .alpha(if (isSelected) 1f else 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun TimeDurationPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            value = hours,
            range = 0..23,
            onValueChange = onHoursChange,
            modifier = Modifier.weight(1f),
            label = "Hours"
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        WheelPicker(
            value = minutes,
            range = 0..59,
            onValueChange = onMinutesChange,
            modifier = Modifier.weight(1f),
            label = "Min"
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        WheelPicker(
            value = seconds,
            range = 0..59,
            onValueChange = onSecondsChange,
            modifier = Modifier.weight(1f),
            label = "Sec"
        )
    }
}

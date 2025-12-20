package com.ritesh.cashiro.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@SuppressLint("ModifierParameter")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingCircle(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { LoadingIndicator(
        polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
    ) }
}

@SuppressLint("ModifierParameter")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingLine(modifier: Modifier = Modifier.fillMaxWidth(), progress: Float? = null) {
    if (progress != null) LinearWavyProgressIndicator(modifier = modifier, progress = { progress })
    else LinearWavyProgressIndicator(modifier = modifier)
}

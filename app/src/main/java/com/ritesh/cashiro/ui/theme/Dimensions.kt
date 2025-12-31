package com.ritesh.cashiro.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimensions for consistent UI throughout the app
 */
object Dimensions {
    
    // Padding values (complementing Spacing.kt)
    object Padding {
        val content = 16.dp      // Standard content padding
        val card = 20.dp         // Card internal padding
        val empty = 32.dp        // Empty state padding
        val fab = 16.dp          // FAB padding
    }
    object Radius {
        val xs = 4.dp
        val sm = 8.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp
        val xxl = 48.dp
    }
    
    // Elevation values
    object Elevation {
        val card = 1.dp
        val fab = 6.dp
        val bottomBar = 3.dp
        val dialog = 8.dp
    }
    
    // Alpha values for transparency
    object Alpha {
        const val high = 0.87f
        const val medium = 0.6f
        const val disabled = 0.38f
        const val divider = 0.12f
        const val surface = 0.7f
        const val subtitle = 0.8f
    }
    
    // Icon sizes
    object Icon {
        val small = 16.dp
        val medium = 24.dp
        val large = 48.dp
        val extraLarge = 120.dp  // For empty states
    }
    
    // Corner radius (additional to Shape.kt)
    object CornerRadius {
        val small = 4.dp
        val medium = 8.dp
        val large = 12.dp
        val full = 50.dp  // For circular elements
    }
    
    // Text sizes
    object TextSize {
        val small = 12.sp
        val body = 14.sp
        val medium = 16.sp
        val large = 20.sp
        val title = 24.sp
        val display = 32.sp
    }
    
    // Component specific dimensions
    object Component {
        val bottomBarHeight = 80.dp
        val buttonHeight = 48.dp
        val minTouchTarget = 48.dp
        val dividerThickness = 1.dp
        val progressIndicatorSize = 24.dp
        val chipHeight = 32.dp
    }
    
    // Animation durations (in milliseconds)
    object Animation {
        const val short = 150
        const val medium = 300
        const val long = 500
    }
}
package moye.wear.wraive.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults

@Composable
internal fun rememberChatRotaryScrollableBehavior(state: ScrollableState): RotaryScrollableBehavior {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val behavior = RotaryScrollableDefaults.behavior(state)
    // Keep the crown's scroll/fling state when streamed text recomposes the chat.
    return remember(state, density, configuration) { behavior }
}

package com.composables.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.AppearInstantly
import com.composeunstyled.DisappearInstantly
import com.composeunstyled.Modal
import kotlinx.coroutines.delay

data class ModalSheetProperties(
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = true,
)

@Composable
fun rememberModalBottomSheetState(
    initialDetent: SheetDetent,
    detents: List<SheetDetent> = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded),
    animationSpec: AnimationSpec<Float> = tween(),
    velocityThreshold: () -> Dp = { 125.dp },
    positionalThreshold: (totalDistance: Dp) -> Dp = { 56.dp },
    confirmDetentChange: (SheetDetent) -> Boolean = { true },
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
): ModalBottomSheetState {
    val sheetState = rememberBottomSheetState(
        initialDetent = initialDetent,
        detents = detents,
        animationSpec = animationSpec,
        velocityThreshold = velocityThreshold,
        positionalThreshold = positionalThreshold,
        decayAnimationSpec = decayAnimationSpec,
        confirmDetentChange = confirmDetentChange,
    )
    return rememberSaveable(
        saver = mapSaver(
            save = { modalBottomSheetState -> mapOf("detent" to modalBottomSheetState.currentDetent.identifier) },
            restore = { map ->
                val restoredDetent = detents.first { it.identifier == map["detent"] }
                ModalBottomSheetState(
                    bottomSheetDetent = restoredDetent,
                    bottomSheetState = sheetState
                )
            }
        ),
        init = {
            ModalBottomSheetState(
                bottomSheetDetent = initialDetent,
                bottomSheetState = sheetState
            )
        }
    )
}

class ModalBottomSheetState internal constructor(
    internal val bottomSheetDetent: SheetDetent,
    internal val bottomSheetState: BottomSheetState
) {

    internal var modalDetent by mutableStateOf(bottomSheetDetent)

    var currentDetent: SheetDetent
        get() {
            return modalDetent
        }
        @Deprecated(
            message = "This setter will go away in a future version of the library. Set the value to targetDetent instead",
            replaceWith = ReplaceWith("targetDetent")
        )
        set(value) {
            val isBottomSheetVisible = bottomSheetState.currentDetent != SheetDetent.Hidden
                    || bottomSheetState.targetDetent != SheetDetent.Hidden

            if (isBottomSheetVisible) {
                bottomSheetState.targetDetent = value
            } else {
                modalDetent = value
            }
        }
    var targetDetent: SheetDetent
        get() = bottomSheetState.targetDetent
        set(value) {
            val isBottomSheetVisible = bottomSheetState.currentDetent != SheetDetent.Hidden
                    || bottomSheetState.targetDetent != SheetDetent.Hidden

            if (isBottomSheetVisible) {
                bottomSheetState.targetDetent = value
            } else {
                modalDetent = value
            }
        }

    val isIdle: Boolean by derivedStateOf {
        bottomSheetState.isIdle
    }
    val progress: Float by derivedStateOf {
        bottomSheetState.progress
    }
    val offset: Float by derivedStateOf {
        bottomSheetState.offset
    }

    suspend fun animateTo(value: SheetDetent) {
        val isBottomSheetVisible = bottomSheetState.currentDetent != SheetDetent.Hidden
                || bottomSheetState.targetDetent != SheetDetent.Hidden

        if (isBottomSheetVisible) {
            bottomSheetState.animateTo(value)
        } else {
            modalDetent = value
        }
    }

    fun jumpTo(value: SheetDetent) {
        val isBottomSheetVisible =
            bottomSheetState.currentDetent != SheetDetent.Hidden || bottomSheetState.targetDetent != SheetDetent.Hidden

        if (isBottomSheetVisible) {
            bottomSheetState.jumpTo(value)
        } else {
            modalDetent = value
        }
    }
}

class ModalBottomSheetScope internal constructor(
    internal val modalState: ModalBottomSheetState,
    internal val sheetState: BottomSheetState,
) {
    internal val visibleState = MutableTransitionState(false)
}

private class ModalContext(val onDismissRequest: () -> Unit)

private val LocalModalContext = compositionLocalOf<ModalContext> {
    error("Modal not initialized")
}
val DoNothing: () -> Unit = {}

@Composable
fun ModalBottomSheet(
    state: ModalBottomSheetState,
    properties: ModalSheetProperties = ModalSheetProperties(),
    onDismiss: () -> Unit = DoNothing,
    content: @Composable (ModalBottomSheetScope.() -> Unit),
) {
    val currentCallback by rememberUpdatedState(onDismiss)

    CompositionLocalProvider(LocalModalContext provides ModalContext(currentCallback)) {
        val scope = remember { ModalBottomSheetScope(state, state.bottomSheetState) }
        scope.visibleState.targetState = state.currentDetent != SheetDetent.Hidden

        if (scope.visibleState.currentState || scope.visibleState.targetState || scope.visibleState.isIdle.not()) {
            val onKeyEvent = if (properties.dismissOnBackPress) {
                { event: KeyEvent ->
                    if (
                        event.type == KeyEventType.KeyDown
                        && (event.key == Key.Back || event.key == Key.Escape)
                        && state.bottomSheetState.confirmDetentChange(SheetDetent.Hidden)
                    ) {
                        scope.sheetState.targetDetent = SheetDetent.Hidden
                        true
                    } else false
                }
            } else {
                { false }
            }

            Modal(onKeyEvent = onKeyEvent) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .let {
                            if (properties.dismissOnClickOutside) {
                                it.pointerInput(Unit) {
                                    detectTapGestures {
                                        if (state.bottomSheetState.confirmDetentChange(SheetDetent.Hidden)) {
                                            state.targetDetent = SheetDetent.Hidden
                                        }
                                    }
                                }
                            } else it
                        }
                ) {
                    scope.content()
                }
            }
        }
    }
}

@Composable
fun ModalBottomSheetScope.Scrim(
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(0.6f),
    enter: EnterTransition = AppearInstantly,
    exit: ExitTransition = DisappearInstantly,
) {
    AnimatedVisibility(
        visibleState = visibleState,
        enter = enter,
        exit = exit
    ) {
        Box(Modifier.fillMaxSize().focusable(false).background(scrimColor).then(modifier))
    }
}

@Composable
fun ModalBottomSheetScope.Sheet(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (BottomSheetScope.() -> Unit)
) {
    var hasBeenIntroduced by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // waiting for the dialog to settle: can't just start animation here
        delay(50)
        sheetState.targetDetent = modalState.modalDetent
        hasBeenIntroduced = true
    }

    if (hasBeenIntroduced) {
        val context = LocalModalContext.current
        LaunchedEffect(sheetState.isIdle) {
            if (sheetState.isIdle) {
                if (sheetState.targetDetent == SheetDetent.Hidden) {
                    context.onDismissRequest()
                    modalState.modalDetent = SheetDetent.Hidden
                } else {
                    modalState.modalDetent = sheetState.currentDetent
                }
            }
        }
    }
    BottomSheet(
        state = sheetState,
        enabled = enabled,
        modifier = modifier,
        content = content
    )
}

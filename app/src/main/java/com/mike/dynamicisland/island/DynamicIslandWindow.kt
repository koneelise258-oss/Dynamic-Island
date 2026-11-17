package com.mike.dynamicisland.island

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.MikeDynamicIslandTheme
import com.mike.dynamicisland.core.MikeEngine
import com.mike.dynamicisland.faceid.FaceUnlockPhase
import com.mike.dynamicisland.faceid.StyledFaceScanOverlay
import com.mike.dynamicisland.faceid.rememberFaceAnimationController
import com.mike.dynamicisland.quickglass.IslandBlueprintMetrics
import com.mike.dynamicisland.quickglass.MikeDesignTokens
import com.mike.dynamicisland.quickglass.QuickGlassEngine
import com.mike.dynamicisland.utils.DisplayCutoutManager
import com.mike.dynamicisland.utils.PermissionManager

private class CompactOverlayOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun start() {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

/**
 * Fenêtre flottante strictement bornée en haut de l'écran (PARTIE 1 & PARTIE 4).
 *
 * RÈGLES ABSOLUES APPLIQUÉES :
 * 1. Jamais MATCH_PARENT, jamais de fenêtre transparente plein écran.
 * 2. Jamais FLAG_LAYOUT_NO_LIMITS ou de mode immersif masquant la barre d'état.
 * 3. Dimensions explicitement plafonnées par DisplayCutoutManager (Face ID <= 124dp, Smart Island <= 216dp).
 * 4. Détachement immédiat de la vue dès que l'écran s'éteint ou qu'aucune activité n'est active.
 */
class DynamicIslandWindow(
    private val context: Context,
    private val engine: MikeEngine,
    private val onFaceSuccessHaptic: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var faceView: ComposeView? = null
    private var faceOwner: CompactOverlayOwner? = null

    private var islandView: ComposeView? = null
    private var islandOwner: CompactOverlayOwner? = null

    fun syncOverlayVisibility(
        shouldShowFaceId: Boolean,
        shouldShowIsland: Boolean,
        isIslandExpanded: Boolean
    ) {
        if (!PermissionManager.hasOverlayPermission(context)) {
            detachAll()
            return
        }

        if (shouldShowFaceId) {
            attachOrUpdateFaceWindow()
        } else {
            detachFaceWindow()
        }

        if (shouldShowIsland) {
            attachOrUpdateIslandWindow(isExpanded = isIslandExpanded)
        } else {
            detachIslandWindow()
        }
    }

    fun buildStrictFaceLayoutParams(): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        val settings = engine.settings.value.faceId
        val profile = engine.deviceProfile.value
        val bounds = DisplayCutoutManager.computeFaceOverlayBounds(
            profile = profile,
            density = density,
            ringSizeDp = settings.sizeDp,
            positionMode = settings.positionMode,
            manualOffsetXDp = settings.manualOffsetXDp,
            manualOffsetYDp = settings.manualOffsetYDp
        )
        return WindowManager.LayoutParams(
            bounds.maxWidthPx,
            bounds.maxHeightPx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = bounds.gravity
            x = bounds.xPx
            y = bounds.yPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    fun buildStrictIslandLayoutParams(isExpanded: Boolean): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        val settings = engine.settings.value.island
        val profile = engine.deviceProfile.value
        val bounds = DisplayCutoutManager.computeIslandOverlayBounds(
            profile = profile,
            density = density,
            isExpanded = isExpanded,
            alignment = settings.alignment,
            verticalOffsetDp = settings.verticalOffsetDp,
            horizontalOffsetDp = settings.horizontalOffsetDp
        )
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = bounds.gravity
            x = bounds.xPx
            y = bounds.yPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun attachOrUpdateFaceWindow() {
        val params = buildStrictFaceLayoutParams()
        val existing = faceView
        if (existing != null) {
            runCatching { windowManager.updateViewLayout(existing, params) }
            return
        }

        val owner = CompactOverlayOwner().apply { start() }
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                MikeDynamicIslandTheme {
                    val settings by engine.settings.collectAsState()
                    val unlockState by engine.faceUnlockEngine.state.collectAsState()
                    val profile by engine.deviceProfile.collectAsState()

                    val controller = rememberFaceAnimationController(
                        unlockState = unlockState,
                        settings = settings.faceId,
                        onCompleted = { engine.faceUnlockEngine.markDismissed() },
                        onSuccessReached = {
                            if (settings.faceId.vibrateOnSuccess) onFaceSuccessHaptic()
                        }
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .widthIn(max = MikeDesignTokens.MaxFaceWindowSizeDp)
                            .heightIn(max = MikeDesignTokens.MaxFaceWindowSizeDp)
                            .padding(2.dp)
                    ) {
                        StyledFaceScanOverlay(
                            visible = controller.isVisible || unlockState.phase == FaceUnlockPhase.SCANNING,
                            progress = controller.progress.value,
                            success = controller.isSuccess,
                            interrupted = controller.isInterrupted,
                            isScreenOn = unlockState.isScreenOn,
                            settings = settings.faceId,
                            maxParticleCount = profile.maxParticleCount
                        )
                    }
                }
            }
        }

        runCatching {
            windowManager.addView(composeView, params)
            faceOwner = owner
            faceView = composeView
        }.onFailure {
            owner.destroy()
        }
    }

    private fun attachOrUpdateIslandWindow(isExpanded: Boolean) {
        val params = buildStrictIslandLayoutParams(isExpanded = isExpanded)
        val existing = islandView
        if (existing != null) {
            runCatching { windowManager.updateViewLayout(existing, params) }
            return
        }

        val owner = CompactOverlayOwner().apply { start() }
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                MikeDynamicIslandTheme {
                    val appSettings by engine.settings.collectAsState()
                    val islandState by engine.dynamicIslandEngine.uiState.collectAsState()
                    val profile by engine.deviceProfile.collectAsState()
                    val effectiveGlass = QuickGlassEngine.resolveEffectiveConfig(
                        raw = appSettings.quickGlass,
                        profile = profile
                    )
                    val blueprint = IslandBlueprintMetrics.forScreenWidthDp(profile.screenWidthDp)

                    Box(
                        modifier = Modifier
                            .widthIn(max = blueprint.expandedMaxWidth)
                            .heightIn(max = MikeDesignTokens.MaxFloatingWindowHeightDp)
                    ) {
                        DynamicIslandLayout(
                            state = islandState,
                            islandSettings = appSettings.island,
                            glassConfig = effectiveGlass,
                            blueprint = blueprint,
                            callbacks = engine.islandCallbacks
                        )
                    }
                }
            }
        }

        runCatching {
            windowManager.addView(composeView, params)
            islandOwner = owner
            islandView = composeView
        }.onFailure {
            owner.destroy()
        }
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun detachFaceWindow() {
        faceView?.let { runCatching { windowManager.removeView(it) } }
        faceView = null
        faceOwner?.destroy()
        faceOwner = null
    }

    private fun detachIslandWindow() {
        islandView?.let { runCatching { windowManager.removeView(it) } }
        islandView = null
        islandOwner?.destroy()
        islandOwner = null
    }

    fun detachAll() {
        detachFaceWindow()
        detachIslandWindow()
    }
}

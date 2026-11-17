package com.mike.dynamicisland.island

import android.content.Context
import com.mike.dynamicisland.core.MikeEngine

/**
 * Façade de compatibilité déléguant exclusivement à DynamicIslandWindow (V7).
 * Garantit qu'aucune fenêtre plein écran ni FLAG_LAYOUT_NO_LIMITS ne subsiste dans le projet.
 */
class IslandWindowManager(
    context: Context,
    engine: MikeEngine,
    onFaceSuccessHaptic: () -> Unit
) {
    private val delegate = DynamicIslandWindow(
        context = context,
        engine = engine,
        onFaceSuccessHaptic = onFaceSuccessHaptic
    )

    fun syncOverlayVisibility(
        shouldShowFaceId: Boolean,
        shouldShowIsland: Boolean,
        isIslandExpanded: Boolean = false
    ) {
        delegate.syncOverlayVisibility(
            shouldShowFaceId = shouldShowFaceId,
            shouldShowIsland = shouldShowIsland,
            isIslandExpanded = isIslandExpanded
        )
    }

    fun detachAll() {
        delegate.detachAll()
    }
}

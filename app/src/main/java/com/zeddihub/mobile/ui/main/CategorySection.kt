package com.zeddihub.mobile.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.remote.dto.HomeCategoryDto
import com.zeddihub.mobile.data.remote.dto.HomeItemDto
import com.zeddihub.mobile.data.remote.dto.HomeShortcutDto

/**
 * Renders a single admin-defined home category as:
 *   1) A section header (localized category name)
 *   2) A 4-column grid of items (tiles + folder summaries mixed)
 *   3) For each currently-expanded folder, a full-width expanded panel
 *      holding its child tiles in a 3-column grid.
 *
 * Rendered inside a verticalScroll, therefore NO Lazy* containers —
 * all grids are built with manually chunked Rows. Unresolvable
 * `nav_id`s (or empty folders) are silently skipped so the admin can
 * reference tiles that aren't shipped in the current app build without
 * breaking the UI.
 */
@Composable
fun CategorySection(
    category: HomeCategoryDto,
    language: LanguageCode,
    expanded: SnapshotStateMap<String, Boolean>,
    onNavigate: (String) -> Unit,
) {
    // Visible, known items only. Folder slugs must be non-blank so
    // the expand-state map has a stable key; we fall back to the
    // item's position-derived key if the admin left slug empty.
    val renderable = category.items.filter { item ->
        if (item.isFolder) {
            item.tiles.any { it.visible && resolveNavRoute(it.navId) != null }
        } else {
            resolveNavRoute(item.navId) != null
        }
    }
    if (renderable.isEmpty()) return

    val title = pickCategoryName(language, category.nameCs, category.nameEn)
    if (title.isNotBlank()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontWeight = FontWeight.SemiBold
        )
    }

    // 4-column grid. Chunk into rows so we can sit inside verticalScroll
    // without a Lazy* container (which would crash the measurement pass).
    val columns = 4
    val rows = renderable.chunked(columns)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEachIndexed { rowIdx, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEachIndexed { colIdx, item ->
                    val key = folderKey(category.slug, rowIdx, colIdx, item)
                    if (item.isFolder) {
                        val isOpen = expanded[key] == true
                        FolderTile(
                            label = pickCategoryName(language, item.nameCs, item.nameEn),
                            icon = resolveIcon(item.icon),
                            tint = parseHexColor(
                                item.color,
                                MaterialTheme.colorScheme.primary
                            ),
                            expanded = isOpen,
                            onToggle = { expanded[key] = !isOpen },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val route = resolveNavRoute(item.navId)
                        if (route != null) {
                            QuickTileMirror(
                                label = pickCategoryName(
                                    language,
                                    item.labelCs,
                                    item.labelEn
                                ),
                                icon = resolveIcon(item.icon),
                                tint = parseHexColor(
                                    item.color,
                                    MaterialTheme.colorScheme.primary
                                ),
                                onClick = { onNavigate(route) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                // Pad the last row with invisible spacers so the cells
                // keep their expected width.
                repeat(columns - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }

            // After this row, drop in expanded panels for any folders
            // that live in it. We deliberately render panels per-row so
            // the opened drawer appears directly under its parent tile
            // instead of at the very bottom of the category.
            rowItems.forEachIndexed { colIdx, item ->
                if (!item.isFolder) return@forEachIndexed
                val key = folderKey(category.slug, rowIdx, colIdx, item)
                val isOpen = expanded[key] == true
                AnimatedVisibility(
                    visible = isOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    FolderExpandedPanel(
                        tiles = item.tiles,
                        language = language,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

/**
 * Folder summary tile — same visual footprint as QuickTile but with a
 * chevron indicator to hint at the akordeon behaviour. The chevron
 * rotates 180° on expand/collapse via animateFloatAsState, and the
 * whole tile triggers a light haptic pulse on click so the accordion
 * feels physical.
 */
@Composable
private fun FolderTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "chevronRotation"
    )
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            },
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(Modifier.size(2.dp))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(chevronAngle)
                )
            }
        }
    }
}

/**
 * Full-width panel revealed when a folder is expanded. Renders the
 * folder's child `HomeShortcutDto`s as a 3-column chunked grid — no
 * Lazy* container because we're inside verticalScroll. Unresolvable
 * tiles are filtered out.
 */
@Composable
private fun FolderExpandedPanel(
    tiles: List<HomeShortcutDto>,
    language: LanguageCode,
    onNavigate: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val resolvable = tiles
        .filter { it.visible }
        .mapNotNull { sc -> resolveNavRoute(sc.navId)?.let { sc to it } }
    if (resolvable.isEmpty()) return

    val columns = 3
    val rows = resolvable.chunked(columns)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = colors.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiles.forEach { (sc, route) ->
                        QuickTileMirror(
                            label = pickCategoryName(language, sc.labelCs, sc.labelEn),
                            icon = resolveIcon(sc.icon),
                            tint = parseHexColor(sc.color, colors.primary),
                            onClick = { onNavigate(route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowTiles.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Local mirror of DashboardScreen's private `QuickTile`. Duplicated
 * (instead of making the original internal) so this file stays
 * self-contained and the Dashboard's rewrite stays surgical.
 */
@Composable
private fun QuickTileMirror(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/** CS-first with EN fallback, mirroring DashboardScreen.pickLocalized. */
private fun pickCategoryName(language: LanguageCode, cs: String, en: String): String = when {
    language == LanguageCode.CS && cs.isNotBlank() -> cs
    language == LanguageCode.EN && en.isNotBlank() -> en
    cs.isNotBlank() -> cs
    else -> en
}

/**
 * Stable per-folder key for the expand-state map. Admins usually
 * provide a `slug`; if they don't we fall back to position so at least
 * two unnamed folders inside the same category don't toggle together.
 */
private fun folderKey(
    categorySlug: String,
    rowIdx: Int,
    colIdx: Int,
    item: HomeItemDto,
): String {
    val base = if (item.slug.isNotBlank()) item.slug else "r${rowIdx}c$colIdx"
    return "$categorySlug/$base"
}

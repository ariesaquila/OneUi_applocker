package com.oneui.applocker.ui.home

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.oneui.applocker.R
import com.oneui.applocker.core.designsystem.OneUiCard
import com.oneui.applocker.core.designsystem.OneUiFilterRow
import com.oneui.applocker.core.designsystem.OneUiHeader
import com.oneui.applocker.core.designsystem.OneUiSearchBar
import com.oneui.applocker.core.designsystem.OneUiSwitch
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiOrange
import com.oneui.applocker.core.theme.OneUiRed
import com.oneui.applocker.data.model.AppItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to the top whenever the selected category tab or search query changes
    LaunchedEffect(selectedFilter, searchQuery) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // One UI Viewing Area Header
        OneUiHeader(
            title = stringResource(R.string.title_home),
            subtitle = stringResource(R.string.subtitle_home, uiState.lockedCount),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.title_settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )

        // Missing permissions alert banner
        AnimatedVisibility(visible = !uiState.hasRequiredPermissions) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(OneUiOrange.copy(alpha = 0.15f))
                    .clickable(onClick = onNavigateToPermissions)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = null,
                        tint = OneUiOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_missing_permissions_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.home_missing_permissions_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_missing_permissions_action),
                        style = MaterialTheme.typography.labelLarge,
                        color = OneUiBlue
                    )
                }
            }
        }

        // Search Bar
        OneUiSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Filters (Hepsi, İndirilenler, Sistem, Kilitli)
        val filterOptions = listOf(
            Triple(stringResource(R.string.filter_all), selectedFilter == 0, uiState.totalCount),
            Triple(stringResource(R.string.filter_downloaded), selectedFilter == 1, uiState.downloadedCount),
            Triple(stringResource(R.string.filter_system), selectedFilter == 2, uiState.systemCount),
            Triple(stringResource(R.string.filter_locked), selectedFilter == 3, uiState.lockedCount)
        )

        OneUiFilterRow(
            filters = filterOptions,
            onFilterSelected = viewModel::onFilterSelect,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        // Quick bulk actions (Lock all / Unlock all) + App count indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.apps_count_suffix, uiState.apps.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row {
                TextButton(onClick = viewModel::lockAll) {
                    Text(
                        text = stringResource(R.string.lock_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = OneUiBlue
                    )
                }
                TextButton(onClick = viewModel::unlockAll) {
                    Text(
                        text = stringResource(R.string.unlock_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = OneUiRed
                    )
                }
            }
        }

        // Applications List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OneUiBlue)
            }
        } else if (uiState.apps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.apps,
                    key = { it.packageName }
                ) { app ->
                    AppListItem(
                        app = app,
                        onToggle = { viewModel.toggleAppLock(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppItem,
    onToggle: () -> Unit
) {
    OneUiCard(
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    val bitmap = rememberAppBitmap(app.packageName, app.icon)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.appName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DefaultAppIcon()
                    }
                } else {
                    DefaultAppIcon()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App Name & Badges
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.isSystemApp) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.system_app_tag),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // One UI Switch
            OneUiSwitch(
                checked = app.isLocked,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun DefaultAppIcon() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBlue.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_shield),
            contentDescription = null,
            tint = OneUiBlue,
            modifier = Modifier.size(24.dp)
        )
    }
}

private val appBitmapCache = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(200)

@Composable
private fun rememberAppBitmap(packageName: String, drawable: Drawable): androidx.compose.ui.graphics.ImageBitmap? {
    return androidx.compose.runtime.remember(packageName) {
        val cached = appBitmapCache.get(packageName)
        if (cached != null) {
            cached
        } else {
            try {
                val bitmap = drawable.toBitmap(width = 96, height = 96).asImageBitmap()
                appBitmapCache.put(packageName, bitmap)
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
}

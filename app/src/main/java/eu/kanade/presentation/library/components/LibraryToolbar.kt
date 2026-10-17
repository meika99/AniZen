package eu.kanade.presentation.library.components

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.runOnEnterKeyPressed
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomEntry: () -> Unit,
    onClickSyncNow: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    navigateUp: (() -> Unit)? = null,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomEntry = onClickOpenRandomEntry,
        onClickSyncNow = onClickSyncNow,
        scrollBehavior = scrollBehavior,
        navigateUp = navigateUp,
    )
}

// ANZ -->
/*
 * Toolbar for the new library home: bold title with an accent counter, the actions grouped in one
 * soft glass capsule, and a background that takes the hero's ambient tint (see
 * [LibraryHomeAmbient]) so the bar and the screen under it look like a single surface.
 */
@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomEntry: () -> Unit,
    onClickSyncNow: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    navigateUp: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val capsuleAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f

    val ambient = LibraryHomeAmbient.color
    // Never fades all the way to plain surface: scrolling keeps a part of the tint so the bar does
    // not jump to black, and a hairline at the bottom edge appears as the content slides under it.
    val tinted = if (ambient != null) {
        lerp(ambient, colors.surface, LibraryHomeAmbient.fade * ToolbarMaxFade)
    } else {
        null
    }
    val hairline = colors.onSurface

    // The app bar paints `surface`, so overriding it here is enough to tint the bar.
    MaterialTheme(colorScheme = if (tinted != null) colors.copy(surface = tinted) else colors) {
        Box(
            modifier = Modifier.drawWithContent {
                drawContent()
                val edge = LibraryHomeAmbient.fade
                if (edge > 0f) {
                    val y = size.height - 0.5.dp.toPx()
                    drawLine(
                        color = hairline.copy(alpha = 0.10f * edge),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
        ) {
        if (searchQuery != null) {
            LibrarySearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                hasFilters = hasFilters,
                onClickFilter = onClickFilter,
                onClickRefresh = onClickRefresh,
                onClickGlobalUpdate = onClickGlobalUpdate,
                onClickOpenRandomEntry = onClickOpenRandomEntry,
                onClickSyncNow = onClickSyncNow,
                scrollBehavior = scrollBehavior,
            )
        } else {
        SearchToolbar(
            titleContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title.text,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, false),
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    if (title.numberOfEntries != null) {
                        Text(
                            text = "${title.numberOfEntries}",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(colors.primary.copy(alpha = 0.18f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            color = colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            },
            searchQuery = searchQuery,
            onChangeSearchQuery = onSearchQueryChange,
            searchEnabled = false,
            actions = {
                val filterTint = if (hasFilters) colors.active else LocalContentColor.current
                val actionList = persistentListOf<AppBar.AppBarAction>(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = Icons.Outlined.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_library),
                        onClick = onClickGlobalUpdate,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_category),
                        onClick = onClickRefresh,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_open_random_manga),
                        onClick = onClickOpenRandomEntry,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(SYMR.strings.sync_library),
                        onClick = onClickSyncNow,
                    ),
                )
                if (searchQuery == null) {
                    Row(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(colors.onBackground.copy(alpha = capsuleAlpha)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(MR.strings.action_search),
                            )
                        }
                        AppBarActions(actionList)
                    }
                } else {
                    AppBarActions(actionList)
                }
            },
            scrollBehavior = scrollBehavior,
            navigateUp = navigateUp,
        )
        }
        }
    }
}

private const val ToolbarMaxFade = 0.55f

/** Search mode: the query lives in a rounded pill with the search glyph and a clear button. */
@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String?) -> Unit,
    hasFilters: Boolean,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomEntry: () -> Unit,
    onClickSyncNow: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val colors = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val requestKeyboard = rememberSaveable { mutableStateOf(query.isEmpty()) }
    LaunchedEffect(focusRequester) {
        if (requestKeyboard.value) {
            focusRequester.requestFocus()
            requestKeyboard.value = false
        }
    }
    val hideKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    AppBar(
        titleContent = {
            // Same workaround as the stock search bar: a harmless place for bounced focus.
            Box(modifier = Modifier.size(1.dp).focusable())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface.copy(alpha = 0.09f))
                    .border(1.dp, colors.primary.copy(alpha = 0.28f), CircleShape)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.primary,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(MR.strings.action_search_hint),
                            color = colors.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .runOnEnterKeyPressed(action = hideKeyboard)
                            .clearFocusOnSoftKeyboardHide(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { hideKeyboard() }),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.primary),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            focusRequester.requestFocus()
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(MR.strings.action_reset),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
        navigateUp = { onQueryChange(null) },
        actions = {
            val filterTint = if (hasFilters) colors.active else LocalContentColor.current
            AppBarActions(
                persistentListOf<AppBar.AppBarAction>(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = Icons.Outlined.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_library),
                        onClick = onClickGlobalUpdate,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_category),
                        onClick = onClickRefresh,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_open_random_manga),
                        onClick = onClickOpenRandomEntry,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(SYMR.strings.sync_library),
                        onClick = onClickSyncNow,
                    ),
                ),
            )
        },
        isActionMode = false,
        scrollBehavior = scrollBehavior,
    )
}
// ANZ <--

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfEntries: Int? = null,
)

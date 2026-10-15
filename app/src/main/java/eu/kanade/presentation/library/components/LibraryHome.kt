package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.tachiyomi.ui.library.LibraryDisplayItem
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.util.system.CoverColorObserver
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.anime.model.asAnimeCover
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.domain.library.model.LibraryDisplayMode

// ANZ -->
/*
 * New "home" style for the library: a poster hero carousel tinted with the cover's own colour,
 * Netflix/Crunchyroll style shelves and a modern poster grid.
 *
 * It only replaces the classic grids; search results and the List display mode still use the
 * classic layouts (see [shouldUseLibraryHome]).
 */

/** Whether the library page should render the new home layout instead of the classic grid. */
internal fun shouldUseLibraryHome(searchQuery: String?, displayMode: LibraryDisplayMode): Boolean {
    return searchQuery.isNullOrEmpty() && displayMode != LibraryDisplayMode.List
}

private enum class LibraryHomeTab(val label: String) {
    Home("Inicio"),
    All("Todos"),
    Watching("En curso"),
    NotStarted("Sin empezar"),
    Finished("Completados"),
}

private val EdgePadding = 16.dp
private val CardGap = 10.dp
private val CardWidth = 116.dp
private val WideCardWidth = 148.dp
private val PosterRadius = 14.dp
private val TagRadius = 10.dp
private val ShelfSpacing = 22.dp
private val BottomSpace = 28.dp
private val HeroRadius = 22.dp
private val HeroSpacing = 12.dp
private val HeroMaxWidth = 280.dp
private val RankZone = 52.dp
private val RankFont = 112.sp

private const val HeroWidthFraction = 0.62f
private const val AmbientStrength = 0.5f
private const val AmbientFallback = 0.14f
private const val SelectedScale = 0.95f
private const val ColumnsPortrait = 3
private const val ColumnsLandscape = 6
private const val HeroCount = 5
private const val MaxShelfItems = 20
private const val MinTopItems = 3
private const val MinGenreItems = 3
private const val MaxGenreShelves = 3
private const val RankStroke = 6f

@Immutable
private class HomeActions(
    val onClick: (LibraryAnime) -> Unit,
    val onLongClick: (LibraryAnime) -> Unit,
    val onPlay: ((LibraryAnime) -> Unit)?,
    val onFolderClick: ((LibraryDisplayItem.Folder) -> Unit)?,
    val onFolderLongClick: ((LibraryDisplayItem.Folder) -> Unit)?,
    val onOpenTab: (LibraryHomeTab) -> Unit,
)

@Immutable
private class GenreShelf(val name: String, val entries: ImmutableList<LibraryItem>)

@Immutable
private class Shelves(
    val heroes: ImmutableList<LibraryItem>,
    val watching: ImmutableList<LibraryItem>,
    val notStarted: ImmutableList<LibraryItem>,
    val recent: ImmutableList<LibraryItem>,
    val top: ImmutableList<LibraryItem>,
    val finished: ImmutableList<LibraryItem>,
    val genres: ImmutableList<GenreShelf>,
)

private sealed interface GridRow {
    class Title(val name: String) : GridRow
    class Cells(val cells: List<LibraryDisplayItem>) : GridRow
}

private fun LibraryAnime.isWatching(): Boolean = hasStarted && unseenCount > 0

private fun LibraryAnime.isFinished(): Boolean = hasStarted && unseenCount <= 0 && totalEpisodes > 0

private fun flattenAnime(items: List<LibraryDisplayItem>): List<LibraryItem> {
    return items.flatMap { item ->
        when (item) {
            is LibraryDisplayItem.Anime -> listOf(item.libraryItem)
            is LibraryDisplayItem.Folder -> item.items
            is LibraryDisplayItem.Header -> emptyList()
        }
    }
}

private fun buildShelves(all: List<LibraryItem>): Shelves {
    val watching = all
        .filter { it.libraryAnime.isWatching() }
        .sortedByDescending { it.libraryAnime.lastSeen }
    val notStarted = all.filter { !it.libraryAnime.hasStarted }
    val recent = all.sortedByDescending { it.libraryAnime.anime.dateAdded }
    val top = all
        .filter { it.libraryAnime.seenCount > 0 }
        .sortedByDescending { it.libraryAnime.seenCount }
    val finished = all.filter { it.libraryAnime.isFinished() }

    val heroes = (
        watching.take(3) +
            notStarted.sortedByDescending { it.libraryAnime.anime.dateAdded }.take(3) +
            all.sortedByDescending { it.libraryAnime.lastSeen }
        )
        .distinctBy { it.libraryAnime.id }
        .take(HeroCount)

    val byGenre = LinkedHashMap<String, MutableList<LibraryItem>>()
    val names = HashMap<String, String>()
    all.forEach { item ->
        item.libraryAnime.anime.genre.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .forEach { genre ->
                val key = genre.lowercase()
                names.putIfAbsent(key, genre.replaceFirstChar { it.uppercase() })
                byGenre.getOrPut(key) { ArrayList() }.add(item)
            }
    }
    val genres = byGenre.entries
        .filter { it.value.size >= MinGenreItems && (it.value.size < all.size || all.size <= MinGenreItems) }
        .sortedByDescending { it.value.size }
        .take(MaxGenreShelves)
        .map { GenreShelf(names[it.key].orEmpty(), it.value.take(MaxShelfItems).toImmutableList()) }

    return Shelves(
        heroes = heroes.toImmutableList(),
        watching = watching.take(MaxShelfItems).toImmutableList(),
        notStarted = notStarted.take(MaxShelfItems).toImmutableList(),
        recent = recent.take(MaxShelfItems).toImmutableList(),
        top = top.take(HeroCount).toImmutableList(),
        finished = finished.take(MaxShelfItems).toImmutableList(),
        genres = genres.toImmutableList(),
    )
}

private fun buildGridRows(
    tab: LibraryHomeTab,
    items: List<LibraryDisplayItem>,
    flat: List<LibraryItem>,
    columns: Int,
): List<GridRow> {
    val cells: List<LibraryDisplayItem> = when (tab) {
        LibraryHomeTab.Home -> emptyList()
        LibraryHomeTab.All -> items
        LibraryHomeTab.Watching -> flat
            .filter { it.libraryAnime.isWatching() }
            .sortedByDescending { it.libraryAnime.lastSeen }
            .map { LibraryDisplayItem.Anime(it) }
        LibraryHomeTab.NotStarted -> flat
            .filter { !it.libraryAnime.hasStarted }
            .map { LibraryDisplayItem.Anime(it) }
        LibraryHomeTab.Finished -> flat
            .filter { it.libraryAnime.isFinished() }
            .map { LibraryDisplayItem.Anime(it) }
    }

    val rows = ArrayList<GridRow>()
    var current = ArrayList<LibraryDisplayItem>()
    fun flush() {
        if (current.isNotEmpty()) {
            rows.add(GridRow.Cells(current))
            current = ArrayList()
        }
    }
    for (cell in cells) {
        if (cell is LibraryDisplayItem.Header) {
            flush()
            rows.add(GridRow.Title(cell.name))
        } else {
            current.add(cell)
            if (current.size == columns) flush()
        }
    }
    flush()
    return rows
}

private fun emptyMessage(tab: LibraryHomeTab): String = when (tab) {
    LibraryHomeTab.Watching -> "No tienes series en curso. Empieza una desde Inicio."
    LibraryHomeTab.NotStarted -> "Ya empezaste todas tus series."
    LibraryHomeTab.Finished -> "Todavía no terminaste ninguna serie."
    LibraryHomeTab.Home, LibraryHomeTab.All -> "No hay series para mostrar."
}

@Composable
internal fun LibraryHome(
    items: ImmutableList<LibraryDisplayItem>,
    contentPadding: PaddingValues,
    selection: ImmutableList<LibraryAnime>,
    onClick: (LibraryAnime) -> Unit,
    onLongClick: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
    onFolderClick: ((LibraryDisplayItem.Folder) -> Unit)?,
    onFolderLongClick: ((LibraryDisplayItem.Folder) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val flat = remember(items) { flattenAnime(items) }
    val shelves = remember(flat) { buildShelves(flat) }
    val selectedIds = remember(selection) { selection.map { it.id }.toSet() }

    var tabIndex by rememberSaveable { mutableStateOf(0) }
    val tab = LibraryHomeTab.entries[tabIndex.coerceIn(0, LibraryHomeTab.entries.lastIndex)]

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) ColumnsLandscape else ColumnsPortrait

    val heroPager = rememberPagerState { shelves.heroes.size }
    val listState = rememberLazyListState()
    val gridRows = remember(tab, items, flat, columns) { buildGridRows(tab, items, flat, columns) }

    val actions = remember(onClick, onLongClick, onClickContinueWatching, onFolderClick, onFolderLongClick) {
        HomeActions(
            onClick = onClick,
            onLongClick = onLongClick,
            onPlay = onClickContinueWatching,
            onFolderClick = onFolderClick,
            onFolderLongClick = onFolderLongClick,
            onOpenTab = { tabIndex = it.ordinal },
        )
    }

    LaunchedEffect(tab) { listState.scrollToItem(0) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + BottomSpace),
    ) {
        item(key = "top", contentType = "top") {
            HomeTop(
                tab = tab,
                heroes = shelves.heroes,
                pagerState = heroPager,
                selectedIds = selectedIds,
                actions = actions,
                onTabSelected = { tabIndex = it.ordinal },
            )
        }
        if (tab == LibraryHomeTab.Home) {
            homeShelves(shelves = shelves, total = flat.size, selectedIds = selectedIds, actions = actions)
        } else {
            gridSection(tab = tab, rows = gridRows, columns = columns, selectedIds = selectedIds, actions = actions)
        }
    }
}

// region Sections

private fun LazyListScope.homeShelves(
    shelves: Shelves,
    total: Int,
    selectedIds: Set<Long>,
    actions: HomeActions,
) {
    shelf("watching", "Continuar viendo", shelves.watching, selectedIds, actions, wide = true, seeAll = LibraryHomeTab.Watching)
    shelf("not-started", "Por empezar", shelves.notStarted, selectedIds, actions, seeAll = LibraryHomeTab.NotStarted)
    if (shelves.top.size >= MinTopItems) {
        item(key = "shelf-top", contentType = "ranked") {
            RankedShelf(
                title = "Tu top ${shelves.top.size}",
                entries = shelves.top,
                selectedIds = selectedIds,
                actions = actions,
                modifier = Modifier.padding(top = ShelfSpacing),
            )
        }
    }
    shelf("recent", "Agregados recientemente", shelves.recent, selectedIds, actions)
    shelves.genres.forEach { genre ->
        shelf("genre-${genre.name}", genre.name, genre.entries, selectedIds, actions)
    }
    shelf("finished", "Completados", shelves.finished, selectedIds, actions, seeAll = LibraryHomeTab.Finished)
    item(key = "see-all", contentType = "footer") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ShelfSpacing + 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            FilledTonalButton(onClick = { actions.onOpenTab(LibraryHomeTab.All) }) {
                Text("Ver toda la biblioteca ($total)")
            }
        }
    }
}

private fun LazyListScope.shelf(
    key: String,
    title: String,
    entries: ImmutableList<LibraryItem>,
    selectedIds: Set<Long>,
    actions: HomeActions,
    wide: Boolean = false,
    seeAll: LibraryHomeTab? = null,
) {
    if (entries.isEmpty()) return
    item(key = "shelf-$key", contentType = "shelf") {
        Shelf(
            title = title,
            entries = entries,
            selectedIds = selectedIds,
            actions = actions,
            modifier = Modifier.padding(top = ShelfSpacing),
            wide = wide,
            seeAll = seeAll,
        )
    }
}

private fun LazyListScope.gridSection(
    tab: LibraryHomeTab,
    rows: List<GridRow>,
    columns: Int,
    selectedIds: Set<Long>,
    actions: HomeActions,
) {
    if (rows.isEmpty()) {
        item(key = "empty-${tab.name}", contentType = "empty") {
            Text(
                text = emptyMessage(tab),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EdgePadding, vertical = ShelfSpacing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    itemsIndexed(
        items = rows,
        key = { index, _ -> "grid-${tab.name}-$index" },
        contentType = { _, row -> if (row is GridRow.Title) "grid-title" else "grid-cells" },
    ) { _, row ->
        when (row) {
            is GridRow.Title -> Text(
                text = row.name,
                modifier = Modifier.padding(horizontal = EdgePadding, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            is GridRow.Cells -> GridRowContent(
                cells = row.cells,
                columns = columns,
                selectedIds = selectedIds,
                actions = actions,
            )
        }
    }
}

// endregion

// region Top block (tabs + hero)

@Composable
private fun HomeTop(
    tab: LibraryHomeTab,
    heroes: ImmutableList<LibraryItem>,
    pagerState: PagerState,
    selectedIds: Set<Long>,
    actions: HomeActions,
    onTabSelected: (LibraryHomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vibrant by CoverColorObserver.vibrantColors.collectAsState()
    val background = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary

    val current = if (tab == LibraryHomeTab.Home) heroes.getOrNull(pagerState.currentPage) else null
    val seed = current?.let { vibrant[it.libraryAnime.id] }?.let { Color(it) }
    val ambientTarget = if (seed != null) {
        lerp(background, seed, AmbientStrength)
    } else {
        lerp(background, accent, AmbientFallback)
    }
    val ambient by animateColorAsState(targetValue = ambientTarget, label = "libraryAmbient")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(ambient, background))),
    ) {
        TabChips(selected = tab, onSelected = onTabSelected)
        if (current != null) {
            Hero(
                heroes = heroes,
                pagerState = pagerState,
                current = current,
                selectedIds = selectedIds,
                actions = actions,
            )
        }
    }
}

@Composable
private fun TabChips(
    selected: LibraryHomeTab,
    onSelected: (LibraryHomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = EdgePadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(LibraryHomeTab.entries, key = { it.name }) { tab ->
            HomeChip(label = tab.label, selected = tab == selected, onClick = { onSelected(tab) })
        }
    }
}

@Composable
private fun HomeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Text(
        text = label,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) onBackground else onBackground.copy(alpha = 0.12f))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = if (selected) MaterialTheme.colorScheme.background else onBackground,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    )
}

@Composable
private fun Hero(
    heroes: ImmutableList<LibraryItem>,
    pagerState: PagerState,
    current: LibraryItem,
    selectedIds: Set<Long>,
    actions: HomeActions,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = minOf(maxWidth * HeroWidthFraction, HeroMaxWidth)
        val sidePadding = (maxWidth - cardWidth) / 2

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSpacing = HeroSpacing,
                key = { index -> heroes.getOrNull(index)?.libraryAnime?.id ?: index },
            ) { page ->
                val item = heroes[page]
                val entry = item.libraryAnime
                HeroCard(
                    item = item,
                    isSelected = entry.id in selectedIds,
                    onClick = { actions.onClick(entry) },
                    onLongClick = { actions.onLongClick(entry) },
                    modifier = Modifier.graphicsLayer {
                        val distance = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue
                            .coerceIn(0f, 1f)
                        val scale = 1f - 0.08f * distance
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.45f * distance
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
            HeroInfo(item = current, actions = actions)
            Spacer(Modifier.height(14.dp))
            PagerDots(pagerState = pagerState, count = heroes.size)
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCard(
    item: LibraryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = item.libraryAnime
    val anime = entry.anime
    val cover = remember(anime.id, anime.thumbnailUrl, anime.coverLastModified) { anime.asAnimeCover() }
    val shape = RoundedCornerShape(HeroRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        // The hero cover is the one that seeds the ambient colour of the whole screen.
        AnimeCover.Book(
            data = cover,
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            shouldExtractColor = true,
            measureRatio = false,
        )
        if (entry.unseenCount > 0) {
            UnseenTag(count = entry.unseenCount, modifier = Modifier.align(Alignment.TopStart))
        }
        if (entry.hasStarted && entry.totalEpisodes > 0) {
            ProgressStrip(
                progress = progressOf(entry),
                height = 5.dp,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
        if (isSelected) {
            SelectedMark(modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun HeroInfo(item: LibraryItem, actions: HomeActions, modifier: Modifier = Modifier) {
    val entry = item.libraryAnime
    val anime = entry.anime
    val play = actions.onPlay
    val canPlay = play != null && entry.unseenCount > 0
    val label = when {
        !canPlay -> "Ver detalles"
        entry.hasStarted -> "Continuar viendo"
        else -> "Empezar a ver"
    }
    val onBackground = MaterialTheme.colorScheme.onBackground
    val genres = anime.genre.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.take(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = anime.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.totalEpisodes > 0) {
            Text(
                text = "${entry.seenCount} de ${entry.totalEpisodes} episodios vistos",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = onBackground.copy(alpha = 0.7f),
            )
        }
        if (genres.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                genres.forEach { genre ->
                    Text(
                        text = genre,
                        modifier = Modifier
                            .background(onBackground.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = onBackground.copy(alpha = 0.9f),
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    if (canPlay && play != null) play(entry) else actions.onClick(entry)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            ) {
                if (canPlay) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = label, fontWeight = FontWeight.Bold)
            }
            FilledTonalIconButton(onClick = { actions.onClick(entry) }) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = "Detalles")
            }
        }
    }
}

@Composable
private fun PagerDots(pagerState: PagerState, count: Int, modifier: Modifier = Modifier) {
    if (count > 1) {
        val onBackground = MaterialTheme.colorScheme.onBackground
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(count) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(targetValue = if (selected) 18.dp else 6.dp, label = "heroDot")
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(onBackground.copy(alpha = if (selected) 0.9f else 0.3f)),
                )
            }
        }
    }
}

// endregion

// region Shelves

@Composable
private fun Shelf(
    title: String,
    entries: ImmutableList<LibraryItem>,
    selectedIds: Set<Long>,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    seeAll: LibraryHomeTab? = null,
) {
    Column(modifier = modifier) {
        ShelfHeader(
            title = title,
            onSeeAll = if (seeAll != null) ({ actions.onOpenTab(seeAll) }) else null,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = EdgePadding),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            items(entries, key = { it.libraryAnime.id }) { item ->
                val entry = item.libraryAnime
                val play = actions.onPlay
                PosterCard(
                    item = item,
                    isSelected = entry.id in selectedIds,
                    onClick = { actions.onClick(entry) },
                    onLongClick = { actions.onLongClick(entry) },
                    modifier = Modifier.width(if (wide) WideCardWidth else CardWidth),
                    showProgress = wide,
                    subtitle = if (wide && entry.totalEpisodes > 0) {
                        "${entry.seenCount} de ${entry.totalEpisodes} episodios"
                    } else {
                        null
                    },
                    onPlay = if (wide && play != null) ({ play(entry) }) else null,
                )
            }
        }
    }
}

@Composable
private fun ShelfHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EdgePadding)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onSeeAll != null) {
            Text(
                text = "Ver todo",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RankedShelf(
    title: String,
    entries: ImmutableList<LibraryItem>,
    selectedIds: Set<Long>,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val numeral = TextStyle(
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        fontSize = RankFont,
        fontWeight = FontWeight.Black,
        lineHeight = RankFont,
        drawStyle = Stroke(width = RankStroke, join = StrokeJoin.Round),
    )
    Column(modifier = modifier) {
        ShelfHeader(title = title, onSeeAll = null)
        LazyRow(
            contentPadding = PaddingValues(horizontal = EdgePadding),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            itemsIndexed(entries, key = { _, item -> item.libraryAnime.id }) { index, item ->
                val entry = item.libraryAnime
                Box(modifier = Modifier.width(RankZone + CardWidth)) {
                    Text(
                        text = (index + 1).toString(),
                        modifier = Modifier.align(Alignment.BottomStart),
                        style = numeral,
                    )
                    PosterCard(
                        item = item,
                        isSelected = entry.id in selectedIds,
                        onClick = { actions.onClick(entry) },
                        onLongClick = { actions.onLongClick(entry) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(CardWidth),
                        showTitle = false,
                    )
                }
            }
        }
    }
}

// endregion

// region Grid ("Todos", "En curso", ...)

@Composable
private fun GridRowContent(
    cells: List<LibraryDisplayItem>,
    columns: Int,
    selectedIds: Set<Long>,
    actions: HomeActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EdgePadding)
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        cells.forEach { cell ->
            Box(modifier = Modifier.weight(1f)) {
                when (cell) {
                    is LibraryDisplayItem.Anime -> {
                        val entry = cell.libraryItem.libraryAnime
                        PosterCard(
                            item = cell.libraryItem,
                            isSelected = entry.id in selectedIds,
                            onClick = { actions.onClick(entry) },
                            onLongClick = { actions.onLongClick(entry) },
                            modifier = Modifier.fillMaxWidth(),
                            titleLines = 2,
                        )
                    }
                    is LibraryDisplayItem.Folder -> {
                        FolderGridItem(
                            folder = cell,
                            displayMode = LibraryDisplayMode.ComfortableGrid,
                            onClick = { actions.onFolderClick?.invoke(cell) },
                            onLongClick = { actions.onFolderLongClick?.invoke(cell) },
                            usePanorama = false,
                        )
                    }
                    is LibraryDisplayItem.Header -> Unit
                }
            }
        }
        repeat(columns - cells.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// endregion

// region Poster card

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterCard(
    item: LibraryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    showTitle: Boolean = true,
    titleLines: Int = 1,
    subtitle: String? = null,
    onPlay: (() -> Unit)? = null,
) {
    val entry = item.libraryAnime
    val anime = entry.anime
    val cover = remember(anime.id, anime.thumbnailUrl, anime.coverLastModified) { anime.asAnimeCover() }
    val shape = RoundedCornerShape(PosterRadius)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .graphicsLayer {
                val scale = if (isSelected) SelectedScale else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) colors.primary else colors.onBackground.copy(alpha = 0.08f),
                    shape = shape,
                ),
        ) {
            AnimeCover.Book(
                data = cover,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                shouldExtractColor = false,
                measureRatio = false,
            )
            if (entry.unseenCount > 0) {
                UnseenTag(count = entry.unseenCount, modifier = Modifier.align(Alignment.TopStart))
            }
            if (showProgress && entry.hasStarted && entry.totalEpisodes > 0) {
                ProgressStrip(
                    progress = progressOf(entry),
                    height = 4.dp,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
            if (onPlay != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 12.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(role = Role.Button, onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Continuar viendo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (isSelected) {
                SelectedMark(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        if (showTitle) {
            Text(
                text = anime.title,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun progressOf(entry: LibraryAnime): Float {
    if (entry.totalEpisodes <= 0) return 0f
    return (entry.seenCount.toFloat() / entry.totalEpisodes).coerceIn(0f, 1f)
}

@Composable
private fun UnseenTag(count: Long, modifier: Modifier = Modifier) {
    Text(
        text = count.toString(),
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = TagRadius))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ProgressStrip(progress: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.Black.copy(alpha = 0.55f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun SelectedMark(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(6.dp)
            .size(22.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
    )
}

// endregion
// ANZ <--

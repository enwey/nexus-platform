package com.nexus.platform.feature.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.domain.model.DiscoverHeroCard
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted
import kotlin.math.min

private val TopLevelBottomPadding = 96.dp
private val QuickPlayText = TextMain
private const val DiscoverPageSize = 30

data class DiscoverCategoryItem(
    val labelRes: Int,
    val key: String
)

@Composable
fun DiscoverScreen(
    games: List<GameItem>,
    hero: DiscoverHeroCard?,
    onCategoryChange: (String) -> Unit,
    onGameClick: (GameItem) -> Unit,
    onQuickPlayClick: (GameItem) -> Unit
) {
    val categories = remember {
        listOf(
            DiscoverCategoryItem(R.string.discover_category_all, "all"),
            DiscoverCategoryItem(R.string.discover_category_action, "action"),
            DiscoverCategoryItem(R.string.discover_category_casual, "casual"),
            DiscoverCategoryItem(R.string.discover_category_rpg, "rpg")
        )
    }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }

    val rankedGames = remember(games) { games.take(10) }
    val heroTarget = remember(hero, rankedGames) {
        val heroAppId = hero?.appId.orEmpty()
        rankedGames.firstOrNull { it.id == heroAppId } ?: rankedGames.firstOrNull()
    }

    val myGameSource = remember(games) { games }
    var loadedCount by remember(myGameSource) {
        mutableIntStateOf(min(DiscoverPageSize, myGameSource.size))
    }
    val loadedGames = remember(myGameSource, loadedCount) {
        myGameSource.take(loadedCount)
    }
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            if (loadedCount >= myGameSource.size) return@derivedStateOf false
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(shouldLoadMore, loadedCount, myGameSource.size) {
        if (shouldLoadMore) {
            loadedCount = min(loadedCount + DiscoverPageSize, myGameSource.size)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .background(BackgroundBase)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp)
            ) {
                Text(
                    stringResource(R.string.discover_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                Banner(
                    hero = hero,
                    onClick = {
                        heroTarget?.let(onGameClick)
                    }
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                CategoryRow(
                    categories = categories,
                    selectedIndex = selectedCategoryIndex,
                    onSelect = { index ->
                        if (index != selectedCategoryIndex) {
                            selectedCategoryIndex = index
                            loadedCount = 0
                            onCategoryChange(categories[index].key)
                        }
                    }
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                SectionHeader(
                    title = stringResource(R.string.discover_section_rank),
                    action = stringResource(R.string.discover_view_more)
                )
            }
        }

        if (rankedGames.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    Text(
                        text = stringResource(R.string.discover_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(rankedGames.size) { index ->
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    RankedItem(
                        game = rankedGames[index],
                        onGameClick = onGameClick,
                        onQuickPlayClick = onQuickPlayClick
                    )
                }
            }
        }

        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                SectionHeader(
                    title = stringResource(R.string.discover_section_category_all),
                    action = null
                )
            }
        }

        if (myGameSource.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    Text(
                        text = stringResource(R.string.discover_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(
                items = loadedGames,
                key = { game -> "discover_list_${game.id}" }
            ) { game ->
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    RankedItem(
                        game = game,
                        onGameClick = onGameClick,
                        onQuickPlayClick = onQuickPlayClick
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = TopLevelBottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (loadedCount < myGameSource.size) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = stringResource(R.string.library_load_end),
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Banner(hero: DiscoverHeroCard?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
            .clickable { onClick() }
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.discover_banner_hot), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hero?.title?.ifBlank { stringResource(R.string.discover_banner_title) }
                    ?: stringResource(R.string.discover_banner_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            if (!hero?.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hero?.subtitle.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<DiscoverCategoryItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEachIndexed { index, category ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Primary else BackgroundSurface)
                    .border(1.dp, if (selected) Primary else BorderLight, RoundedCornerShape(20.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(category.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (!action.isNullOrBlank()) {
            Text(action, color = Primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RankedItem(
    game: GameItem,
    onGameClick: (GameItem) -> Unit,
    onQuickPlayClick: (GameItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BackgroundSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
            .clickable { onGameClick(game) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                .background(BackgroundSurfaceElevated)
        ) {
            GameLogo(
                iconUrl = game.iconUrl,
                seed = "${game.id}_${game.name}_discover",
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.description.ifBlank { "v${game.version}" },
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
                .clickable { onQuickPlayClick(game) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.discover_quick_play),
                color = QuickPlayText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

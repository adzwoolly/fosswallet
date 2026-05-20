package nz.eloque.foss_wallet.ui.screens.wallet

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.map
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.LocalizedPassWithTags
import nz.eloque.foss_wallet.model.PassType
import nz.eloque.foss_wallet.model.Tag
import nz.eloque.foss_wallet.ui.Screen
import nz.eloque.foss_wallet.ui.card.ShortPassCard
import nz.eloque.foss_wallet.ui.components.GroupCard
import nz.eloque.foss_wallet.ui.components.SwipeToDismiss

private sealed interface WalletItem {
    data class Group(
        val groupId: Long,
        val passes: List<LocalizedPassWithTags>,
    ) : WalletItem

    data class Single(
        val pass: LocalizedPassWithTags,
    ) : WalletItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletView(
    navController: NavController,
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector = Icons.Default.Wallet,
    archive: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    selectedPasses: SnapshotStateSet<LocalizedPassWithTags>,
    onVisiblePassesChanged: (Set<LocalizedPassWithTags>) -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val emptyState = rememberLazyListState()
    val passFlow = walletViewModel.filteredPasses
    val passes: List<LocalizedPassWithTags> by remember(passFlow) {
        passFlow.map { passes ->
            passes.filter { archive == it.metadata.archived }
        }
    }.collectAsState(listOf())

    val tagFlow = walletViewModel.allTags
    val tags by tagFlow.collectAsState(setOf())

    val passTypesToShow = remember { PassType.all().toMutableStateList() }

    val sortOption = walletViewModel.sortOptionState.collectAsState().value
    val nearbyPassIds by walletViewModel.nearbyPassIds.collectAsState()
    val locationEnabled = walletViewModel.settingsStore.isLocationEnabled()

    val tagToFilterFor = remember { mutableStateOf<Tag?>(null) }
    val passToDelete = remember { mutableStateOf<LocalizedPassWithTags?>(null) }

    val sortedPasses =
        passes
            .filter { localizedPass -> passTypesToShow.any { localizedPass.pass.type.isSameType(it) } }
            .filter { localizedPass -> tagToFilterFor.value == null || localizedPass.tags.contains(tagToFilterFor.value) }
            .sortedWith(sortOption.comparator)
            .groupBy { it.metadata.groupId }
            .toList()
    val visiblePasses = sortedPasses.flatMap { it.second }.toSet()

    LaunchedEffect(visiblePasses) {
        selectedPasses.removeAll { it !in visiblePasses }
        onVisiblePassesChanged(visiblePasses)
    }

    passToDelete.value?.let { pendingDelete ->
        DeleteConfirmationDialog(
            settingsStore = walletViewModel.settingsStore,
            onConfirm = {
                walletViewModel.delete(pendingDelete.pass)
                Toast.makeText(context, resources.getString(R.string.pass_deleted), Toast.LENGTH_SHORT).show()
                passToDelete.value = null
            },
            onDismiss = {
                passToDelete.value = null
            },
        )
    }

    if (sortedPasses.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = emptyIcon,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
                contentDescription = stringResource(R.string.wallet),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(0.5f),
                alpha = 0.25f,
            )
        }
    }

    LazyColumn(
        state = if (passes.isEmpty()) emptyState else listState,
        verticalArrangement =
            Arrangement
                .spacedBy(8.dp),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        val walletItems: List<WalletItem> =
            sortedPasses.flatMap { (groupId, passes) ->
                if (groupId != null) {
                    listOf(WalletItem.Group(groupId, passes))
                } else {
                    passes.map { WalletItem.Single(it) }
                }
            }
        val nearbyItems =
            if (locationEnabled) {
                walletItems.filter { item ->
                    when (item) {
                        is WalletItem.Group -> item.passes.any { it.pass.id in nearbyPassIds }
                        is WalletItem.Single -> item.pass.pass.id in nearbyPassIds
                    }
                }
            } else {
                emptyList()
            }
        val regularItems = walletItems.filter { it !in nearbyItems }

        item {
            FilterBlock(
                walletViewModel = walletViewModel,
                sortOption = sortOption,
                onSortChange = { walletViewModel.setSortOption(it) },
                passTypesToShow = passTypesToShow,
                tags = tags,
                tagToFilterFor = tagToFilterFor,
            )
        }

        if (nearbyItems.isNotEmpty()) {
            item(key = "nearby_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nearby_header),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(nearbyItems, key = {
                when (it) {
                    is WalletItem.Group -> "nearby_group_${it.groupId}"
                    is WalletItem.Single -> "nearby_${it.pass.pass.id}"
                }
            }) { item ->
                when (item) {
                    is WalletItem.Group ->
                        GroupCard(
                            groupId = item.groupId,
                            passes = item.passes,
                            allTags = tags,
                            onClick = { navController.navigate("pass/${it.id}") },
                            walletViewModel = walletViewModel,
                            selectedPasses = selectedPasses,
                            nearbyPassIds = nearbyPassIds,
                        )
                    is WalletItem.Single -> {
                        val pass = item.pass
                        val isSelectionMode = selectedPasses.isNotEmpty()
                        SwipeToDismiss(
                            leftSwipeBackground = {
                                Icon(
                                    imageVector = if (archive) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null,
                                )
                            },
                            rightSwipeBackground = {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            allowLeftSwipe = !isSelectionMode,
                            allowRightSwipe = !isSelectionMode,
                            onLeftSwipe = { if (archive) walletViewModel.unarchive(pass.pass) else walletViewModel.archive(pass.pass) },
                            onRightSwipe = { passToDelete.value = pass },
                            modifier = Modifier.padding(2.dp),
                        ) {
                            ShortPassCard(
                                pass = pass,
                                allTags = tags,
                                isNearby = true,
                                onClick = {
                                    if (selectedPasses.isNotEmpty()) {
                                        if (selectedPasses.contains(pass)) selectedPasses.remove(pass) else selectedPasses.add(pass)
                                    } else {
                                        navController.navigate("pass/${pass.pass.id}")
                                    }
                                },
                                onLongClick = {
                                    if (selectedPasses.contains(pass)) selectedPasses.remove(pass) else selectedPasses.add(pass)
                                },
                                selected = selectedPasses.contains(pass),
                            )
                        }
                    }
                }
            }
        }

        if (nearbyItems.isNotEmpty()) {
            item(key = "nearby_divider") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        items(regularItems, key = {
            when (it) {
                is WalletItem.Group -> "group_${it.groupId}"
                is WalletItem.Single -> it.pass.pass.id
            }
        }) { item ->
            when (item) {
                is WalletItem.Group ->
                    GroupCard(
                        groupId = item.groupId,
                        passes = item.passes,
                        allTags = tags,
                        onClick = { navController.navigate("pass/${it.id}") },
                        walletViewModel = walletViewModel,
                        selectedPasses = selectedPasses,
                    )
                is WalletItem.Single -> {
                    val pass = item.pass
                    val isSelectionMode = selectedPasses.isNotEmpty()
                    SwipeToDismiss(
                        leftSwipeBackground = {
                            Icon(imageVector = if (archive) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null)
                        },
                        rightSwipeBackground = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        allowLeftSwipe = !isSelectionMode,
                        allowRightSwipe = !isSelectionMode,
                        onLeftSwipe = { if (archive) walletViewModel.unarchive(pass.pass) else walletViewModel.archive(pass.pass) },
                        onRightSwipe = { passToDelete.value = pass },
                        modifier = Modifier.padding(2.dp),
                    ) {
                        ShortPassCard(
                            pass = pass,
                            allTags = tags,
                            onClick = {
                                if (selectedPasses.isNotEmpty()) {
                                    if (selectedPasses.contains(pass)) selectedPasses.remove(pass) else selectedPasses.add(pass)
                                } else {
                                    navController.navigate("pass/${pass.pass.id}")
                                }
                            },
                            onLongClick = {
                                if (selectedPasses.contains(pass)) selectedPasses.remove(pass) else selectedPasses.add(pass)
                            },
                            selected = selectedPasses.contains(pass),
                        )
                    }
                }
            }
        }
        if (!archive) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.Archive.route)
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.show_archived_passes),
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.padding(4.dp))
        }
    }
}

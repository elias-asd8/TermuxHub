package com.maazm7d.termuxhub.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullRefreshIndicator
import androidx.compose.material3.pulltorefresh.pullRefresh
import androidx.compose.material3.pulltorefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maazm7d.termuxhub.domain.model.getPublishedDate
import com.maazm7d.termuxhub.ui.components.CategoryChips
import com.maazm7d.termuxhub.ui.components.SearchBar
import com.maazm7d.termuxhub.ui.components.ToolCard

enum class SortType(val label: String) {
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first"),
    MOST_STARRED("Most starred"),
    LEAST_STARRED("Least starred")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val starsMap by viewModel.starsMap.collectAsState()

    val searchQuery = rememberSaveable { mutableStateOf("") }
    var selectedCategoryIndex by rememberSaveable { mutableStateOf(0) }
    var currentSort by rememberSaveable { mutableStateOf(SortType.NEWEST_FIRST) }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val refreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    val categoryCounts = remember(uiState.tools) { uiState.tools.groupingBy { it.category }.eachCount() }
    val categories = remember(uiState.tools, categoryCounts) {
        listOf("All" to uiState.tools.size) + categoryCounts.keys.sorted().map { it to (categoryCounts[it] ?: 0) }
    }

    val filteredTools = remember(uiState.tools, searchQuery.value, selectedCategoryIndex, currentSort, starsMap) {
        uiState.tools
            .filter { tool ->
                val matchesQuery = searchQuery.value.isBlank() ||
                        tool.name.contains(searchQuery.value, true) ||
                        tool.description.contains(searchQuery.value, true)
                val matchesCategory = selectedCategoryIndex == 0 ||
                        tool.category.equals(categories[selectedCategoryIndex].first, true)
                matchesQuery && matchesCategory
            }
            .let { list ->
                when (currentSort) {
                    SortType.NEWEST_FIRST -> list.sortedByDescending { it.getPublishedDate() }
                    SortType.OLDEST_FIRST -> list.sortedBy { it.getPublishedDate() }
                    SortType.MOST_STARRED -> list.sortedByDescending { starsMap[it.id] ?: 0 }
                    SortType.LEAST_STARRED -> list.sortedBy { starsMap[it.id] ?: 0 }
                }
            }
    }

    Box(modifier = Modifier.pullRefresh(refreshState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
        ) {
            // Search + sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(queryState = searchQuery, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortType.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label) },
                                leadingIcon = {
                                    if (currentSort == sort) Icon(Icons.Default.Check, null)
                                },
                                onClick = {
                                    currentSort = sort
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Category row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { categoryMenuExpanded = true }) {
                        Icon(Icons.Default.GridView, contentDescription = "Categories")
                    }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        categories.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text("${item.first} (${item.second})") },
                                leadingIcon = {
                                    if (selectedCategoryIndex == index) Icon(Icons.Default.Check, null)
                                },
                                onClick = {
                                    selectedCategoryIndex = index
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                CategoryChips(
                    chips = categories,
                    selectedIndex = selectedCategoryIndex,
                    onChipSelected = { selectedCategoryIndex = it }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!uiState.error.isNullOrBlank()) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (uiState.isLoading && filteredTools.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(filteredTools, key = { it.id }) { tool ->
                        ToolCard(
                            tool = tool,
                            stars = starsMap[tool.id],
                            onOpenDetails = onOpenDetails,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSave = { viewModel.toggleFavorite(it) }
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

package com.maazm7d.termuxhub.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maazm7d.termuxhub.domain.model.getPublishedDate
import com.maazm7d.termuxhub.ui.components.SearchBar
import com.maazm7d.termuxhub.ui.components.ToolCard

enum class SortType(val label: String) {
    NEWEST_FIRST("Newest"),
    OLDEST_FIRST("Oldest"),
    MOST_STARRED("Most ⭐"),
    LEAST_STARRED("Least ⭐")
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

    val listState = rememberLazyListState()

    val categoryCounts = remember(uiState.tools) {
        uiState.tools.groupingBy { it.category }.eachCount()
    }

    val categories = remember(uiState.tools, categoryCounts) {
        listOf("All" to uiState.tools.size) +
                categoryCounts.keys.sorted().map { it to (categoryCounts[it] ?: 0) }
    }

    val filteredTools = remember(
        uiState.tools,
        searchQuery.value,
        selectedCategoryIndex,
        currentSort,
        starsMap
    ) {

        uiState.tools
            .filter { tool ->

                val matchesQuery =
                    searchQuery.value.isBlank() ||
                            tool.name.contains(searchQuery.value, true) ||
                            tool.description.contains(searchQuery.value, true)

                val matchesCategory =
                    selectedCategoryIndex == 0 ||
                            tool.category.equals(
                                categories[selectedCategoryIndex].first,
                                true
                            )

                matchesQuery && matchesCategory
            }
            .let { list ->

                when (currentSort) {

                    SortType.NEWEST_FIRST ->
                        list.sortedByDescending { it.getPublishedDate() }

                    SortType.OLDEST_FIRST ->
                        list.sortedBy { it.getPublishedDate() }

                    SortType.MOST_STARRED ->
                        list.sortedByDescending { starsMap[it.id] ?: 0 }

                    SortType.LEAST_STARRED ->
                        list.sortedBy { starsMap[it.id] ?: 0 }
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            SearchBar(
                queryState = searchQuery,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {

                Column(
                    modifier = Modifier.padding(10.dp)
                ) {

                    Text(
                        text = "Sort",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        SortType.values().forEachIndexed { index, sort ->

                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = SortType.values().size
                                ),
                                selected = currentSort == sort,
                                onClick = { currentSort = sort },
                                label = { Text(sort.label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {

                        categories.forEachIndexed { index, item ->

                            FilterChip(
                                selected = selectedCategoryIndex == index,
                                onClick = { selectedCategoryIndex = index },

                                label = {
                                    Text("${item.first} (${item.second})")
                                },

                                leadingIcon =
                                if (selectedCategoryIndex == index) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,

                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (!uiState.error.isNullOrBlank()) {

                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (uiState.isLoading && filteredTools.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
    }
}

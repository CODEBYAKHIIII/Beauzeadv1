package com.example.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.BannerAdPlaceholder
import com.example.components.EmptyStateView
import com.example.components.WavesCard
import com.example.components.WavesChip
import com.example.components.WavesFAB
import com.example.components.WavesHeader
import com.example.components.WavesTextField
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.SampleData
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProductListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    val productState by remember { FirestoreDataRepository.observeProducts() }
        .collectAsState(initial = FirestoreState.Loading)
    val allProducts = when (val state = productState) {
        FirestoreState.Loading -> {
            StateScreen(type = StateType.LOADING, message = "Loading products...")
            return
        }
        is FirestoreState.Failure -> {
            StateScreen(type = StateType.ERROR, message = state.message)
            return
        }
        is FirestoreState.Data -> state.value
    }
    val filteredProducts = allProducts.filter { product ->
        val matchesFilter = when (selectedFilter) {
            "Active" -> !product.isArchived
            "Archived" -> product.isArchived
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                (product.sku?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (product.description?.contains(searchQuery, ignoreCase = true) ?: false)
        matchesFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Products & Services",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier.testTag("product_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = OnPrimary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToAddProduct,
                        modifier = Modifier.testTag("product_add_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Product",
                            tint = OnPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            WavesFAB(
                onClick = onNavigateToAddProduct,
                contentDescription = "Add Product"
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("product_list_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearchVisible) {
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    WavesTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Search Products",
                        placeholder = "Product name or SKU...",
                        leadingIcon = Icons.Filled.Search
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // Filter chips: [All] [Active] [Archived]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Active", "Archived").forEach { filter ->
                    WavesChip(
                        text = filter,
                        isSelected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                val emptyMessage = when {
                    searchQuery.isNotBlank() ->
                        "No products match '$searchQuery'."
                    selectedFilter != "All" ->
                        "No products with status '$selectedFilter'."
                    else ->
                        "Add recurring services or items to insert into invoices quickly."
                }
                EmptyStateView(
                    icon = Icons.Filled.Inventory2,
                    title = "No products found",
                    message = emptyMessage,
                    buttonText = "+ Add Product",
                    onButtonClick = onNavigateToAddProduct
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        WavesCard(
                            onClick = { onNavigateToEditProduct(product.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldInk.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint = EmeraldInk,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (!product.description.isNullOrBlank()) {
                                        Text(
                                            text = product.description,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildString {
                                            append("SKU: ")
                                            append(product.sku ?: "—")
                                            append(" · Tax: ")
                                            append("${product.taxRate.toInt()}%")
                                        },
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = SampleData.formatCurrency(product.unitPrice),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "/ ${product.unit}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        BannerAdPlaceholder(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }
}
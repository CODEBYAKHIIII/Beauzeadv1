package com.example.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
import com.example.components.WavesBottomNav
import com.example.components.WavesCard
import com.example.components.WavesChip
import com.example.components.WavesFAB
import com.example.components.WavesHeader
import com.example.components.WavesNavTab
import com.example.components.WavesTextField
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.showDemoToast
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.InvoiceDisplayFormat
import com.example.data.BusinessProfile
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClientListScreen(
    onNavigateToClientDetail: (String) -> Unit,
    onNavigateToAddClient: () -> Unit,
    onNavigateToTab: (WavesNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    val clientState by remember { FirestoreDataRepository.observeClients() }
        .collectAsState(initial = FirestoreState.Loading)
    // Currency display follows the business country, matching the PDF exports.
    val businessState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val businessCountry = ((businessState as? FirestoreState.Data<*>)?.value as? BusinessProfile)
        ?.country.orEmpty().ifBlank { "India" }
    fun formatCurrency(amount: Double): String =
        InvoiceDisplayFormat.formatCurrency(amount, businessCountry)
    val allClients = when (val state = clientState) {
        FirestoreState.Loading -> {
            StateScreen(type = StateType.LOADING, message = "Loading clients...")
            return
        }
        is FirestoreState.Failure -> {
            StateScreen(type = StateType.ERROR, message = state.message)
            return
        }
        is FirestoreState.Data -> state.value
    }
    val filteredClients = allClients.filter { client ->
        val matchesFilter = when (selectedFilter) {
            "Active" -> !client.isArchived
            "Archived" -> client.isArchived
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                client.name.contains(searchQuery, ignoreCase = true) ||
                (client.email?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (client.phone?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (client.city?.contains(searchQuery, ignoreCase = true) ?: false)
        matchesFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Clients",
                actions = {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier.testTag("client_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = OnPrimary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToAddClient,
                        modifier = Modifier.testTag("client_add_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Client",
                            tint = OnPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            WavesBottomNav(
                currentTab = WavesNavTab.CLIENTS,
                onTabSelected = onNavigateToTab
            )
        },
        floatingActionButton = {
            WavesFAB(
                onClick = onNavigateToAddClient,
                contentDescription = "Add Client"
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("client_list_screen")
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
                        label = "Search Clients",
                        placeholder = "Type name, email, phone or city...",
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

            // Filter chips row: [All] [Active] [Archived]
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

            if (filteredClients.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Person,
                    title = "No clients found",
                    message = "Add your first client to start creating professional invoices.",
                    buttonText = "+ Add Client",
                    onButtonClick = onNavigateToAddClient
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients) { client ->
                        WavesCard(
                            onClick = { onNavigateToClientDetail(client.id) }
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
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = EmeraldInk,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = client.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${client.email ?: "No email"} · ${client.invoiceCount} invoices",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Billed: ${formatCurrency(client.totalBilled)}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    if (client.totalDue > 0) {
                                        Text(
                                            text = "${formatCurrency(client.totalDue)} due",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed
                                        )
                                    } else {
                                        Text(
                                            text = "All paid",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    }
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
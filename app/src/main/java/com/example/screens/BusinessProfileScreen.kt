package com.example.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import coil.compose.AsyncImage
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BusinessProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val initial = when (val state = profileState) {
        FirestoreState.Loading -> {
            StateScreen(type = StateType.LOADING, message = "Loading business profile...")
            return
        }
        is FirestoreState.Failure -> {
            StateScreen(type = StateType.ERROR, title = "Profile Error", message = state.message)
            return
        }
        is FirestoreState.Data -> state.value
    }

    var isSaving by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Business info
    var businessName by remember { mutableStateOf(initial.name) }
    var tagline by remember { mutableStateOf(initial.tagline) }
    var logoUrl by remember { mutableStateOf(initial.logoUrl) }
    var isUploadingLogo by remember { mutableStateOf(false) }
    var logoToCrop by remember { mutableStateOf<Uri?>(null) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            logoToCrop = uri
        }
    }

    // Contact
    var email by remember { mutableStateOf(initial.email) }
    var phone by remember { mutableStateOf(initial.phone) }
    var website by remember { mutableStateOf(initial.website) }

    // Address
    var addressLine1 by remember { mutableStateOf(initial.addressLine1) }
    var addressLine2 by remember { mutableStateOf(initial.addressLine2) }
    var city by remember { mutableStateOf(initial.city) }
    var state by remember { mutableStateOf(initial.state) }
    var postalCode by remember { mutableStateOf(initial.postalCode) }

    var country by remember { mutableStateOf(initial.country) }
    var countryDropdownOpen by remember { mutableStateOf(false) }
    val countries = listOf(
        "India", "USA", "UK", "UAE", "Australia", "Canada",
        "Germany", "Singapore", "Nigeria", "Kenya",
        "South Africa", "Brazil"
    )

    fun saveProfile() {
        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                FirestoreDataRepository.saveBusinessProfile(
                    initial.copy(
                        name = businessName,
                        tagline = tagline,
                        email = email,
                        phone = phone,
                        website = website,
                        logoUrl = logoUrl,
                        addressLine1 = addressLine1,
                        addressLine2 = addressLine2,
                        city = city,
                        state = state,
                        postalCode = postalCode,
                        country = country
                    )
                )
                showSuccess = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to save business profile."
            } finally {
                isSaving = false
            }
        }
    }

    val cropSourceUri = logoToCrop
    if (cropSourceUri != null) {
        LogoCropScreen(
            sourceUri = cropSourceUri,
            onNavigateBack = { logoToCrop = null },
            onChooseAnotherImage = { logoPicker.launch("image/*") },
            onSaveCroppedImage = { croppedUri ->
                isUploadingLogo = true
                try {
                    logoUrl = FirestoreDataRepository.uploadBusinessLogo(croppedUri)
                    logoToCrop = null
                } finally {
                    croppedUri.path?.let { java.io.File(it).delete() }
                    isUploadingLogo = false
                }
            }
        )
        return
    }

    if (isSaving) {
        StateScreen(type = StateType.LOADING, message = "Saving business profile...")
        return
    }
    if (errorMessage.isNotBlank()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Profile Error",
            message = errorMessage,
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }
    if (showSuccess) {
        StateScreen(
            type = StateType.SUCCESS,
            title = "Profile Saved!",
            message = "Your business profile has been updated.",
            primaryButtonText = "DONE",
            onPrimaryClick = onNavigateBack
        )
        return
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Business Profile",
                onBackClick = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = ::saveProfile,
                        modifier = Modifier.testTag("business_save_header_button")
                    ) {
                        Text(
                            "Save",
                            color = OnPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("business_profile_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ===== LOGO =====
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6F4EA))
                        .clickable {
                            logoPicker.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = "Business Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Business Logo",
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    if (isUploadingLogo) {
                        CircularProgressIndicator(color = EmeraldInk, modifier = Modifier.size(28.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EmeraldInk.copy(alpha = 0.85f))
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Upload",
                            tint = OnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to upload business logo",
                    fontSize = 13.sp,
                    color = EmeraldInk,
                    fontWeight = FontWeight.Medium
                )
            }

            // ===== BUSINESS INFO =====
            SectionHeader(title = "BUSINESS INFO")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = "Business Name *"
                    )
                    WavesTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = "Tagline"
                    )
                }
            }

            // ===== CONTACT =====
            SectionHeader(title = "CONTACT")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email *",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    WavesTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Mobile Number *",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    WavesTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = "Website"
                    )
                }
            }

            // ===== ADDRESS =====
            SectionHeader(title = "ADDRESS")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = addressLine1,
                        onValueChange = { addressLine1 = it },
                        label = "Address Line 1 *"
                    )
                    WavesTextField(
                        value = addressLine2,
                        onValueChange = { addressLine2 = it },
                        label = "Address Line 2"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WavesTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = "City *",
                            modifier = Modifier.weight(1f)
                        )
                        WavesTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = "State",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WavesTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = "Postal Code *",
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = "Country",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = country,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clickable { countryDropdownOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { countryDropdownOpen = true }) {
                                            Icon(
                                                Icons.Filled.ArrowDropDown,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceColor,
                                        unfocusedContainerColor = SurfaceColor,
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = InputBorderGray
                                    )
                                )
                                DropdownMenu(
                                    expanded = countryDropdownOpen,
                                    onDismissRequest = { countryDropdownOpen = false }
                                ) {
                                    countries.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                country = item
                                                countryDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== SAVE BUTTON =====
            WavesPrimaryButton(
                text = "SAVE CHANGES",
                onClick = ::saveProfile
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}
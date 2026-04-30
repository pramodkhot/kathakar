package com.kathakar.app.ui.screens

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.kathakar.app.R

// Language data — code + native name + English name
data class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String
)

val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("en", "English",    "English"),
    AppLanguage("hi", "हिन्दी",     "Hindi"),
    AppLanguage("mr", "मराठी",      "Marathi"),
    AppLanguage("ta", "தமிழ்",      "Tamil"),
    AppLanguage("te", "తెలుగు",     "Telugu"),
    AppLanguage("bn", "বাংলা",      "Bengali"),
    AppLanguage("gu", "ગુજરાતી",    "Gujarati"),
    AppLanguage("kn", "ಕನ್ನಡ",      "Kannada"),
    AppLanguage("pa", "ਪੰਜਾਬੀ",     "Punjabi")
)

fun getCurrentLanguageCode(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (locales.isEmpty) "en"
    else locales[0]?.language ?: "en"
}

fun applyLanguage(code: String) {
    val localeList = if (code == "en") LocaleListCompat.getEmptyLocaleList()
                     else LocaleListCompat.forLanguageTags(code)
    AppCompatDelegate.setApplicationLocales(localeList)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: com.kathakar.app.domain.model.User? = null,
    onBack: () -> Unit,
    onSaveContentLanguages: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    var currentLangCode  by remember { mutableStateOf(getCurrentLanguageCode()) }
    var showLangDialog   by remember { mutableStateOf(false) }
    var pendingLangCode  by remember { mutableStateOf(currentLangCode) }
    var showContentLangDialog by remember { mutableStateOf(false) }

    // Content language preference — initialise from user profile
    val contentLangs = remember(user?.preferredLanguages) {
        mutableStateListOf<String>().also {
            it.addAll(user?.preferredLanguages ?: listOf("en"))
        }
    }

    val currentLang = SUPPORTED_LANGUAGES.find { it.code == currentLangCode }
        ?: SUPPORTED_LANGUAGES[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title),
                    fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── UI Language Section ──────────────────────────────────────────
            SettingsSectionHeader(text = stringResource(R.string.language_settings))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showLangDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.language_settings),
                            fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(text = stringResource(R.string.language_settings_sub),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)) {
                        Text(text = currentLang.nativeName, fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }

            Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.language_change_note),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 18.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Content Language Section (Approach C) ────────────────────────
            SettingsSectionHeader(text = stringResource(R.string.content_language_title))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { showContentLangDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.content_language_title),
                            fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(text = stringResource(R.string.content_language_sub),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    // Show badge count
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)) {
                        Text(text = "${contentLangs.size} selected",
                            fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }

            // Show selected content languages as chips
            if (contentLangs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(
                        androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    contentLangs.forEach { code ->
                        val lang = SUPPORTED_LANGUAGES.find { it.code == code }
                        if (lang != null) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)) {
                                Text(text = lang.nativeName, fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── About Section ─────────────────────────────────────────────────
            SettingsSectionHeader(text = stringResource(R.string.about_title))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsInfoRow(icon = Icons.Default.Info,
                        label = stringResource(R.string.app_name), value = "KathaKar")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsInfoRow(icon = Icons.Default.Star, label = "Version", value = "1.0.0")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsInfoRow(icon = Icons.Default.Place, label = "Languages",
                        value = "9 Indian languages")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── UI Language Dialog ────────────────────────────────────────────────────
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text(text = stringResource(R.string.language_settings),
                fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        val isSelected = lang.code == pendingLangCode
                        Surface(modifier = Modifier.fillMaxWidth().clickable { pendingLangCode = lang.code },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)) {
                            Row(modifier = Modifier.padding(12.dp, 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected,
                                    onClick = { pendingLangCode = lang.code },
                                    modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = lang.nativeName,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 15.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface)
                                    if (lang.code != "en") {
                                        Text(text = lang.englishName, fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showLangDialog = false; currentLangCode = pendingLangCode
                    applyLanguage(pendingLangCode)
                    (context as? android.app.Activity)?.let { a -> a.window.decorView.post { a.recreate() } }
                }) { Text(text = stringResource(R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingLangCode = currentLangCode; showLangDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Content Language Multi-Select Dialog (Approach C) ────────────────────
    if (showContentLangDialog) {
        val tempSelected = remember { mutableStateListOf<String>().also { it.addAll(contentLangs) } }
        AlertDialog(
            onDismissRequest = { showContentLangDialog = false },
            title = { Text(text = stringResource(R.string.content_language_title),
                fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.content_language_sub),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        val isChecked = lang.code in tempSelected
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (isChecked) {
                                    if (tempSelected.size > 1) tempSelected.remove(lang.code)
                                } else { tempSelected.add(lang.code) }
                            },
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp, 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) tempSelected.add(lang.code)
                                        else if (tempSelected.size > 1) tempSelected.remove(lang.code)
                                    })
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(text = lang.nativeName, fontSize = 15.sp,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal)
                                    if (lang.code != "en") {
                                        Text(text = lang.englishName, fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    contentLangs.clear()
                    contentLangs.addAll(tempSelected)
                    showContentLangDialog = false
                    onSaveContentLanguages(tempSelected.toList())
                }) { Text(text = stringResource(R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showContentLangDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector,
                             label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(text = label, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

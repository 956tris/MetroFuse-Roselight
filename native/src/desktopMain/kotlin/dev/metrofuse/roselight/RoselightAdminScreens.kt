package dev.metrofuse.roselight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.metrofuse.api.PluginContext
import dev.metrofuse.api.UiContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private data class RoselightSettingItem(
    val key: String,
    val title: String,
    val category: String,
    val type: String,
    val value: String,
    val boolValue: Boolean,
    val options: List<String>,
)

private fun parseRoselightSettings(snapshot: JsonObject): List<RoselightSettingItem> =
    (snapshot["settings"] as? JsonArray).orEmpty().mapNotNull { el ->
        val obj = el as? JsonObject ?: return@mapNotNull null
        val key = obj["key"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: key
        val category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "General"
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "string"
        val prim = obj["value"] as? JsonPrimitive
        val options = (obj["options"] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
        RoselightSettingItem(
            key = key,
            title = title,
            category = category,
            type = type,
            value = prim?.contentOrNull.orEmpty(),
            boolValue = prim?.booleanOrNull == true,
            options = options,
        )
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RoselightSettingsScreen(
    ui: UiContext,
    context: PluginContext,
) {
    val dark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<JsonObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var draftStrings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    suspend fun callSettings(payload: JsonObject) {
        error = null
        try {
            val res = context.callService("ui.settings/v1", payload)
            snapshot = res
            draftStrings = parseRoselightSettings(res).filter { it.type == "string" }.associate { it.key to it.value }
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            error = e.message ?: "Unable to reach host settings."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        callSettings(buildJsonObject { put("action", "snapshot") })
    }

    MusicTheme(dark) {
        val colors = MaterialTheme.colorScheme
        Surface(Modifier.fillMaxSize(), color = colors.background, contentColor = colors.onSurface) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val items = remember(snapshot) { snapshot?.let(::parseRoselightSettings).orEmpty() }
                val grouped = remember(items) { items.groupBy { it.category } }

                LazyColumn(
                    Modifier.widthIn(max = 820.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(end = 44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconButton(
                                onClick = { ui.navigate("home") },
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(colors.surfaceVariant),
                            ) {
                                Glyph("back", "Back", Modifier.size(19.dp), colors.primary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                                Text("Roselight Apple Music Experience", color = colors.onSurfaceVariant, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { ui.navigate("plugins") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant, contentColor = colors.primary),
                                shape = CircleShape,
                            ) {
                                Text("Plugins", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (loading) {
                        item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary) }
                    }

                    error?.let { msg ->
                        item {
                            Surface(color = colors.primary.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                                Text(msg, Modifier.padding(14.dp), color = colors.primary, fontSize = 13.sp)
                            }
                        }
                    }

                    grouped.forEach { (category, catItems) ->
                        item(key = "apple-cat:$category") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    category.uppercase(),
                                    Modifier.padding(start = 14.dp),
                                    color = colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.6.sp,
                                )
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    catItems.forEachIndexed { idx, setting ->
                                        if (idx > 0) HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f))
                                        when (setting.type) {
                                            "boolean" -> {
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text(setting.title, Modifier.weight(1f), fontSize = 16.sp)
                                                    Switch(
                                                        checked = setting.boolValue,
                                                        onCheckedChange = { checked ->
                                                            scope.launch {
                                                                callSettings(
                                                                    buildJsonObject {
                                                                        put("action", "set")
                                                                        put("key", setting.key)
                                                                        put("value", checked)
                                                                    },
                                                                )
                                                            }
                                                        },
                                                        colors =
                                                            SwitchDefaults.colors(
                                                                checkedThumbColor = Color.White,
                                                                checkedTrackColor = colors.primary,
                                                                uncheckedThumbColor = Color.White,
                                                                uncheckedTrackColor = colors.surfaceVariant,
                                                            ),
                                                    )
                                                }
                                            }

                                            "select" -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(setting.title, fontSize = 16.sp)
                                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        setting.options.forEach { option ->
                                                            val selected = setting.value == option
                                                            FilterChip(
                                                                selected = selected,
                                                                onClick = {
                                                                    scope.launch {
                                                                        callSettings(
                                                                            buildJsonObject {
                                                                                put("action", "set")
                                                                                put("key", setting.key)
                                                                                put("value", option)
                                                                            },
                                                                        )
                                                                    }
                                                                },
                                                                shape = CircleShape,
                                                                border = null,
                                                                colors =
                                                                    FilterChipDefaults.filterChipColors(
                                                                        containerColor = colors.surfaceVariant,
                                                                        labelColor = colors.onSurface,
                                                                        selectedContainerColor = colors.primary,
                                                                        selectedLabelColor = Color.White,
                                                                    ),
                                                                label = { Text(option, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            else -> {
                                                val currentDraft = draftStrings[setting.key] ?: setting.value
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(setting.title, fontSize = 16.sp)
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        OutlinedTextField(
                                                            value = currentDraft,
                                                            onValueChange = { draftStrings = draftStrings + (setting.key to it) },
                                                            modifier = Modifier.weight(1f),
                                                            singleLine = true,
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors =
                                                                OutlinedTextFieldDefaults.colors(
                                                                    focusedBorderColor = colors.primary,
                                                                    unfocusedBorderColor = colors.outlineVariant,
                                                                ),
                                                        )
                                                        Button(
                                                            onClick = {
                                                                scope.launch {
                                                                    callSettings(
                                                                        buildJsonObject {
                                                                            put("action", "set")
                                                                            put("key", setting.key)
                                                                            put("value", currentDraft)
                                                                        },
                                                                    )
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                                                            shape = RoundedCornerShape(12.dp),
                                                        ) {
                                                            Text("Save", fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .border(0.5.dp, colors.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Built-in MetroFuse Interface", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Restore the original MetroFuse screen anytime for Storage folders, Backup & Restore, or local file import.",
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        callSettings(buildJsonObject { put("action", "openOriginal") })
                                    }
                                },
                                shape = CircleShape,
                            ) {
                                Text("Use original MetroFuse UI", color = colors.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RoselightPluginsScreen(
    ui: UiContext,
    context: PluginContext,
) {
    val dark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<JsonObject?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var marketplaceUrl by remember { mutableStateOf("") }
    var expandedPluginId by remember { mutableStateOf<String?>(null) }

    suspend fun callPlugins(payload: JsonObject) {
        busy = true
        error = null
        try {
            val res = context.callService("ui.plugins/v1", payload)
            snapshot = res
            if (marketplaceUrl.isBlank()) {
                marketplaceUrl = res["marketplaceUrl"]?.jsonPrimitive?.contentOrNull.orEmpty()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            error = e.message ?: "Unable to manage plugins."
        } finally {
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        callPlugins(buildJsonObject { put("action", "snapshot") })
    }

    val plugins = remember(snapshot) { (snapshot?.get("plugins") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject } }
    val uiApps = remember(snapshot) { (snapshot?.get("uiApps") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject } }
    val marketplace = remember(snapshot) { (snapshot?.get("marketplace") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject } }

    MusicTheme(dark) {
        val colors = MaterialTheme.colorScheme
        Surface(Modifier.fillMaxSize(), color = colors.background, contentColor = colors.onSurface) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = 880.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(end = 44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconButton(
                                onClick = { ui.navigate("home") },
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(colors.surfaceVariant),
                            ) {
                                Glyph("back", "Back", Modifier.size(19.dp), colors.primary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Plugins", fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                                Text("${plugins.size} installed · Providers, UI Shells & Marketplace", color = colors.onSurfaceVariant, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { ui.navigate("settings") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant, contentColor = colors.primary),
                                shape = CircleShape,
                            ) {
                                Text("Settings", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceVariant)
                                .padding(3.dp),
                        ) {
                            listOf("Installed", "Marketplace", "Interface").forEachIndexed { index, label ->
                                val selected = tab == index
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) colors.surface else Color.Transparent)
                                        .clickable { tab = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) colors.primary else colors.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    if (busy) {
                        item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary) }
                    }

                    error?.let { msg ->
                        item {
                            Surface(color = colors.primary.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                                Text(msg, Modifier.padding(14.dp), color = colors.primary, fontSize = 13.sp)
                            }
                        }
                    }

                    when (tab) {
                        0 -> {
                            items(plugins, key = { it["id"]?.jsonPrimitive?.contentOrNull ?: it.toString() }) { plugin ->
                                val id = plugin["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val name = plugin["name"]?.jsonPrimitive?.contentOrNull ?: id
                                val version = plugin["version"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val author = plugin["author"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val desc = plugin["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val enabled = plugin["enabled"]?.jsonPrimitive?.booleanOrNull == true
                                val state = plugin["state"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val pluginError = plugin["error"]?.jsonPrimitive?.contentOrNull
                                val settings = (plugin["settings"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                                val expanded = expandedPluginId == id

                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("$name · v$version", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                            Text("$author · $state", color = colors.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                        Switch(
                                            checked = enabled,
                                            enabled = !busy,
                                            onCheckedChange = { target ->
                                                scope.launch {
                                                    callPlugins(
                                                        buildJsonObject {
                                                            put("action", if (target) "enable" else "disable")
                                                            put("id", id)
                                                        },
                                                    )
                                                }
                                            },
                                            colors =
                                                SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = colors.primary,
                                                ),
                                        )
                                    }

                                    if (desc.isNotBlank()) {
                                        Text(desc, color = colors.onSurfaceVariant, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (!pluginError.isNullOrBlank()) {
                                        Text(pluginError, color = colors.primary, fontSize = 12.sp)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (settings.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = { expandedPluginId = if (expanded) null else id },
                                                shape = CircleShape,
                                            ) {
                                                Text(if (expanded) "Hide Options" else "Options (${settings.size})", color = colors.primary, fontSize = 12.sp)
                                            }
                                        }
                                        TextButton(
                                            enabled = !busy,
                                            onClick = {
                                                scope.launch {
                                                    callPlugins(buildJsonObject { put("action", "reload"); put("id", id) })
                                                }
                                            },
                                        ) { Text("Reload", color = colors.onSurfaceVariant) }
                                        TextButton(
                                            enabled = !busy,
                                            onClick = {
                                                scope.launch {
                                                    callPlugins(buildJsonObject { put("action", "uninstall"); put("id", id) })
                                                }
                                            },
                                        ) { Text("Remove", color = colors.primary) }
                                    }

                                    if (expanded && settings.isNotEmpty()) {
                                        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f))
                                        settings.forEach { def ->
                                            val key = def["key"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                            val title = def["title"]?.jsonPrimitive?.contentOrNull ?: key
                                            val type = def["type"]?.jsonPrimitive?.contentOrNull ?: "STRING"
                                            val rawVal: JsonElement? = def["value"]
                                            if (type == "BOOLEAN") {
                                                val checked = (rawVal as? JsonPrimitive)?.booleanOrNull == true
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text(title, Modifier.weight(1f), fontSize = 14.sp)
                                                    Switch(
                                                        checked = checked,
                                                        enabled = !busy,
                                                        onCheckedChange = { next ->
                                                            scope.launch {
                                                                callPlugins(
                                                                    buildJsonObject {
                                                                        put("action", "setPluginSetting")
                                                                        put("id", id)
                                                                        put("key", key)
                                                                        put("value", next)
                                                                    },
                                                                )
                                                            }
                                                        },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.primary),
                                                    )
                                                }
                                            } else {
                                                val currentText = (rawVal as? JsonPrimitive)?.contentOrNull.orEmpty()
                                                Text("$title: $currentText", color = colors.onSurfaceVariant, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            item {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text("Marketplace Registry", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = marketplaceUrl,
                                            onValueChange = { marketplaceUrl = it },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            placeholder = { Text("https://raw.githubusercontent.com/.../registry.json") },
                                        )
                                        Button(
                                            enabled = !busy && marketplaceUrl.isNotBlank(),
                                            onClick = {
                                                scope.launch {
                                                    callPlugins(buildJsonObject { put("action", "fetchMarketplace"); put("url", marketplaceUrl.trim()) })
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                                            shape = RoundedCornerShape(12.dp),
                                        ) { Text("Fetch") }
                                    }
                                }
                            }

                            items(marketplace, key = { it["id"]?.jsonPrimitive?.contentOrNull ?: it.toString() }) { entry ->
                                val id = entry["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val name = entry["name"]?.jsonPrimitive?.contentOrNull ?: id
                                val version = entry["version"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val installedVer = entry["installedVersion"]?.jsonPrimitive?.contentOrNull
                                val compatible = entry["compatible"]?.jsonPrimitive?.booleanOrNull == true
                                val desc = entry["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text("$name · v$version", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                            if (installedVer != null) Text("Installed v$installedVer", color = colors.primary, fontSize = 12.sp)
                                        }
                                        Button(
                                            enabled = !busy && compatible,
                                            onClick = {
                                                scope.launch {
                                                    callPlugins(
                                                        buildJsonObject {
                                                            put("action", "installMarketplace")
                                                            put("id", id)
                                                            put("url", marketplaceUrl.trim())
                                                        },
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                                            shape = CircleShape,
                                        ) {
                                            Text(if (installedVer == version) "Reinstall" else if (installedVer != null) "Update" else "Install")
                                        }
                                    }
                                    Text(desc, color = colors.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }

                        else -> {
                            item {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surface)
                                        .border(0.5.dp, colors.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text("App Interface", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    uiApps.forEach { app ->
                                        val id = app["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                        val name = app["name"]?.jsonPrimitive?.contentOrNull ?: id
                                        val active = app["active"]?.jsonPrimitive?.booleanOrNull == true
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (active) colors.surfaceVariant else Color.Transparent)
                                                .clickable(enabled = !busy) {
                                                    scope.launch {
                                                        callPlugins(buildJsonObject { put("action", "setUiApp"); put("id", id) })
                                                    }
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(name, fontWeight = FontWeight.Medium)
                                            if (active) Text("Active", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedButton(
                                        enabled = !busy,
                                        onClick = {
                                            scope.launch {
                                                callPlugins(buildJsonObject { put("action", "setSafeMode"); put("enabled", true) })
                                            }
                                        },
                                        shape = CircleShape,
                                    ) {
                                        Text("Use original MetroFuse interface (import local files)", color = colors.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

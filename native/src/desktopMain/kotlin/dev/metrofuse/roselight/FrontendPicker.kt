package dev.metrofuse.roselight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.metrofuse.api.PluginContext
import dev.metrofuse.sdk.PluginAlertDialog
import dev.metrofuse.sdk.resources.Res
import dev.metrofuse.sdk.resources.offline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.stringResource

internal data class FrontendChoice(
    val id: String,
    val title: String,
)

internal data class FrontendSelection(
    val selected: String,
    val choices: List<FrontendChoice>,
)

internal fun parseFrontendSelection(response: JsonObject): FrontendSelection {
    val choices =
        (response["sources"] as? JsonArray)
            ?.mapNotNull { value ->
                val source = value as? JsonObject ?: return@mapNotNull null
                val id = (source["id"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
                val title = (source["title"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
                if (id.isNullOrBlank() || title.isNullOrBlank()) null else FrontendChoice(id, title)
            }.orEmpty()
            .distinctBy { it.id }
    val selected = (response["selected"] as? JsonPrimitive)?.contentOrNull.orEmpty()
    return FrontendSelection(selected.takeIf { id -> choices.any { it.id == id } }.orEmpty(), choices)
}

@Composable
internal fun FrontendPicker(
    context: PluginContext,
    dismiss: () -> Unit,
    selected: (FrontendSelection) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selection by remember { mutableStateOf<FrontendSelection?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        try {
            selection = parseFrontendSelection(context.callService("ui.frontends/v1", buildJsonObject {}))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }
    PluginAlertDialog(
        onDismissRequest = { if (!loading) dismiss() },
        title = { Text("Music source") },
        text = {
            Column(Modifier.heightIn(max = 310.dp).verticalScroll(rememberScrollState())) {
                val current = selection
                if (loading) {
                    Text("Loading sources…")
                } else if (current == null) {
                    Text("Use the original interface and tap Home to change providers on this MetroFuse build.")
                } else {
                    val offline = FrontendChoice("", stringResource(Res.string.offline))
                    (listOf(offline) + current.choices).forEach { entry ->
                        val checked = current.selected == entry.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .semantics { this.selected = checked }
                                .clickable(role = Role.RadioButton, enabled = !loading) {
                                    loading = true
                                    failed = false
                                    scope.launch {
                                        try {
                                            val updated =
                                                context.callService(
                                                    "ui.frontends/v1",
                                                    buildJsonObject {
                                                        put("select", entry.id)
                                                    },
                                                )
                                            selected(parseFrontendSelection(updated))
                                            dismiss()
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            failed = true
                                        } finally {
                                            loading = false
                                        }
                                    }
                                }.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (checked) Glyph("check", "", Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (failed) Text("That source is no longer available.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss, enabled = !loading || selection == null) { Text("Close") } },
        dismissButton = {},
    )
}

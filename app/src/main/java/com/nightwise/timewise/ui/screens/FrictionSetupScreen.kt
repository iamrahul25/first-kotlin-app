package com.nightwise.timewise.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.friction.FrictionContent
import com.nightwise.timewise.data.friction.FrictionLogic
import com.nightwise.timewise.data.friction.FrictionRepository
import com.nightwise.timewise.data.friction.FrictionStep
import com.nightwise.timewise.data.friction.LovedPhoto
import com.nightwise.timewise.data.friction.MotivationTrack
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.ui.components.FocusTextField
import com.nightwise.timewise.ui.components.FocusToggle
import com.nightwise.timewise.ui.components.LockedPhotoCover
import com.nightwise.timewise.ui.components.OutlineButton
import com.nightwise.timewise.ui.components.PinDialog
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SetPinDialog
import com.nightwise.timewise.ui.components.SmallActionButton
import com.nightwise.timewise.ui.components.TrackArt
import com.nightwise.timewise.ui.components.TrackPlayer
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberStoredImage
import com.nightwise.timewise.ui.components.rememberTrackPlayer
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.ForestDeep
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PHOTOS_PER_PICK = 10
private const val PHOTO_COLUMNS = 3

/** Picks, imports and saves what the extra-time steps show. */
@Composable
fun FrictionSetupRoute(onOpenAwareness: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { FrictionRepository.get(context) }
    val content by repository.content.collectAsState()
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }

    fun import(block: () -> Unit) {
        scope.launch {
            importing = true
            withContext(Dispatchers.IO) { block() }
            importing = false
        }
    }

    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS_PER_PICK)) { uris ->
        if (uris.isNotEmpty()) import { repository.addPhotos(uris) }
    }
    val pickMusic = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) import { repository.addTracks(uris) }
    }

    FrictionSetupScreen(
        content = content,
        importing = importing,
        repository = repository,
        onAddPhotos = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onAddMusic = { pickMusic.launch(arrayOf("audio/*")) },
        onOpenAwareness = onOpenAwareness,
        onBack = onBack
    )
}

@Composable
private fun FrictionSetupScreen(
    content: FrictionContent,
    importing: Boolean,
    repository: FrictionRepository,
    onAddPhotos: () -> Unit,
    onAddMusic: () -> Unit,
    onOpenAwareness: () -> Unit,
    onBack: () -> Unit
) {
    val readyCount = FrictionStep.entries.count { FrictionLogic.isReady(it, content) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(title = "Extra-time steps", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(ForestDeep, Forest)))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "BEFORE ANY EXTRA TIME", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(text = "$readyCount of ${FrictionStep.entries.size} steps ready", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "When an app's limit runs out, getting ${LimitLogic.EXTRA_MINUTES} more minutes means going through each step, one by one. Steps you haven't set up are skipped, so fill in all of them.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        if (importing) Text(text = "Adding…", color = Slate, fontSize = 12.sp)

        StepCard(FrictionStep.PHRASE, content, "Type a sentence like this one, word for word. It's a different one each time.") {
            Text(
                text = "“${FrictionLogic.PHRASES.first()}”",
                color = Ink,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontStyle = FontStyle.Italic
            )
        }

        StepCard(FrictionStep.PEOPLE, content, "Photos of the people you want to do better for: family, your partner, friends. Swipe through all of them. Lock any photo with a PIN so only you can see it.") {
            PeopleSetup(content, repository, onAddPhotos)
        }

        StepCard(FrictionStep.DREAM, content, "What you want to achieve. You'll type one of them out to keep going.") {
            content.dreams.forEach { dream ->
                ListItemRow(text = dream.text, onRemove = { repository.removeDream(dream.id) })
            }
            AddRow(placeholder = "e.g. Run a marathon", onAdd = repository::addDream)
        }

        StepCard(FrictionStep.AWARENESS, content, "Real facts on what screen time does to sleep, focus and health, a quote, and what you gain by cutting back.") {
            Text(
                text = "See all facts & quotes",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenAwareness)
                    .padding(vertical = 4.dp),
                color = Forest,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        StepCard(FrictionStep.TODOS, content, "Things you mean to get done. You'll pick the one you'll do once the extra time is over. Finished ones aren't shown.") {
            content.todos.forEach { todo ->
                ListItemRow(
                    text = todo.text,
                    done = todo.done,
                    onToggleDone = { repository.setTodoDone(todo.id, !todo.done) },
                    onRemove = { repository.removeTodo(todo.id) }
                )
            }
            AddRow(placeholder = "e.g. Finish the report", onAdd = repository::addTodo)
        }

        StepCard(FrictionStep.MUSIC, content, "Motivating songs from your phone. One plays and you listen for ${FrictionLogic.MUSIC_SECONDS} seconds.") {
            MusicSetup(content.tracks, repository, onAddMusic)
        }
    }
}

@Composable
private fun StepCard(
    step: FrictionStep,
    content: FrictionContent,
    description: String,
    body: @Composable () -> Unit
) {
    val ready = FrictionLogic.isReady(step, content)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (ready) Forest else NeutralChip),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "${step.ordinal + 1}", color = if (ready) Color.White else Slate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = step.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                text = when {
                    step.builtIn -> "Always on"
                    ready -> "Ready"
                    else -> "Not set up"
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (ready) Mint else NeutralChip)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = if (ready) Forest else Slate,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(text = description, color = Slate, fontSize = 12.sp, lineHeight = 17.sp)
        body()
    }
}

@Composable
private fun AddRow(placeholder: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val add = {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FocusTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() })
        )
        SmallActionButton(text = "Add", onClick = add)
    }
}

@Composable
private fun ListItemRow(
    text: String,
    onRemove: () -> Unit,
    done: Boolean = false,
    onToggleDone: (() -> Unit)? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onToggleDone != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (done) Forest else Color.Transparent)
                    .border(2.dp, if (done) Forest else CardBorder, CircleShape)
                    .clickable(onClick = onToggleDone),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Rounded.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Forest)
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (done) Mist else Ink,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textDecoration = if (done) TextDecoration.LineThrough else null
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove",
            tint = Mist,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .padding(5.dp)
        )
    }
}

@Composable
private fun PeopleSetup(content: FrictionContent, repository: FrictionRepository, onAddPhotos: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LovedPhoto?>(null) }
    var askPinFor by remember { mutableStateOf<(() -> Unit)?>(null) }
    var setPinThen by remember { mutableStateOf<(() -> Unit)?>(null) }
    var changePin by remember { mutableStateOf(false) }
    var confirmForgot by remember { mutableStateOf(false) }

    val tiles: List<LovedPhoto?> = content.photos + null
    tiles.chunked(PHOTO_COLUMNS).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { photo ->
                val tileModifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                if (photo == null) {
                    Column(
                        modifier = tileModifier
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddPhotos),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp))
                        Text(text = "Add photos", color = Forest, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    PhotoTile(
                        photo = photo,
                        hidden = photo.locked && !revealed,
                        repository = repository,
                        modifier = tileModifier.clickable {
                            if (photo.locked && !revealed) {
                                askPinFor = { revealed = true; editing = photo }
                            } else {
                                editing = photo
                            }
                        }
                    )
                }
            }
            repeat(PHOTO_COLUMNS - row.size) { Box(modifier = Modifier.weight(1f)) }
        }
    }

    if (content.photos.isNotEmpty() || content.hasPin) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NeutralChip)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Slate, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Photo PIN", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (content.hasPin) "Set · locked photos need it" else "Not set · needed to lock a photo",
                    color = Slate,
                    fontSize = 11.sp
                )
            }
            if (content.hasPin) {
                LinkText("Change") { askPinFor = { changePin = true } }
                LinkText("Forgot?") { confirmForgot = true }
            } else {
                LinkText("Set PIN") { setPinThen = {} }
            }
        }
        if (content.hasPin && content.photos.any { it.locked }) {
            LinkText(if (revealed) "Hide locked photos" else "Show locked photos") {
                if (revealed) revealed = false else askPinFor = { revealed = true }
            }
        }
    }

    editing?.let { photo ->
        PhotoDialog(
            photo = content.photos.firstOrNull { it.id == photo.id } ?: photo,
            onSave = { caption, locked ->
                val save = { repository.updatePhoto(photo.id) { it.copy(caption = caption.trim(), locked = locked) } }
                if (locked && !content.hasPin) setPinThen = save else save()
            },
            onDelete = { repository.removePhoto(photo.id) },
            onDismiss = { editing = null }
        )
    }
    askPinFor?.let { then ->
        PinDialog(
            title = "Enter your PIN",
            message = "Your photo PIN unlocks locked photos.",
            onDismiss = { askPinFor = null },
            onSubmit = { pin -> repository.checkPin(pin).also { if (it) then() } },
            confirmLabel = "OK"
        )
    }
    setPinThen?.let { then ->
        SetPinDialog(
            onDismiss = { setPinThen = null },
            onSet = { pin ->
                repository.setPin(pin)
                then()
            }
        )
    }
    if (changePin) {
        SetPinDialog(onDismiss = { changePin = false }, onSet = repository::setPin)
    }
    if (confirmForgot) {
        AlertDialog(
            onDismissRequest = { confirmForgot = false },
            title = { Text("Reset your PIN?") },
            text = { Text("Without the PIN, locked photos can't be shown. Resetting deletes your locked photos and removes the PIN. Other photos stay.") },
            confirmButton = {
                TextButton(onClick = { repository.resetPin(); revealed = false; confirmForgot = false }) { Text("Delete & reset", color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmForgot = false }) { Text("Cancel", color = Slate) } }
        )
    }
}

@Composable
private fun PhotoTile(photo: LovedPhoto, hidden: Boolean, repository: FrictionRepository, modifier: Modifier) {
    Box(modifier = modifier.background(NeutralChip)) {
        if (hidden) {
            LockedPhotoCover(modifier = Modifier.fillMaxSize())
        } else {
            rememberStoredImage(repository.photoFile(photo), maxPx = 360)?.let { image ->
                Image(bitmap = image, contentDescription = photo.caption, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            if (photo.locked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(3.dp)
                )
            }
        }
        if (photo.caption.isNotBlank() && !hidden) {
            Text(
                text = photo.caption,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PhotoDialog(
    photo: LovedPhoto,
    onSave: (caption: String, locked: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var caption by remember(photo.id) { mutableStateOf(photo.caption) }
    var locked by remember(photo.id) { mutableStateOf(photo.locked) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FocusTextField(value = caption, onValueChange = { caption = it }, placeholder = "Who is this? e.g. Mom")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Lock with PIN", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Hidden until your PIN is entered", color = Slate, fontSize = 12.sp)
                    }
                    FocusToggle(checked = locked, onCheckedChange = { locked = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(caption, locked); onDismiss() }) { Text("Save", color = Forest) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(); onDismiss() }) { Text("Delete", color = Red) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Slate) }
            }
        }
    )
}

@Composable
private fun MusicSetup(tracks: List<MotivationTrack>, repository: FrictionRepository, onAddMusic: () -> Unit) {
    val player = rememberTrackPlayer()
    tracks.forEach { track -> TrackRow(track, repository, player) }
    OutlineButton(onClick = onAddMusic) {
        Text(text = "+ Add music", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TrackRow(track: MotivationTrack, repository: FrictionRepository, player: TrackPlayer) {
    val playing = player.current?.id == track.id && player.isPlaying
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TrackArt(repository.artFile(track), size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = track.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val failed = player.current?.id == track.id && player.failed
            Text(
                text = if (failed) "Can't play this file" else track.artist ?: "Unknown artist",
                color = if (failed) Red else Slate,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = Forest,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Mint)
                .clickable {
                    if (player.current?.id == track.id) player.toggle() else player.play(track)
                }
                .padding(6.dp)
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove",
            tint = Mist,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable {
                    if (player.current?.id == track.id) player.stop()
                    repository.removeTrack(track.id)
                }
                .padding(5.dp)
        )
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        color = Forest,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

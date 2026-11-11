package com.nightwise.timewise.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nightwise.timewise.data.friction.FrictionRepository
import com.nightwise.timewise.data.friction.MotivationTrack
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.ForestDeep
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

@Composable
fun FocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Mist, fontSize = 14.sp) },
        singleLine = singleLine,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Forest,
            unfocusedBorderColor = CardBorder,
            errorBorderColor = Red,
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite,
            errorContainerColor = CardWhite,
            cursorColor = Forest,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink
        )
    )
}

/** Asks for the photo PIN; [onSubmit] returns whether it was right. */
@Composable
fun PinDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
    confirmLabel: String = "Unlock"
) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val submit = {
        if (onSubmit(pin)) onDismiss() else {
            wrong = true
            pin = ""
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Forest) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message, color = Slate, fontSize = 13.sp)
                PinField(pin, onChange = { pin = it; wrong = false }, isError = wrong, onDone = { if (pin.length == FrictionRepository.PIN_LENGTH) submit() })
                if (wrong) Text("Wrong PIN, try again.", color = Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = pin.length == FrictionRepository.PIN_LENGTH) {
                Text(confirmLabel, color = Forest)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate) } }
    )
}

/** Picks a new photo PIN, typed twice. */
@Composable
fun SetPinDialog(onDismiss: () -> Unit, onSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val complete = pin.length == FrictionRepository.PIN_LENGTH && confirm.length == FrictionRepository.PIN_LENGTH
    val mismatch = complete && pin != confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Forest) },
        title = { Text("Set a photo PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Locked photos stay hidden until this ${FrictionRepository.PIN_LENGTH}-digit PIN is entered, so only you can see them.",
                    color = Slate,
                    fontSize = 13.sp
                )
                PinField(pin, onChange = { pin = it }, placeholder = "New PIN")
                PinField(confirm, onChange = { confirm = it }, isError = mismatch, placeholder = "Type it again")
                if (mismatch) Text("The PINs don't match.", color = Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(pin); onDismiss() }, enabled = complete && !mismatch) {
                Text("Save", color = Forest)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate) } }
    )
}

@Composable
private fun PinField(
    value: String,
    onChange: (String) -> Unit,
    isError: Boolean = false,
    placeholder: String = "PIN",
    onDone: () -> Unit = {}
) {
    FocusTextField(
        value = value,
        onValueChange = { typed -> onChange(typed.filter(Char::isDigit).take(FrictionRepository.PIN_LENGTH)) },
        placeholder = placeholder,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = PasswordVisualTransformation()
    )
}

private const val IMAGE_CACHE_BYTES = 24 * 1024 * 1024

private val imageCache = object : LruCache<String, ImageBitmap>(IMAGE_CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/** A saved image, decoded off the main thread no larger than [maxPx] and cached. */
@Composable
fun rememberStoredImage(file: File?, maxPx: Int): ImageBitmap? {
    val key = file?.let { "${it.absolutePath}@$maxPx" }
    val image by produceState(key?.let { imageCache.get(it) }, key) {
        if (file == null || key == null) {
            value = null
            return@produceState
        }
        value = imageCache.get(key) ?: withContext(Dispatchers.IO) {
            decodeFile(file, maxPx)?.also { imageCache.put(key, it) }
        }
    }
    return image
}

private fun decodeFile(file: File, maxPx: Int): ImageBitmap? = runCatching {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()

/** Cover art, or a music note on the brand gradient when the file has none. */
@Composable
fun TrackArt(file: File?, size: Dp, modifier: Modifier = Modifier) {
    val art = rememberStoredImage(file, maxPx = 512)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 6))
            .background(Brush.linearGradient(listOf(ForestDeep, Forest))),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            Image(bitmap = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(size / 2.4f))
        }
    }
}

/** Grey card with a lock, standing in for a locked photo. */
@Composable
fun LockedPhotoCover(modifier: Modifier = Modifier, label: String? = null) {
    Column(
        modifier = modifier.background(NeutralChip),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = "Locked photo", tint = Slate, modifier = Modifier.size(28.dp))
        if (label != null) Text(label, color = Slate, fontSize = 12.sp)
    }
}

/** Plays one picked track at a time. State is Compose state, so the UI follows along. */
class TrackPlayer(private val context: Context) {
    var current by mutableStateOf<MotivationTrack?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var onCompletion: (() -> Unit)? = null

    private var player: MediaPlayer? = null
    private var prepared = false

    fun play(track: MotivationTrack) {
        release()
        current = track
        failed = false
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnPreparedListener {
                prepared = true
                it.start()
                this@TrackPlayer.isPlaying = true
            }
            setOnCompletionListener {
                this@TrackPlayer.isPlaying = false
                onCompletion?.invoke()
            }
            setOnErrorListener { _, _, _ ->
                this@TrackPlayer.isPlaying = false
                failed = true
                true
            }
            try {
                setDataSource(context, Uri.parse(track.uri))
                prepareAsync()
            } catch (_: Exception) {
                failed = true
            }
        }
    }

    fun toggle() {
        val p = player ?: return
        if (!prepared || failed) return
        if (isPlaying) p.pause() else p.start()
        isPlaying = !isPlaying
    }

    fun pause() {
        if (isPlaying) toggle()
    }

    fun stop() {
        release()
        current = null
    }

    private fun release() {
        player?.release()
        player = null
        prepared = false
        isPlaying = false
    }
}

/** A [TrackPlayer] that pauses when the screen goes to the background and stops when it leaves. */
@Composable
fun rememberTrackPlayer(): TrackPlayer {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember { TrackPlayer(context.applicationContext) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) player.pause() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.stop()
        }
    }
    return player
}

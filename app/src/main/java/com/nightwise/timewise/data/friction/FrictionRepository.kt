package com.nightwise.timewise.data.friction

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max

/**
 * Photos, dreams, to-dos and music for the extra-time steps. Photos are copied into app
 * storage so they survive the originals being moved; music is read in place.
 */
class FrictionRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val photosDir = File(context.filesDir, "friction_photos")
    private val artDir = File(context.filesDir, "friction_art")

    private val _content = MutableStateFlow(read())
    val content: StateFlow<FrictionContent> = _content.asStateFlow()

    fun photoFile(photo: LovedPhoto): File = File(photosDir, "${photo.id}.jpg")

    fun artFile(track: MotivationTrack): File? = if (track.hasArt) File(artDir, "${track.id}.jpg") else null

    /** Copies picked images in, scaled down. Returns how many were added. Call off the main thread. */
    fun addPhotos(uris: List<Uri>): Int {
        photosDir.mkdirs()
        val added = uris.mapNotNull { uri ->
            val bitmap = decode(uri, PHOTO_MAX_PX) ?: return@mapNotNull null
            val photo = LovedPhoto(newId(), caption = "", locked = false)
            val saved = runCatching {
                photoFile(photo).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            }.isSuccess
            bitmap.recycle()
            photo.takeIf { saved }
        }
        if (added.isNotEmpty()) update { it.copy(photos = it.photos + added) }
        return added.size
    }

    fun updatePhoto(id: String, transform: (LovedPhoto) -> LovedPhoto) =
        update { content -> content.copy(photos = content.photos.map { if (it.id == id) transform(it) else it }) }

    fun removePhoto(id: String) {
        val photo = _content.value.photos.firstOrNull { it.id == id } ?: return
        photoFile(photo).delete()
        update { content -> content.copy(photos = content.photos.filter { it.id != id }) }
    }

    fun addDream(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        update { it.copy(dreams = it.dreams + Dream(newId(), trimmed)) }
    }

    fun removeDream(id: String) = update { content -> content.copy(dreams = content.dreams.filter { it.id != id }) }

    fun addTodo(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        update { it.copy(todos = it.todos + FrictionTodo(newId(), trimmed, done = false)) }
    }

    fun setTodoDone(id: String, done: Boolean) =
        update { content -> content.copy(todos = content.todos.map { if (it.id == id) it.copy(done = done) else it }) }

    fun removeTodo(id: String) = update { content -> content.copy(todos = content.todos.filter { it.id != id }) }

    /** Keeps read access to picked audio and saves each file's cover art. Call off the main thread. */
    fun addTracks(uris: List<Uri>): Int {
        artDir.mkdirs()
        val existing = _content.value.tracks.map { it.uri }.toSet()
        val added = uris.filter { it.toString() !in existing }.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val id = newId()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
                    ?: displayName(uri)?.substringBeforeLast('.')
                    ?: "Track"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
                val hasArt = retriever.embeddedPicture?.let { saveArt(id, it) } ?: false
                MotivationTrack(id, uri.toString(), title, artist, hasArt)
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }
        if (added.isNotEmpty()) update { it.copy(tracks = it.tracks + added) }
        return added.size
    }

    fun removeTrack(id: String) {
        val track = _content.value.tracks.firstOrNull { it.id == id } ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(track.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        artFile(track)?.delete()
        update { content -> content.copy(tracks = content.tracks.filter { it.id != id }) }
    }

    fun checkPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    fun setPin(pin: String) {
        prefs.edit { putString(KEY_PIN_HASH, hash(pin)) }
        _content.value = _content.value.copy(hasPin = true)
    }

    /** Clears the PIN; locked photos become normal ones. */
    fun removePin() {
        prefs.edit { remove(KEY_PIN_HASH) }
        update { content -> content.copy(photos = content.photos.map { it.copy(locked = false) }, hasPin = false) }
    }

    /** For a forgotten PIN: deletes the locked photos along with it. */
    fun resetPin() {
        _content.value.photos.filter { it.locked }.forEach { photoFile(it).delete() }
        prefs.edit { remove(KEY_PIN_HASH) }
        update { content -> content.copy(photos = content.photos.filter { !it.locked }, hasPin = false) }
    }

    @Synchronized
    private fun update(transform: (FrictionContent) -> FrictionContent) {
        val updated = transform(_content.value)
        prefs.edit { putString(KEY_CONTENT, encode(updated)) }
        _content.value = updated
    }

    private fun read(): FrictionContent {
        val hasPin = prefs.getString(KEY_PIN_HASH, null) != null
        val raw = prefs.getString(KEY_CONTENT, null) ?: return FrictionContent(hasPin = hasPin)
        return runCatching { decode(JSONObject(raw)).copy(hasPin = hasPin) }.getOrElse { FrictionContent(hasPin = hasPin) }
    }

    private fun encode(content: FrictionContent): String = JSONObject()
        .put("photos", JSONArray(content.photos.map {
            JSONObject().put("id", it.id).put("caption", it.caption).put("locked", it.locked)
        }))
        .put("dreams", JSONArray(content.dreams.map { JSONObject().put("id", it.id).put("text", it.text) }))
        .put("todos", JSONArray(content.todos.map {
            JSONObject().put("id", it.id).put("text", it.text).put("done", it.done)
        }))
        .put("tracks", JSONArray(content.tracks.map {
            JSONObject().put("id", it.id).put("uri", it.uri).put("title", it.title)
                .put("artist", it.artist ?: "").put("hasArt", it.hasArt)
        }))
        .toString()

    private fun decode(json: JSONObject) = FrictionContent(
        photos = json.objects("photos").map {
            LovedPhoto(it.getString("id"), it.optString("caption"), it.optBoolean("locked"))
        },
        dreams = json.objects("dreams").map { Dream(it.getString("id"), it.getString("text")) },
        todos = json.objects("todos").map {
            FrictionTodo(it.getString("id"), it.getString("text"), it.optBoolean("done"))
        },
        tracks = json.objects("tracks").map {
            MotivationTrack(
                id = it.getString("id"),
                uri = it.getString("uri"),
                title = it.optString("title", "Track"),
                artist = it.optString("artist").takeIf { artist -> artist.isNotEmpty() },
                hasArt = it.optBoolean("hasArt")
            )
        }
    )

    private fun JSONObject.objects(name: String): List<JSONObject> {
        val array = optJSONArray(name) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    /** Decodes [uri] no larger than [maxPx] on its long side, upright on Android 9+. */
    private fun decode(uri: Uri, maxPx: Int): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longSide = max(info.size.width, info.size.height)
                if (longSide > maxPx) {
                    val scale = maxPx.toFloat() / longSide
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), maxPx) }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    }.getOrNull()

    private fun saveArt(id: String, bytes: ByteArray): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), ART_MAX_PX) }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return false
        File(artDir, "$id.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        true
    }.getOrDefault(false)

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun sampleSize(longSide: Int, maxPx: Int): Int {
        var size = 1
        while (longSide / (size * 2) >= maxPx) size *= 2
        return size
    }

    private fun hash(pin: String): String = MessageDigest.getInstance("SHA-256")
        .digest("$PIN_SALT:$pin".toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        private const val PREFS_NAME = "timewise_friction"
        private const val KEY_CONTENT = "content"
        private const val KEY_PIN_HASH = "photo_pin_hash"
        private const val PIN_SALT = "timewise-photo-pin"
        private const val PHOTO_MAX_PX = 1600
        private const val ART_MAX_PX = 512
        private const val JPEG_QUALITY = 88
        const val PIN_LENGTH = 4

        @Volatile
        private var instance: FrictionRepository? = null

        fun get(context: Context): FrictionRepository =
            instance ?: synchronized(this) {
                instance ?: FrictionRepository(context.applicationContext).also { instance = it }
            }
    }
}

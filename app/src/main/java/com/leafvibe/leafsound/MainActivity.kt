package com.leafvibe.leafsound

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONArray

class MainActivity : Activity() {
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var playerListener: Player.Listener? = null
    private lateinit var tracksBox: LinearLayout
    private lateinit var status: TextView
    private val tracks = mutableListOf<Uri>()
    private val names = mutableListOf<String>()
    private val favorites = linkedSetOf<String>()
    private val playlist = linkedSetOf<String>()
    private var currentIndex = -1
    private var filter = FILTER_ALL
    private val green = Color.rgb(34, 139, 87)
    private val dark = Color.rgb(20, 28, 24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLibrary()
        buildInterface()
        connectPlaybackService()
    }

    private fun connectPlaybackService() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get()
                playerListener = object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        status.text = if (isPlaying) "Lecture en cours" else "Lecture en pause"
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        currentIndex = controller?.currentMediaItemIndex ?: -1
                        refreshTracks()
                    }
                }
                controller?.addListener(playerListener!!)
                status.text = "Lecteur prêt · " + tracks.size + " titre(s) mémorisé(s)"
                refreshTracks()
            } catch (e: Exception) {
                status.text = "Connexion au lecteur impossible : " + (e.message ?: "erreur")
            }
        }, mainExecutor)
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 16)
            setBackgroundColor(dark)
        }
        root.addView(TextView(this).apply {
            text = "LeafSound"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        })
        root.addView(TextView(this).apply {
            text = "Ta musique. Ta bibliothèque. Ta liberté."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 14)
        })
        root.addView(actionButton("＋ Ajouter des musiques") { chooseAudio() })
        status = TextView(this).apply {
            text = "Connexion au lecteur…"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 8)
        }
        root.addView(status)

        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        filters.addView(actionButton("Tout") { setFilter(FILTER_ALL) }, weightParams())
        filters.addView(actionButton("♥ Favoris") { setFilter(FILTER_FAVORITES) }, weightParams())
        filters.addView(actionButton("♫ Playlist") { setFilter(FILTER_PLAYLIST) }, weightParams())
        root.addView(filters)

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        controls.addView(actionButton("⏮") { previous() }, weightParams())
        controls.addView(actionButton("▶ / ❚❚") { togglePlayback() }, weightParams())
        controls.addView(actionButton("⏭") { next() }, weightParams())
        root.addView(controls)

        root.addView(TextView(this).apply {
            text = "BIBLIOTHÈQUE"
            textSize = 12f
            setTextColor(Color.rgb(120, 205, 155))
            setPadding(0, 16, 0, 8)
        })
        tracksBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(tracksBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshTracks()
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, -2, 1f).apply {
        setMargins(3, 5, 3, 5)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(green))
            setOnClickListener { action() }
        }

    private fun chooseAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_AUDIO)
    }

    @Deprecated("Use Activity Result API in a later UI refinement")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_AUDIO || resultCode != RESULT_OK || data == null) return
        val selected = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) selected.add(clip.getItemAt(i).uri)
        } ?: data.data?.let { selected.add(it) }
        var added = 0
        selected.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            if (!tracks.contains(uri)) {
                tracks.add(uri)
                names.add(queryDisplayName(uri))
                added++
            }
        }
        saveLibrary()
        filter = FILTER_ALL
        refreshTracks()
        status.text = added.toString() + " nouveau(x) titre(s) ajouté(s) · " + tracks.size + " au total"
    }

    private fun queryDisplayName(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: (uri.lastPathSegment?.substringAfterLast('/') ?: "Fichier audio")
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/') ?: "Fichier audio"
        }
    }

    private fun setFilter(value: String) {
        filter = value
        refreshTracks()
    }

    private fun refreshTracks() {
        if (!::tracksBox.isInitialized) return
        tracksBox.removeAllViews()
        val visible = tracks.indices.filter { index ->
            when (filter) {
                FILTER_FAVORITES -> favorites.contains(tracks[index].toString())
                FILTER_PLAYLIST -> playlist.contains(tracks[index].toString())
                else -> true
            }
        }
        if (visible.isEmpty()) {
            tracksBox.addView(TextView(this).apply {
                text = when (filter) {
                    FILTER_FAVORITES -> "Aucun favori. Utilise le cœur à côté d'un titre."
                    FILTER_PLAYLIST -> "Playlist vide. Ajoute des titres avec le bouton +."
                    else -> "Aucun titre. Utilise « Ajouter des musiques »."
                }
                setTextColor(Color.LTGRAY)
                setPadding(0, 12, 0, 12)
            })
            return
        }
        visible.forEach { index ->
            val uriKey = tracks[index].toString()
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 2, 0, 2)
            }
            val title = TextView(this).apply {
                text = (if (index == currentIndex) "♫  " else "♪  ") + names[index]
                textSize = 14f
                setTextColor(if (index == currentIndex) Color.rgb(120, 220, 160) else Color.WHITE)
                setPadding(8, 12, 4, 12)
                setOnClickListener { play(index) }
            }
            line.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(actionButton(if (favorites.contains(uriKey)) "♥" else "♡") {
                if (!favorites.add(uriKey)) favorites.remove(uriKey)
                saveLibrary()
                refreshTracks()
            })
            line.addView(actionButton(if (playlist.contains(uriKey)) "−" else "+") {
                if (!playlist.add(uriKey)) playlist.remove(uriKey)
                saveLibrary()
                refreshTracks()
            })
            tracksBox.addView(line)
            tracksBox.addView(View(this).apply { setBackgroundColor(Color.DKGRAY) },
                LinearLayout.LayoutParams(-1, 1))
        }
    }

    private fun play(index: Int) {
        if (index !in tracks.indices) return
        val mediaController = controller ?: run {
            status.text = "Le lecteur se connecte encore…"
            return
        }
        currentIndex = index
        val items = tracks.indices.map { i ->
            MediaItem.Builder()
                .setUri(tracks[i])
                .setMediaMetadata(MediaMetadata.Builder().setTitle(names[i]).setArtist("LeafSound").build())
                .build()
        }
        mediaController.setMediaItems(items, index, 0L)
        mediaController.prepare()
        mediaController.play()
        status.text = "Lecture : " + names[index]
        refreshTracks()
    }

    private fun togglePlayback() {
        val p = controller ?: return
        when {
            tracks.isEmpty() -> status.text = "Ajoute d'abord un fichier audio."
            p.isPlaying -> p.pause()
            p.mediaItemCount > 0 -> p.play()
            else -> play(if (currentIndex >= 0) currentIndex else 0)
        }
    }

    private fun next() {
        val p = controller
        if (p != null && p.mediaItemCount > 0) p.seekToNextMediaItem()
        else if (tracks.isNotEmpty()) play((currentIndex + 1).mod(tracks.size))
    }

    private fun previous() {
        val p = controller
        if (p != null && p.mediaItemCount > 0) p.seekToPreviousMediaItem()
        else if (tracks.isNotEmpty()) play(if (currentIndex <= 0) tracks.lastIndex else currentIndex - 1)
    }

    private fun loadLibrary() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val savedUris = readStringArray(prefs.getString(KEY_URIS, null))
        val savedNames = readStringArray(prefs.getString(KEY_NAMES, null))
        savedUris.forEachIndexed { index, value ->
            try {
                val uri = Uri.parse(value)
                contentResolver.openFileDescriptor(uri, "r")?.close()
                tracks.add(uri)
                names.add(savedNames.getOrNull(index) ?: queryDisplayName(uri))
            } catch (_: Exception) {
                // Ignore unavailable files or revoked document permissions.
            }
        }
        favorites.addAll(readStringArray(prefs.getString(KEY_FAVORITES, null)))
        playlist.addAll(readStringArray(prefs.getString(KEY_PLAYLIST, null)))
        val valid = tracks.map { it.toString() }.toSet()
        favorites.retainAll(valid)
        playlist.retainAll(valid)
    }

    private fun readStringArray(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf { s -> s.isNotBlank() } }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveLibrary() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        fun encode(values: Collection<String>) = JSONArray(values).toString()
        prefs.edit()
            .putString(KEY_URIS, encode(tracks.map { it.toString() }))
            .putString(KEY_NAMES, encode(names))
            .putString(KEY_FAVORITES, encode(favorites))
            .putString(KEY_PLAYLIST, encode(playlist))
            .apply()
    }

    override fun onDestroy() {
        playerListener?.let { controller?.removeListener(it) }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_AUDIO = 4101
        private const val PREFS = "leafsound_library"
        private const val KEY_URIS = "uris"
        private const val KEY_NAMES = "names"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAYLIST = "playlist"
        private const val FILTER_ALL = "all"
        private const val FILTER_FAVORITES = "favorites"
        private const val FILTER_PLAYLIST = "playlist"
    }
}

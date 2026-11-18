package com.leafvibe.leafsound

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var searchField: EditText
    private var searchQuery = ""
    private val tracks = mutableListOf<Uri>()
    private val names = mutableListOf<String>()
    private val favorites = linkedSetOf<String>()
    private val playlists = linkedMapOf<String, LinkedHashSet<String>>()
    private var activePlaylistName = "Ma playlist"
    private var repeatLimit = 0
    private var repeatCount = 0
    private var repeatScope = "none"
    private var lastQueueIndex = -1
    private var currentIndex = -1
    private var filter = FILTER_ALL
    private val green = Color.rgb(34, 139, 87)
    private val dark = Color.rgb(20, 28, 24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLibrary()
        requestNotificationPermission()
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
                        val p = controller
                        val now = p?.currentMediaItemIndex ?: -1
                        if (repeatScope == "playlist" && now == 0 && lastQueueIndex == (p?.mediaItemCount ?: 0) - 1) {
                            repeatCount++
                            if (repeatLimit > 0 && repeatCount >= repeatLimit) { p?.repeatMode = Player.REPEAT_MODE_OFF; repeatScope = "none"; status.text = "Répétition de la playlist terminée." }
                        } else if (repeatScope == "track" && reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                            repeatCount++
                            if (repeatLimit > 0 && repeatCount >= repeatLimit) { p?.repeatMode = Player.REPEAT_MODE_OFF; repeatScope = "none"; status.text = "Répétition du titre terminée." }
                        }
                        lastQueueIndex = now
                        currentIndex = now
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
        searchField = EditText(this).apply {
            hint = "Rechercher un titre…"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                    refreshTracks()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(searchField)

        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        filters.addView(actionButton("Tout") { setFilter(FILTER_ALL) }, weightParams())
        filters.addView(actionButton("♥ Favoris") { setFilter(FILTER_FAVORITES) }, weightParams())
        filters.addView(actionButton("♫ Playlist") { setFilter(FILTER_PLAYLIST) }, weightParams())
        root.addView(filters)
        val playlistActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        playlistActions.addView(actionButton("＋ Gérer playlists") { managePlaylists() }, weightParams())
        playlistActions.addView(actionButton("▶ Lire playlist") { playActivePlaylist() }, weightParams())
        root.addView(playlistActions)

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        controls.addView(actionButton("⏮") { previous() }, weightParams())
        controls.addView(actionButton("▶ / ❚❚") { togglePlayback() }, weightParams())
        controls.addView(actionButton("⏭") { next() }, weightParams())
        root.addView(controls)
        val playbackModes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        playbackModes.addView(actionButton("Aléatoire") {
            controller?.let { player ->
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                status.text = "Lecture aléatoire : " + (if (player.shuffleModeEnabled) "activée" else "désactivée")
            }
        }, weightParams())
        playbackModes.addView(actionButton("Répéter 1× à ∞") { chooseRepeat() }, weightParams())
        root.addView(playbackModes)

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


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }

    private fun managePlaylists() {
        val options = arrayOf("Créer", "Choisir", "Renommer", "Supprimer")
        AlertDialog.Builder(this).setTitle("Playlists : " + activePlaylistName).setItems(options) { _, which ->
            when (which) {
                0 -> editPlaylistName("Nouvelle playlist", "") { name -> if (name.isNotBlank() && !playlists.containsKey(name)) { playlists[name] = linkedSetOf(); activePlaylistName = name; saveLibrary(); refreshTracks() } }
                1 -> {
                    val names = playlists.keys.toTypedArray()
                    AlertDialog.Builder(this).setTitle("Choisir").setItems(names) { _, i -> activePlaylistName = names[i]; saveLibrary(); setFilter(FILTER_PLAYLIST) }.show()
                }
                2 -> editPlaylistName("Renommer", activePlaylistName) { name -> if (name.isNotBlank() && (name == activePlaylistName || !playlists.containsKey(name))) { val data = playlists.remove(activePlaylistName) ?: linkedSetOf(); playlists[name] = data; activePlaylistName = name; saveLibrary(); refreshTracks() } }
                3 -> if (playlists.size > 1) AlertDialog.Builder(this).setMessage("Supprimer cette playlist ? Les fichiers audio restent intacts.").setNegativeButton("Annuler", null).setPositiveButton("Supprimer") { _, _ -> playlists.remove(activePlaylistName); activePlaylistName = playlists.keys.first(); saveLibrary(); refreshTracks() }.show() else status.text = "Il faut conserver une playlist."
            }
        }.setNegativeButton("Fermer", null).show()
    }

    private fun editPlaylistName(title: String, initial: String, done: (String) -> Unit) {
        val field = EditText(this).apply { setText(initial); hint = "Nom" }
        AlertDialog.Builder(this).setTitle(title).setView(field).setNegativeButton("Annuler", null).setPositiveButton("OK") { _, _ -> done(field.text.toString().trim()) }.show()
    }

    private fun playActivePlaylist() {
        val ids = tracks.indices.filter { playlists[activePlaylistName]?.contains(tracks[it].toString()) == true }
        if (ids.isEmpty()) { status.text = "Playlist vide."; return }
        val p = controller ?: return
        val items = ids.map { i -> MediaItem.Builder().setUri(tracks[i]).setMediaMetadata(MediaMetadata.Builder().setTitle(names[i]).setArtist("LeafSound · " + activePlaylistName).build()).build() }
        p.setMediaItems(items, 0, 0L); p.prepare(); p.play(); currentIndex = ids.first(); lastQueueIndex = 0
        status.text = "Lecture : " + activePlaylistName
    }

    private fun chooseRepeat() {
        AlertDialog.Builder(this).setTitle("Répéter").setItems(arrayOf("Ce titre", "La playlist active")) { _, scope ->
            val choices = arrayOf("Désactivé", "1×", "2×", "3×", "4×", "5×", "∞")
            AlertDialog.Builder(this).setTitle("Nombre de répétitions").setItems(choices) { _, n ->
                val p = controller ?: return@setItems
                if (scope == 1) playActivePlaylist()
                repeatLimit = when (n) { 0 -> 0; 6 -> -1; else -> n }
                repeatCount = 0
                repeatScope = if (repeatLimit == 0) "none" else if (scope == 0) "track" else "playlist"
                p.repeatMode = when { repeatLimit == 0 -> Player.REPEAT_MODE_OFF; scope == 0 -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_ALL }
                status.text = if (repeatLimit == 0) "Répétition désactivée" else "Répétition activée : " + (if (repeatLimit < 0) "infinie" else repeatLimit.toString() + " fois")
            }.show()
        }.setNegativeButton("Annuler", null).show()
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
            val matchesFilter = when (filter) {
                FILTER_FAVORITES -> favorites.contains(tracks[index].toString())
                FILTER_PLAYLIST -> playlists[activePlaylistName]?.contains(tracks[index].toString()) == true
                else -> true
            }
            matchesFilter && names[index].lowercase().contains(searchQuery)
        }
        if (visible.isEmpty()) {
            tracksBox.addView(TextView(this).apply {
                text = when (filter) {
                    FILTER_FAVORITES -> "Aucun favori. Utilise le cœur à côté d'un titre."
                    FILTER_PLAYLIST -> "Playlist « " + activePlaylistName + " » vide. Ajoute des titres avec le bouton +."
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
            line.addView(actionButton(if (playlists[activePlaylistName]?.contains(uriKey) == true) "−" else "+") {
                val selected = playlists.getOrPut(activePlaylistName) { linkedSetOf() }
                if (!selected.add(uriKey)) selected.remove(uriKey)
                saveLibrary()
                refreshTracks()
            })
            line.addView(actionButton("✕") { removeTrack(index) })
            tracksBox.addView(line)
            tracksBox.addView(View(this).apply { setBackgroundColor(Color.DKGRAY) },
                LinearLayout.LayoutParams(-1, 1))
        }
    }

    private fun removeTrack(index: Int) {
        if (index !in tracks.indices) return
        val removed = tracks.removeAt(index)
        names.removeAt(index)
        favorites.remove(removed.toString())
        playlists.values.forEach { it.remove(removed.toString()) }
        if (currentIndex == index) {
            controller?.stop()
            controller?.clearMediaItems()
            currentIndex = -1
        } else if (currentIndex > index) currentIndex--
        saveLibrary()
        refreshTracks()
        status.text = "Titre retiré de la bibliothèque (le fichier original est conservé)."
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
        val raw = prefs.getString(KEY_PLAYLISTS, null)
        if (!raw.isNullOrBlank()) try {
            val obj = org.json.JSONObject(raw)
            obj.keys().forEach { name -> playlists[name] = readStringArray(obj.optJSONArray(name)?.toString()).toCollection(linkedSetOf()) }
        } catch (_: Exception) {}
        if (playlists.isEmpty()) {
            val legacy = readStringArray(prefs.getString("playlist", null)).toCollection(linkedSetOf())
            playlists["Ma playlist"] = legacy
        }
        activePlaylistName = prefs.getString(KEY_ACTIVE_PLAYLIST, null)?.takeIf { playlists.containsKey(it) } ?: playlists.keys.first()
        val valid = tracks.map { it.toString() }.toSet()
        favorites.retainAll(valid)
        playlists.values.forEach { it.retainAll(valid) }
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
        val savedLists = org.json.JSONObject()
        playlists.forEach { (name, values) -> savedLists.put(name, JSONArray(values)) }
        prefs.edit().putString(KEY_URIS, encode(tracks.map { it.toString() }))
            .putString(KEY_NAMES, encode(names)).putString(KEY_FAVORITES, encode(favorites))
            .putString(KEY_PLAYLISTS, savedLists.toString()).putString(KEY_ACTIVE_PLAYLIST, activePlaylistName).apply()
    }

    override fun onDestroy() {
        playerListener?.let { controller?.removeListener(it) }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_AUDIO = 4101
        private const val REQUEST_NOTIFICATIONS = 4102
        private const val PREFS = "leafsound_library"
        private const val KEY_URIS = "uris"
        private const val KEY_NAMES = "names"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_ACTIVE_PLAYLIST = "active_playlist"
        private const val FILTER_ALL = "all"
        private const val FILTER_FAVORITES = "favorites"
        private const val FILTER_PLAYLIST = "playlist"
    }
}

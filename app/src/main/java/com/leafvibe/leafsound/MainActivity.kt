package com.leafvibe.leafsound

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.content.ComponentName
import android.content.Intent
import android.app.DownloadManager
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
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
import android.app.Dialog
import android.view.Window
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.DownloadListener
import android.webkit.CookieManager
import android.webkit.WebSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
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
    private val categories = linkedMapOf<String, LinkedHashSet<String>>()
    private var activeCategoryName = ""
    private var browserDialog: Dialog? = null
    private var downloadFolderUri: Uri? = null
    private var pendingDownload: PendingDownload? = null
    private val downloadExecutor = Executors.newSingleThreadExecutor()
    private var activePlaylistName = "Ma playlist"
    private var repeatLimit = 0
    private var repeatCount = 0
    private var repeatScope = "none"
    private var lastQueueIndex = -1
    private var currentIndex = -1
    private var pendingExportUri: Uri? = null
    private var filter = FILTER_ALL
    private val green = LeafColour.OASIS
    private val dark = LeafColour.CREAM_DARK

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
            setTextColor(LeafColour.CREAM_DARK)
            setBackgroundColor(LeafColour.OASIS)
            setPadding(12, 12, 12, 12)
            gravity = Gravity.CENTER_VERTICAL
        })
        root.addView(TextView(this).apply {
            text = "Ta musique. Ta bibliothèque. Ta liberté."
            textSize = 14f
            setTextColor(LeafColour.WALNUT)
            setPadding(0, 4, 0, 14)
        })
        root.addView(actionButton("＋ Ajouter des musiques") { chooseAudio() })
        root.addView(actionButton("🌐 LeafBrowser · sites & téléchargements") { openLeafBrowser() })
        val transferActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        transferActions.addView(actionButton("Sauvegarde JSON") { exportLibraryJson() }, weightParams())
        transferActions.addView(actionButton("Importer JSON") { importLibraryJson() }, weightParams())
        root.addView(transferActions)
        val fileActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fileActions.addView(actionButton("Exporter les audios") { chooseAudioExportFolder() }, weightParams())
        fileActions.addView(actionButton("Télécharger") { downloadAudio() }, weightParams())
        root.addView(fileActions)
        status = TextView(this).apply {
            text = "Connexion au lecteur…"
            textSize = 13f
            setTextColor(LeafColour.WALNUT)
            setPadding(0, 12, 0, 8)
        }
        root.addView(status)
        searchField = EditText(this).apply {
            hint = "Rechercher un titre…"
            setHintTextColor(LeafColour.SAND_BROWN)
            setTextColor(LeafColour.WALNUT)
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
        root.addView(actionButton("▦ Gérer les univers musicaux") { manageCategories() })
        val universStrip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        universStrip.addView(actionButton("Tout") { setFilter(FILTER_ALL) }, weightParams())
        categories.keys.take(4).forEach { name -> universStrip.addView(actionButton(name) { playCategory(name) }, weightParams()) }
        root.addView(universStrip)

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
            setTextColor(LeafColour.CREAM_DARK)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(LeafColour.WALNUT))
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


    // A "univers" is a broad, user-created music space (Rock, Chill, Road, Jazz…).
    // Playlists remain separate, named listening sequences.
    private fun manageCategories() {
        val actions = arrayOf("Créer un univers", "Parcourir les univers", "Renommer l'univers actif", "Supprimer l'univers actif", "Affecter des morceaux")
        AlertDialog.Builder(this).setTitle("Univers musicaux").setItems(actions) { _, choice ->
            when (choice) {
                0 -> editCategory("Créer un univers", "", "🎵") { name, _ ->
                    if (name.isNotBlank() && !categories.containsKey(name)) {
                        categories[name] = linkedSetOf(); activeCategoryName = name
                        saveLibrary(); refreshTracks()
                    }
                }
                1 -> {
                    val all = categories.keys.toTypedArray()
                    if (all.isEmpty()) status.text = "Crée ton premier univers musical."
                    else AlertDialog.Builder(this).setTitle("Choisir un univers").setItems(all) { _, i ->
                        activeCategoryName = all[i]; filter = CATEGORY_PREFIX + activeCategoryName; refreshTracks()
                    }.show()
                }
                2 -> if (activeCategoryName.isNotBlank()) editCategory("Renommer", activeCategoryName, "🎵") { name, _ ->
                    if (name.isNotBlank() && (name == activeCategoryName || !categories.containsKey(name))) {
                        val values = categories.remove(activeCategoryName) ?: linkedSetOf()
                        categories[name] = values; activeCategoryName = name
                        if (filter.startsWith(CATEGORY_PREFIX)) filter = CATEGORY_PREFIX + name
                        saveLibrary(); refreshTracks()
                    }
                }
                3 -> if (activeCategoryName.isNotBlank()) AlertDialog.Builder(this)
                    .setMessage("Supprimer cet univers ? Les morceaux et playlists restent conservés.")
                    .setNegativeButton("Annuler", null).setPositiveButton("Supprimer") { _, _ ->
                        categories.remove(activeCategoryName); activeCategoryName = categories.keys.firstOrNull().orEmpty()
                        if (filter.startsWith(CATEGORY_PREFIX)) filter = FILTER_ALL
                        saveLibrary(); refreshTracks()
                    }.show()
                4 -> if (activeCategoryName.isNotBlank()) chooseCategoryTracks(activeCategoryName)
            }
        }.setNegativeButton("Fermer", null).show()
    }

    private fun editCategory(title: String, initial: String, emoji: String, done: (String, String) -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 6, 18, 0) }
        val name = EditText(this).apply { hint = "Nom : Rock, Chill, Voiture…"; setText(initial) }
        val icon = EditText(this).apply { hint = "Emoji"; setText(emoji); setSingleLine(true) }
        box.addView(name); box.addView(icon)
        AlertDialog.Builder(this).setTitle(title).setView(box).setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer") { _, _ -> done(name.text.toString().trim(), icon.text.toString().trim()) }.show()
    }

    private fun chooseCategoryTracks(category: String) {
        val labels = names.toTypedArray()
        if (labels.isEmpty()) { status.text = "Ajoute d'abord des morceaux."; return }
        val checked = BooleanArray(tracks.size) { categories[category]?.contains(tracks[it].toString()) == true }
        AlertDialog.Builder(this).setTitle("Morceaux · $category")
            .setMultiChoiceItems(labels, checked) { _, i, isChecked ->
                val set = categories.getOrPut(category) { linkedSetOf() }
                if (isChecked) set.add(tracks[i].toString()) else set.remove(tracks[i].toString())
            }.setPositiveButton("Enregistrer") { _, _ -> saveLibrary(); refreshTracks() }
            .setNegativeButton("Annuler", null).show()
    }

    private fun assignTrackToCategory(index: Int) {
        if (categories.isEmpty()) { manageCategories(); return }
        val labels = categories.keys.toTypedArray()
        val checked = BooleanArray(labels.size) { categories[labels[it]]?.contains(tracks[index].toString()) == true }
        AlertDialog.Builder(this).setTitle("Univers de « ${names[index]} »")
            .setMultiChoiceItems(labels, checked) { _, i, yes ->
                val set = categories.getOrPut(labels[i]) { linkedSetOf() }
                if (yes) set.add(tracks[index].toString()) else set.remove(tracks[index].toString())
            }.setPositiveButton("Enregistrer") { _, _ -> saveLibrary(); refreshTracks() }
            .setNegativeButton("Annuler", null).show()
    }

    private fun playCategory(category: String) {
        val indexes = tracks.indices.filter { categories[category]?.contains(tracks[it].toString()) == true }
        if (indexes.isEmpty()) { status.text = "L'univers « $category » est vide."; return }
        val p = controller ?: return
        val queue = indexes.map { i ->
            MediaItem.Builder().setUri(tracks[i]).setMediaMetadata(
                MediaMetadata.Builder().setTitle(names[i]).setArtist("LeafSound · $category").build()
            ).build()
        }
        p.setMediaItems(queue, 0, 0L); p.prepare(); p.play()
        currentIndex = indexes.first(); lastQueueIndex = 0; activeCategoryName = category
        saveLibrary()
        status.text = "Lecture de l'univers : $category"
    }

    private fun openLeafBrowser() {
        val dialog = Dialog(this)
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(LeafColour.CREAM_DARK) }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(6, 6, 6, 6); setBackgroundColor(LeafColour.OASIS) }
        val address = EditText(this).apply {
            hint = "Adresse du site légal (HTTPS)"
            setSingleLine(true); textSize = 13f
            setTextColor(LeafColour.WALNUT); setHintTextColor(LeafColour.SAND_BROWN)
            setBackgroundColor(LeafColour.CREAM_DARK)
        }
        lateinit var browser: WebView
        val open = actionButton("OK") {
            var url = address.text.toString().trim()
            if (url.isNotBlank()) {
                if (!url.contains("://")) url = "https://$url"
                val parsed = Uri.parse(url)
                if (parsed.scheme == "https" && !parsed.host.isNullOrBlank()) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_LAST_WEB, url).apply()
                    browser.loadUrl(url)
                } else status.text = "Adresse refusée : LeafBrowser utilise HTTPS."
            }
        }
        bar.addView(address, LinearLayout.LayoutParams(0, -2, 1f)); bar.addView(open)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(actionButton("‹ Retour") { if (browser.canGoBack()) browser.goBack() }, weightParams())
        controls.addView(actionButton("☆ Favori") { saveBrowserBookmark(browser) }, weightParams())
        controls.addView(actionButton("Sites") { showBrowserBookmarks(browser) }, weightParams())
        controls.addView(actionButton("SD") { chooseDownloadFolder() }, weightParams())
        browser = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                }
            }
            webChromeClient = WebChromeClient()
            var startX = 0f
            var startY = 0f
            setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y; false }
                    android.view.MotionEvent.ACTION_UP -> {
                        val edge = 24 * resources.displayMetrics.density
                        val dx = event.x - startX
                        val dy = kotlin.math.abs(event.y - startY)
                        if (startX <= edge && dx > 100 * resources.displayMetrics.density &&
                            dy < 70 * resources.displayMetrics.density && canGoBack()) {
                            goBack(); true
                        } else false
                    }
                    else -> false
                }
            }
            setDownloadListener(DownloadListener { url, agent, disposition, mime, _ ->
                val uri = Uri.parse(url)
                if (uri.scheme == "https") {
                    pendingDownload = PendingDownload(url, agent, disposition, mime)
                    if (downloadFolderUri == null) chooseDownloadFolder() else savePendingDownload()
                } else status.text = "Téléchargement bloqué : lien non HTTPS."
            })
        }
        page.addView(bar); page.addView(controls)
        page.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(page)
        dialog.setOnKeyListener { _, key, event ->
            if (key == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && browser.canGoBack()) {
                browser.goBack(); true
            } else false
        }
        dialog.setOnDismissListener { browser.destroy(); browserDialog = null }
        browserDialog = dialog
        dialog.show(); dialog.window?.setLayout(-1, -1)
        val last = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LAST_WEB, null)
        if (!last.isNullOrBlank()) { address.setText(last); browser.loadUrl(last) }
    }


    private fun saveBrowserBookmark(browser: WebView) {
        val url = browser.url ?: return
        if (!url.startsWith("https://")) return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(KEY_BOOKMARKS, "[]")) }.getOrElse { JSONArray() }
        for (i in 0 until array.length()) if (array.optJSONObject(i)?.optString("url") == url) {
            status.text = "Site déjà enregistré."; return
        }
        array.put(JSONObject().put("title", browser.title ?: Uri.parse(url).host).put("url", url))
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
        status.text = "Site ajouté aux favoris LeafBrowser."
    }

    private fun showBrowserBookmarks(browser: WebView) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(KEY_BOOKMARKS, "[]")) }.getOrElse { JSONArray() }
        if (array.length() == 0) { status.text = "Aucun site enregistré pour le moment."; return }
        val labels = Array(array.length()) { i ->
            val item = array.optJSONObject(i) ?: JSONObject()
            item.optString("title", item.optString("url"))
        }
        AlertDialog.Builder(this).setTitle("Mes sites enregistrés")
            .setItems(labels) { _, index ->
                val url = array.optJSONObject(index)?.optString("url").orEmpty()
                if (url.startsWith("https://")) browser.loadUrl(url)
            }.setNeutralButton("Gérer") { _, _ ->
                val options = Array(array.length()) { i -> labels[i] + " · supprimer" }
                AlertDialog.Builder(this).setTitle("Supprimer un raccourci")
                    .setItems(options) { _, index ->
                        val rebuilt = JSONArray()
                        for (i in 0 until array.length()) if (i != index) rebuilt.put(array.get(i))
                        prefs.edit().putString(KEY_BOOKMARKS, rebuilt.toString()).apply()
                    }.show()
            }.setNegativeButton("Fermer", null).show()
    }

    private fun chooseDownloadFolder() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_DOWNLOAD_FOLDER)
    }

    private fun savePendingDownload() {
        val task = pendingDownload ?: return
        val folderUri = downloadFolderUri ?: return
        downloadExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(task.url).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true; connectTimeout = 15000; readTimeout = 30000
                    setRequestProperty("User-Agent", task.userAgent)
                    CookieManager.getInstance().getCookie(task.url)?.let { setRequestProperty("Cookie", it) }
                }
                connection.connect()
                require(connection.responseCode in 200..299) { "Réponse HTTP ${connection.responseCode}" }
                val filename = URLUtil.guessFileName(task.url, task.disposition, task.mime)
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "audio-download" }
                val folder = DocumentFile.fromTreeUri(this, folderUri) ?: error("Dossier SD inaccessible")
                val file = folder.createFile(task.mime ?: "application/octet-stream", filename) ?: error("Création impossible")
                contentResolver.openOutputStream(file.uri, "w").use { output ->
                    requireNotNull(output) { "Écriture impossible" }
                    connection.inputStream.use { input -> input.copyTo(output) }
                }
                runOnUiThread {
                    if (!tracks.contains(file.uri)) { tracks.add(file.uri); names.add(filename); saveLibrary(); refreshTracks() }
                    status.text = "Téléchargement enregistré sur la SD et ajouté à LeafSound : $filename"
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Téléchargement impossible : ${e.message ?: "erreur"}" }
            } finally { connection?.disconnect(); pendingDownload = null }
        }
    }

    private data class PendingDownload(val url: String, val userAgent: String, val disposition: String?, val mime: String?)

    private fun exportLibraryJson() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "LeafSound-sauvegarde.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT_JSON)
    }

    private fun importLibraryJson() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_IMPORT_JSON)
    }

    private fun buildBackupJson(): JSONObject {
        val root = JSONObject()
        root.put("format", "LeafSoundBackup")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val items = JSONArray()
        tracks.forEachIndexed { index, uri ->
            items.put(JSONObject().put("uri", uri.toString()).put("name", names.getOrNull(index) ?: queryDisplayName(uri)))
        }
        root.put("tracks", items)
        root.put("favorites", JSONArray(favorites))
        val lists = JSONObject()
        playlists.forEach { (name, values) -> lists.put(name, JSONArray(values)) }
        root.put("playlists", lists)
        root.put("activePlaylist", activePlaylistName)
        val categoryJson = JSONObject()
        categories.forEach { (name, values) -> categoryJson.put(name, JSONArray(values)) }
        root.put("categories", categoryJson)
        root.put("activeCategory", activeCategoryName)
        return root
    }

    private fun writeLibraryJson(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(buildBackupJson().toString(2))
            } ?: throw IllegalStateException("Impossible d'ouvrir le fichier de destination")
            status.text = "Sauvegarde JSON exportée. Les fichiers audio sont exportés séparément."
            Toast.makeText(this, "Sauvegarde LeafSound créée", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            status.text = "Échec de l'export JSON : " + (e.message ?: "erreur")
        }
    }

    private fun readLibraryJson(uri: Uri) {
        try {
            val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IllegalStateException("Fichier illisible")
            val root = JSONObject(raw)
            require(root.optString("format") == "LeafSoundBackup") { "Ce fichier n'est pas une sauvegarde LeafSound." }
            require(root.optInt("schemaVersion", 0) == 1) { "Version de sauvegarde non prise en charge." }
            var added = 0
            val importedKeys = linkedMapOf<String, String>()
            val array = root.optJSONArray("tracks") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val value = item.optString("uri")
                if (value.isBlank()) continue
                val trackUri = Uri.parse(value)
                try {
                    contentResolver.openFileDescriptor(trackUri, "r")?.close()
                    if (!tracks.contains(trackUri)) {
                        tracks.add(trackUri)
                        names.add(item.optString("name").ifBlank { queryDisplayName(trackUri) })
                        added++
                    }
                    importedKeys[value] = value
                } catch (_: Exception) {
                    // SAF permissions are device-specific; inaccessible audio is not silently added.
                }
            }
            val importedFavorites = root.optJSONArray("favorites") ?: JSONArray()
            for (i in 0 until importedFavorites.length()) {
                val key = importedFavorites.optString(i)
                if (importedKeys.containsKey(key)) favorites.add(key)
            }
            val importedLists = root.optJSONObject("playlists") ?: JSONObject()
            val keys = importedLists.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                val values = importedLists.optJSONArray(name) ?: JSONArray()
                val destination = playlists.getOrPut(name) { linkedSetOf() }
                for (i in 0 until values.length()) {
                    val key = values.optString(i)
                    if (importedKeys.containsKey(key)) destination.add(key)
                }
            }
            val importedCategories = root.optJSONObject("categories") ?: JSONObject()
            importedCategories.keys().forEach { category ->
                val values = importedCategories.optJSONArray(category) ?: JSONArray()
                val destination = categories.getOrPut(category) { linkedSetOf() }
                for (i in 0 until values.length()) {
                    val key = values.optString(i)
                    if (importedKeys.containsKey(key)) destination.add(key)
                }
            }
            val importedActiveCategory = root.optString("activeCategory")
            if (categories.containsKey(importedActiveCategory)) activeCategoryName = importedActiveCategory
            val requestedActive = root.optString("activePlaylist")
            if (playlists.containsKey(requestedActive)) activePlaylistName = requestedActive
            saveLibrary()
            refreshTracks()
            status.text = "Import JSON terminé : $added fichier(s) audio retrouvé(s). Les URI protégées par Android doivent être réautorisées si elles viennent d'un autre appareil."
        } catch (e: Exception) {
            status.text = "Import JSON impossible : " + (e.message ?: "fichier invalide")
        }
    }

    private fun chooseAudioExportFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_EXPORT_AUDIO_FOLDER)
    }

    private fun exportAudioToFolder(treeUri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val folder = DocumentFile.fromTreeUri(this, treeUri) ?: throw IllegalStateException("Dossier inaccessible")
            var copied = 0
            var failed = 0
            tracks.forEachIndexed { index, source ->
                try {
                    val display = names.getOrNull(index) ?: "audio-$index"
                    val safeName = display.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "audio-$index" }
                    val mime = contentResolver.getType(source) ?: "audio/mpeg"
                    val target = folder.createFile(mime, safeName) ?: throw IllegalStateException("Création impossible")
                    contentResolver.openInputStream(source).use { input ->
                        requireNotNull(input) { "Lecture impossible" }
                        contentResolver.openOutputStream(target.uri, "w").use { output ->
                            requireNotNull(output) { "Écriture impossible" }
                            input.copyTo(output)
                        }
                    }
                    copied++
                } catch (_: Exception) { failed++ }
            }
            status.text = "Export audio terminé : $copied copié(s), $failed échec(s). Les originaux sont conservés."
        } catch (e: Exception) {
            status.text = "Export audio impossible : " + (e.message ?: "erreur")
        }
    }

    private fun downloadAudio() {
        val field = EditText(this).apply {
            hint = "https://…/musique.mp3"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(this).setTitle("Télécharger un fichier audio autorisé")
            .setMessage("Indique un lien direct vers un fichier audio que tu as le droit de télécharger. Les flux Spotify/Deezer et contenus protégés ne sont pas pris en charge.")
            .setView(field).setNegativeButton("Annuler", null)
            .setPositiveButton("Télécharger") { _, _ ->
                val link = field.text.toString().trim()
                try {
                    val uri = Uri.parse(link)
                    require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "Utilise un lien HTTPS direct." }
                    val request = DownloadManager.Request(uri)
                        .setTitle(URLUtil.guessFileName(link, null, null))
                        .setDescription("Téléchargement LeafSound")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "LeafSound/" + URLUtil.guessFileName(link, null, null))
                        .setAllowedOverMetered(true).setAllowedOverRoaming(false)
                    val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    manager.enqueue(request)
                    status.text = "Téléchargement lancé. Le fichier sera dans Musique/LeafSound."
                } catch (e: Exception) {
                    status.text = "Téléchargement refusé : " + (e.message ?: "lien invalide")
                }
            }.show()
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
        if (resultCode != RESULT_OK || data == null) return
        if (requestCode == REQUEST_EXPORT_JSON) { data.data?.let { writeLibraryJson(it) }; return }
        if (requestCode == REQUEST_IMPORT_JSON) { data.data?.let { readLibraryJson(it) }; return }
        if (requestCode == REQUEST_EXPORT_AUDIO_FOLDER) { data.data?.let { exportAudioToFolder(it) }; return }
        if (requestCode == REQUEST_DOWNLOAD_FOLDER) { data.data?.let { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Exception) {}
            downloadFolderUri = uri
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_DOWNLOAD_FOLDER, uri.toString()).apply()
            savePendingDownload()
        }; return }
        if (requestCode != REQUEST_AUDIO) return
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
                filter.startsWith(CATEGORY_PREFIX) -> categories[filter.removePrefix(CATEGORY_PREFIX)]?.contains(tracks[index].toString()) == true
                else -> true
            }
            matchesFilter && names[index].lowercase().contains(searchQuery)
        }
        if (visible.isEmpty()) {
            tracksBox.addView(TextView(this).apply {
                text = when (filter) {
                    FILTER_FAVORITES -> "Aucun favori. Utilise le cœur à côté d'un titre."
                    FILTER_PLAYLIST -> "Playlist « " + activePlaylistName + " » vide. Ajoute des titres avec le bouton +."
                    filter.startsWith(CATEGORY_PREFIX) -> "Cet univers ne contient aucun morceau."
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
            line.addView(actionButton("▦") { assignTrackToCategory(index) })
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
        categories.values.forEach { it.remove(removed.toString()) }
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
        val rawCategories = prefs.getString(KEY_CATEGORIES, null)
        if (!rawCategories.isNullOrBlank()) try {
            val obj = JSONObject(rawCategories)
            obj.keys().forEach { name -> categories[name] = readStringArray(obj.optJSONArray(name)?.toString()).toCollection(linkedSetOf()) }
        } catch (_: Exception) {}
        activeCategoryName = prefs.getString(KEY_ACTIVE_CATEGORY, null).orEmpty().takeIf { categories.containsKey(it) } ?: categories.keys.firstOrNull().orEmpty()
        downloadFolderUri = prefs.getString(KEY_DOWNLOAD_FOLDER, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val valid = tracks.map { it.toString() }.toSet()
        favorites.retainAll(valid)
        playlists.values.forEach { it.retainAll(valid) }
        categories.values.forEach { it.retainAll(valid) }
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
        val savedCategories = JSONObject()
        categories.forEach { (name, values) -> savedCategories.put(name, JSONArray(values)) }
        prefs.edit().putString(KEY_URIS, encode(tracks.map { it.toString() }))
            .putString(KEY_NAMES, encode(names)).putString(KEY_FAVORITES, encode(favorites))
            .putString(KEY_PLAYLISTS, savedLists.toString()).putString(KEY_ACTIVE_PLAYLIST, activePlaylistName)
            .putString(KEY_CATEGORIES, savedCategories.toString()).putString(KEY_ACTIVE_CATEGORY, activeCategoryName).apply()
    }

    override fun onDestroy() {
        playerListener?.let { controller?.removeListener(it) }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        browserDialog?.dismiss()
        downloadExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_AUDIO = 4101
        private const val REQUEST_NOTIFICATIONS = 4102
        private const val REQUEST_EXPORT_JSON = 4103
        private const val REQUEST_IMPORT_JSON = 4104
        private const val REQUEST_EXPORT_AUDIO_FOLDER = 4105
        private const val REQUEST_DOWNLOAD_FOLDER = 4106
        private const val PREFS = "leafsound_library"
        private const val KEY_URIS = "uris"
        private const val KEY_NAMES = "names"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_ACTIVE_PLAYLIST = "active_playlist"
        private const val KEY_CATEGORIES = "music_categories"
        private const val KEY_ACTIVE_CATEGORY = "active_category"
        private const val KEY_DOWNLOAD_FOLDER = "download_folder_uri"
        private const val KEY_LAST_WEB = "last_web_url"
        private const val KEY_BOOKMARKS = "browser_bookmarks"
        private const val FILTER_ALL = "all"
        private const val FILTER_FAVORITES = "favorites"
        private const val FILTER_PLAYLIST = "playlist"
        private const val CATEGORY_PREFIX = "category:"
    }
}

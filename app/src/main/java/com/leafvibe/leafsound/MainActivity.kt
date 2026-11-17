package com.leafvibe.leafsound

import android.app.Activity
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : Activity() {
    private lateinit var player: ExoPlayer
    private lateinit var tracksBox: LinearLayout
    private lateinit var status: TextView
    private val tracks = mutableListOf<Uri>()
    private val names = mutableListOf<String>()
    private var currentIndex = -1
    private val green = Color.rgb(34, 139, 87)
    private val dark = Color.rgb(20, 28, 24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                status.text = if (isPlaying) "Lecture en cours" else "En pause"
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex
                refreshTracks()
            }
        })
        buildInterface()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 18)
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
            setPadding(0, 4, 0, 22)
        })
        root.addView(actionButton("＋  Ajouter des musiques") { chooseAudio() })
        status = TextView(this).apply {
            text = "Ajoute des fichiers audio pour commencer."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 18, 0, 12)
        }
        root.addView(status)
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        controls.addView(actionButton("⏮") { previous() }, weightParams())
        controls.addView(actionButton("▶ / ❚❚") { togglePlayback() }, weightParams())
        controls.addView(actionButton("⏭") { next() }, weightParams())
        root.addView(controls)
        root.addView(TextView(this).apply {
            text = "MA BIBLIOTHÈQUE"
            textSize = 12f
            setTextColor(Color.rgb(120, 205, 155))
            setPadding(0, 24, 0, 8)
        })
        tracksBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(tracksBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshTracks()
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, -2, 1f).apply {
        setMargins(4, 8, 4, 8)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
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
        selected.forEach { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            if (!tracks.contains(uri)) {
                tracks.add(uri)
                names.add(uri.lastPathSegment?.substringAfterLast('/') ?: "Fichier audio")
            }
        }
        refreshTracks()
        if (tracks.isNotEmpty()) status.text = tracks.size.toString() + " titre(s) ajouté(s)."
    }

    private fun refreshTracks() {
        if (!::tracksBox.isInitialized) return
        tracksBox.removeAllViews()
        if (tracks.isEmpty()) {
            tracksBox.addView(TextView(this).apply {
                text = "Aucun titre pour le moment. Utilise « Ajouter des musiques »."
                setTextColor(Color.LTGRAY)
                setPadding(0, 12, 0, 12)
            })
            return
        }
        names.forEachIndexed { index, name ->
            tracksBox.addView(TextView(this).apply {
                text = (if (index == currentIndex) "♫  " else "♪  ") + name
                textSize = 15f
                setTextColor(if (index == currentIndex) Color.rgb(120, 220, 160) else Color.WHITE)
                setPadding(12, 16, 12, 16)
                setOnClickListener { play(index) }
            })
            tracksBox.addView(View(this).apply { setBackgroundColor(Color.DKGRAY) },
                LinearLayout.LayoutParams(-1, 1))
        }
    }

    private fun play(index: Int) {
        if (index !in tracks.indices) return
        currentIndex = index
        player.setMediaItem(MediaItem.fromUri(tracks[index]))
        player.prepare()
        player.play()
        status.text = "Lecture : " + names[index]
        refreshTracks()
    }

    private fun togglePlayback() {
        when {
            tracks.isEmpty() -> status.text = "Ajoute d'abord un fichier audio."
            player.isPlaying -> player.pause()
            player.mediaItemCount > 0 -> player.play()
            else -> play(if (currentIndex >= 0) currentIndex else 0)
        }
    }

    private fun next() { if (tracks.isNotEmpty()) play((currentIndex + 1).mod(tracks.size)) }
    private fun previous() { if (tracks.isNotEmpty()) play(if (currentIndex <= 0) tracks.lastIndex else currentIndex - 1) }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    companion object { private const val REQUEST_AUDIO = 4101 }
}

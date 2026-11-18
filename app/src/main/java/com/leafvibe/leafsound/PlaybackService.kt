package com.leafvibe.leafsound

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import org.json.JSONObject
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val cycleCommand = SessionCommand(ACTION_NEXT_UNIVERSE, Bundle.EMPTY)
        val categoryButton = buildCategoryButton("Univers suivant", cycleCommand)
        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val defaults = if (controller.isTrusted) {
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                } else {
                    MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_SESSION_COMMANDS
                }
                val commands = defaults.buildUpon().add(cycleCommand).build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                    .setAvailableSessionCommands(commands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == ACTION_NEXT_UNIVERSE) {
                    playNextUniverse(player)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
        }
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .setCustomLayout(listOf(categoryButton))
            .build()
    }

    private fun playNextUniverse(player: ExoPlayer) {
        val prefs = getSharedPreferences("leafsound_library", MODE_PRIVATE)
        val categories = runCatching { JSONObject(prefs.getString("music_categories", "{}") ?: "{}") }.getOrNull()
            ?: return
        val names = mutableListOf<String>()
        val savedOrder = runCatching { org.json.JSONArray(prefs.getString("music_category_order", "[]")) }.getOrNull()
        if (savedOrder != null) for (i in 0 until savedOrder.length()) {
            val name = savedOrder.optString(i)
            if (categories.has(name)) names.add(name)
        }
        if (names.isEmpty()) {
            val iterator = categories.keys()
            while (iterator.hasNext()) names.add(iterator.next())
        }
        if (names.isEmpty()) return
        val active = prefs.getString("active_category", "").orEmpty()
        val nextName = names[(names.indexOf(active) + 1).mod(names.size)]
        val values = categories.optJSONArray(nextName) ?: return
        val uriToName = linkedMapOf<String, String>()
        val uris = prefs.getString("uris", "[]") ?: "[]"
        val titles = prefs.getString("names", "[]") ?: "[]"
        val uriArray = org.json.JSONArray(uris)
        val titleArray = org.json.JSONArray(titles)
        for (i in 0 until uriArray.length()) {
            uriToName[uriArray.optString(i)] = titleArray.optString(i, "Audio")
        }
        val items = mutableListOf<MediaItem>()
        for (i in 0 until values.length()) {
            val uri = values.optString(i)
            val title = uriToName[uri] ?: continue
            items.add(MediaItem.Builder().setUri(uri).setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist("LeafSound · Univers · $nextName").build()
            ).build())
        }
        if (items.isEmpty()) return
        prefs.edit().putString("active_category", nextName).apply()
        mediaSession?.setCustomLayout(listOf(buildCategoryButton("Suivant · $nextName", SessionCommand(ACTION_NEXT_UNIVERSE, Bundle.EMPTY))))
        player.setMediaItems(items, 0, 0L)
        player.prepare()
        player.play()
    }

    private fun buildCategoryButton(label: String, command: SessionCommand): CommandButton =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(label)
            .setCustomIconResId(R.drawable.ic_category)
            .setSessionCommand(command)
            .build()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_NEXT_UNIVERSE = "com.leafvibe.leafsound.NEXT_UNIVERSE"
    }
}

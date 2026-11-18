package com.leafvibe.leafsound

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.view.Gravity
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import org.json.JSONArray
import org.json.JSONObject

/**
 * Customizable directory of external music services.
 * Removing an entry only removes its shortcut from LeafSound; it never uninstalls an app.
 */
object LeafConnectCatalog {
    private const val PREFS = "leafsound_library"
    private const val KEY_SERVICES = "connect_services"

    private fun defaults() = JSONArray().apply {
        put(service("Pixabay Music", "Musique libre et téléchargements", "🎼", "https://pixabay.com/music/"))
        put(service("Pixabay Sound Effects", "Effets sonores et ambiances", "🔊", "https://pixabay.com/sound-effects/"))
        put(service("Shazam", "Identifier une musique", "🔎", "https://www.shazam.com/"))
        put(service("Spotify", "Catalogue, artistes et playlists", "🟢", "https://open.spotify.com/"))
        put(service("Deezer", "Streaming et découverte", "🎧", "https://www.deezer.com/"))
        put(service("SoundCloud", "Artistes et créations indépendantes", "☁️", "https://soundcloud.com/"))
        put(service("Bandcamp", "Découvrir et soutenir les artistes", "🎸", "https://bandcamp.com/"))
        put(service("Jamendo", "Musique indépendante", "🎵", "https://www.jamendo.com/"))
        put(service("Freesound", "Échantillons et bruitages", "🧩", "https://freesound.org/"))
    }

    private fun service(name: String, description: String, icon: String, url: String) =
        JSONObject().put("name", name).put("description", description)
            .put("icon", icon).put("url", url).put("favorite", false).put("hidden", false)

    fun export(activity: Activity): JSONArray = JSONArray(load(activity).toString())

    fun restore(activity: Activity, imported: JSONArray) {
        val clean = JSONArray()
        for (i in 0 until imported.length()) {
            val item = imported.optJSONObject(i) ?: continue
            val url = item.optString("url")
            if (url.startsWith("https://") && !Uri.parse(url).host.isNullOrBlank() &&
                item.optString("name").isNotBlank()) {
                clean.put(JSONObject(item.toString()))
            }
        }
        if (clean.length() > 0) save(activity, clean)
    }

    private fun load(activity: Activity): JSONArray {
        val prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SERVICES, null)
        if (raw.isNullOrBlank()) {
            val initial = defaults()
            save(activity, initial)
            return initial
        }
        return try { JSONArray(raw) } catch (_: Exception) {
            defaults().also { save(activity, it) }
        }
    }

    private fun save(activity: Activity, services: JSONArray) {
        activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit()
            .putString(KEY_SERVICES, services.toString()).apply()
    }

    fun show(activity: Activity, onOpen: (String) -> Unit) {
        val services = load(activity)
        val dialog = Dialog(activity)
        var favoritesOnly = false

        fun render() {
            val page = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 18, 18, 12)
                setBackgroundColor(LeafColour.CREAM_DARK)
            }
            page.addView(TextView(activity).apply {
                text = "LeafSound Connect"
                textSize = 25f
                setTextColor(LeafColour.ON_PRIMARY)
                setBackgroundColor(LeafColour.PRIMARY)
                setPadding(14, 14, 14, 14)
            })
            page.addView(TextView(activity).apply {
                text = "Tes services musicaux · ouvre-les sans quitter LeafSound"
                textSize = 13f
                setTextColor(LeafColour.WALNUT)
                setPadding(4, 10, 4, 8)
            })
            val actions = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(action(activity, "＋ Ajouter") {
                addService(activity) { item ->
                    services.put(item)
                    save(activity, services)
                    render()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(action(activity, if (favoritesOnly) "★ Tous" else "☆ Favoris") {
                favoritesOnly = !favoritesOnly
                render()
            }, LinearLayout.LayoutParams(0, -2, 1f))
            page.addView(actions)

            val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            val scroll = ScrollView(activity)
            scroll.addView(list)
            val visible = (0 until services.length()).mapNotNull { services.optJSONObject(it) }
                .filter { !it.optBoolean("hidden") && (!favoritesOnly || it.optBoolean("favorite")) }
                .sortedByDescending { it.optBoolean("favorite") }
            if (visible.isEmpty()) {
                list.addView(TextView(activity).apply {
                    text = if (favoritesOnly) "Aucun favori pour le moment." else "Aucun service visible. Ajoute un site avec le bouton +."
                    setTextColor(LeafColour.WALNUT)
                    setPadding(8, 24, 8, 24)
                })
            }
            visible.forEach { item ->
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val open = action(activity,
                    item.optString("icon", "🎵") + "  " + item.optString("name") + "\n" + item.optString("description")) {
                    val url = item.optString("url")
                    if (url.startsWith("https://") && !Uri.parse(url).host.isNullOrBlank()) {
                        dialog.dismiss()
                        onOpen(url)
                    }
                }
                open.textSize = 13f
                row.addView(open, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(action(activity, "⋮") { manageItem(activity, item, services, { url -> dialog.dismiss(); onOpen(url) }) { render() } })
                list.addView(row)
            }
            page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            page.addView(action(activity, "Fermer") { dialog.dismiss() })
            dialog.setContentView(page)
            dialog.window?.setLayout(-1, -1)
        }

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        render()
        dialog.show()
        dialog.window?.setLayout(-1, -1)
    }

    private fun manageItem(activity: Activity, item: JSONObject, services: JSONArray, onOpen: (String) -> Unit, changed: () -> Unit) {
        val choices = arrayOf(
            "Ouvrir dans LeafSound",
            if (item.optBoolean("favorite")) "Retirer des favoris" else "Ajouter aux favoris",
            if (item.optBoolean("hidden")) "Afficher dans le catalogue" else "Masquer du catalogue",
            "Déplacer vers le haut",
            "Déplacer vers le bas",
            "Supprimer le raccourci"
        )
        AlertDialog.Builder(activity).setTitle(item.optString("name")).setItems(choices) { _, choice ->
            when (choice) {
                0 -> {
                    val url = item.optString("url")
                    if (url.startsWith("https://") && !Uri.parse(url).host.isNullOrBlank()) onOpen(url)
                }
                1 -> item.put("favorite", !item.optBoolean("favorite"))
                2 -> item.put("hidden", !item.optBoolean("hidden"))
                3, 4 -> {
                    val index = (0 until services.length()).firstOrNull {
                        services.optJSONObject(it)?.optString("url") == item.optString("url")
                    } ?: -1
                    val target = if (choice == 3) index - 1 else index + 1
                    if (index >= 0 && target in 0 until services.length()) {
                        val reordered = JSONArray()
                        for (i in 0 until services.length()) {
                            when (i) {
                                index -> reordered.put(services.get(target))
                                target -> reordered.put(services.get(index))
                                else -> reordered.put(services.get(i))
                            }
                        }
                        while (services.length() > 0) services.remove(services.length() - 1)
                        for (i in 0 until reordered.length()) services.put(reordered.get(i))
                    }
                }
                5 -> AlertDialog.Builder(activity)
                    .setMessage("Supprimer uniquement ce raccourci de LeafSound ? L'application et le compte externes ne seront pas touchés.")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Supprimer") { _, _ ->
                        val rebuilt = JSONArray()
                        for (i in 0 until services.length()) {
                            val candidate = services.optJSONObject(i)
                            if (candidate?.optString("url") != item.optString("url")) rebuilt.put(candidate)
                        }
                        while (services.length() > 0) services.remove(services.length() - 1)
                        for (i in 0 until rebuilt.length()) services.put(rebuilt.get(i))
                        save(activity, services)
                        changed()
                    }.show()
            }
            if (choice != 5) {
                save(activity, services)
                changed()
            }
        }.setNegativeButton("Annuler", null).show()
    }

    private fun addService(activity: Activity, onAdded: (JSONObject) -> Unit) {
        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 8, 20, 0)
        }
        val name = EditText(activity).apply { hint = "Nom du service" }
        val description = EditText(activity).apply { hint = "Description / catégorie" }
        val icon = EditText(activity).apply { hint = "Emoji (facultatif)"; setText("🎵") }
        val url = EditText(activity).apply {
            hint = "Adresse HTTPS du site"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        form.addView(name)
        form.addView(description)
        form.addView(icon)
        form.addView(url)
        AlertDialog.Builder(activity).setTitle("Ajouter un service")
            .setMessage("Ajoute un site HTTPS. Les services nécessitant une application native ou une API officielle seront indiqués séparément dans une prochaine évolution.")
            .setView(form)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Ajouter") { _, _ ->
                val title = name.text.toString().trim()
                var address = url.text.toString().trim()
                if (!address.contains("://")) address = "https://$address"
                val parsed = Uri.parse(address)
                if (title.isBlank() || parsed.scheme != "https" || parsed.host.isNullOrBlank()) {
                    AlertDialog.Builder(activity).setMessage("Indique un nom et une adresse HTTPS valide.").setPositiveButton("OK", null).show()
                } else {
                    onAdded(service(title, description.text.toString().trim(), icon.text.toString().trim().ifBlank { "🎵" }, address))
                }
            }.show()
    }

    private fun action(activity: Activity, label: String, click: () -> Unit): Button =
        Button(activity).apply {
            text = label
            isAllCaps = false
            setTextColor(LeafColour.ON_PRIMARY)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(LeafColour.PRIMARY))
            setOnClickListener { click() }
        }
}

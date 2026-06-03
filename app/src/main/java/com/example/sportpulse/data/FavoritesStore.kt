package com.example.sportpulse.data

import android.content.Context
import com.example.sportpulse.domain.FavoriteTeam
import org.json.JSONArray
import org.json.JSONObject

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("sport_pulse_favorites", Context.MODE_PRIVATE)

    fun load(): List<FavoriteTeam> {
        val raw = prefs.getString(KEY_TEAMS, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                add(
                    FavoriteTeam(
                        name = name,
                        sport = item.optString("sport"),
                        badgeUrl = item.optString("badgeUrl").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    fun save(teams: List<FavoriteTeam>) {
        val array = JSONArray()
        teams.distinctBy { it.name }.forEach { team ->
            array.put(
                JSONObject()
                    .put("name", team.name)
                    .put("sport", team.sport)
                    .put("badgeUrl", team.badgeUrl.orEmpty()),
            )
        }
        prefs.edit().putString(KEY_TEAMS, array.toString()).apply()
    }

    private companion object {
        const val KEY_TEAMS = "teams"
    }
}

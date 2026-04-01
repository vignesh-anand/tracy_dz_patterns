package com.tracydz.patterns.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tracydz.patterns.model.Canopy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "canopy_prefs")

class CanopyStore(private val context: Context) {

    private val gson = Gson()
    private val canopiesKey = stringPreferencesKey("canopies")
    private val activeIdKey = stringPreferencesKey("active_canopy_id")

    val canopies: Flow<List<Canopy>> = context.dataStore.data.map { prefs ->
        val json = prefs[canopiesKey] ?: "[]"
        val type = object : TypeToken<List<Canopy>>() {}.type
        gson.fromJson(json, type)
    }

    val activeCanopyId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[activeIdKey]
    }

    suspend fun saveCanopy(canopy: Canopy) {
        context.dataStore.edit { prefs ->
            val json = prefs[canopiesKey] ?: "[]"
            val type = object : TypeToken<MutableList<Canopy>>() {}.type
            val list: MutableList<Canopy> = gson.fromJson(json, type)
            val index = list.indexOfFirst { it.id == canopy.id }
            if (index >= 0) list[index] = canopy else list.add(canopy)
            prefs[canopiesKey] = gson.toJson(list)
            if (prefs[activeIdKey] == null) prefs[activeIdKey] = canopy.id
        }
    }

    suspend fun deleteCanopy(id: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[canopiesKey] ?: "[]"
            val type = object : TypeToken<MutableList<Canopy>>() {}.type
            val list: MutableList<Canopy> = gson.fromJson(json, type)
            list.removeAll { it.id == id }
            prefs[canopiesKey] = gson.toJson(list)
            if (prefs[activeIdKey] == id) {
                prefs[activeIdKey] = list.firstOrNull()?.id ?: ""
            }
        }
    }

    suspend fun setActiveCanopy(id: String) {
        context.dataStore.edit { prefs ->
            prefs[activeIdKey] = id
        }
    }
}

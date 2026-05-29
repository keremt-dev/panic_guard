package com.intellica.panicshield.block

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.protectedStore by preferencesDataStore(name = "protected_apps")

class ProtectedAppsRepository(private val context: Context) {

    val protectedPackages: Flow<Set<String>> = context.protectedStore.data.map {
        it[PROTECTED] ?: emptySet()
    }

    /**
     * On first run, seed the protected set with the curated bank list ∩
     * actually-installed packages, so the feature works out of the box
     * without the user hand-picking. Idempotent: only seeds once.
     */
    suspend fun seedDefaultsIfFirstRun() {
        val seeded = context.protectedStore.data.first()[SEEDED] ?: false
        if (seeded) return
        val installed = installedPackageNames()
        val defaults = KnownBanks.DEFAULT_TR.intersect(installed)
        context.protectedStore.edit {
            it[PROTECTED] = defaults
            it[SEEDED] = true
        }
    }

    suspend fun setProtected(packageName: String, isProtected: Boolean) {
        context.protectedStore.edit { prefs ->
            val current = prefs[PROTECTED]?.toMutableSet() ?: mutableSetOf()
            if (isProtected) current.add(packageName) else current.remove(packageName)
            prefs[PROTECTED] = current
        }
    }

    private fun installedPackageNames(): Set<String> =
        context.packageManager
            .getInstalledPackages(0)
            .map { it.packageName }
            .toSet()

    private companion object {
        val PROTECTED = stringSetPreferencesKey("protected_packages")
        val SEEDED = booleanPreferencesKey("seeded")
    }
}

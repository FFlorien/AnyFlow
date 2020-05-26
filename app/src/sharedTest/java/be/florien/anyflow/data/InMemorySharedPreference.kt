package be.florien.anyflow.data

import android.content.SharedPreferences

class InMemorySharedPreference : SharedPreferences {
    private var data = HashMap<String, Any?>()
    private val changeListeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun getBoolean(key: String?, defValue: Boolean) = data[key] as? Boolean ?: defValue

    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue

    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String> = data[key] as? MutableSet<String>
            ?: defValues ?: emptySet<String>().toMutableSet()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()

    override fun edit(): SharedPreferences.Editor = InMemoryEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        changeListeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        changeListeners.remove(listener)
    }

    private class InMemoryEditor(private val preferences: InMemorySharedPreference) : SharedPreferences.Editor {
        private val temporaryData: HashMap<String, Any?> = preferences.data.clone() as HashMap<String, Any?>
        private val changedKeys = mutableListOf<String>()

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            changedKeys.add(key)
            temporaryData[key] = value
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            changedKeys.add(key)
            temporaryData[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            changedKeys.add(key)
            temporaryData[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            changedKeys.add(key)
            temporaryData[key] = value
            return this

        }

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            changedKeys.add(key)
            temporaryData[key] = value ?: ""
            return this
        }

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
            changedKeys.add(key)
            temporaryData[key] = values ?: emptySet<String>()
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            temporaryData.remove(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            temporaryData.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            preferences.data = temporaryData
            changedKeys.forEach { key ->
                preferences.changeListeners.forEach {
                    it.onSharedPreferenceChanged(preferences, key)
                }
            }
        }
    }
}
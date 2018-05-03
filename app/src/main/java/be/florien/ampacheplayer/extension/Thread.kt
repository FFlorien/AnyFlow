package be.florien.ampacheplayer.extension

import io.realm.Realm

internal val realmInstances = mutableMapOf<Thread, Realm>()

val Thread.realmInstance: Realm
    get() {
        val instance = realmInstances[this] ?: Realm.getDefaultInstance()
        if (realmInstances[this] == null) {
            realmInstances[this] = instance
        }
        for (thread in realmInstances.keys) {
            if (!thread.isAlive) {
                realmInstances.remove(thread)
            }
        }
        return instance
    }
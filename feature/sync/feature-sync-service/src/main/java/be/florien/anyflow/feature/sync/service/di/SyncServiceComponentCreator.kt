package be.florien.anyflow.feature.sync.service.di

interface SyncServiceComponentCreator {

    fun createSyncServiceComponent() : SyncServiceComponent?
}
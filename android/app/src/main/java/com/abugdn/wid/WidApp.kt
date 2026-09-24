package com.abugdn.wid

import android.app.Application
import com.abugdn.wid.data.Repository
import com.abugdn.wid.sync.DigestWorker
import com.abugdn.wid.sync.Notifier
import com.abugdn.wid.sync.SyncWorker

class WidApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
        Notifier.createChannels(this)
        SyncWorker.schedule(this)
        DigestWorker.schedule(this)
    }
}

val android.content.Context.repository: Repository
    get() = (applicationContext as WidApp).repository

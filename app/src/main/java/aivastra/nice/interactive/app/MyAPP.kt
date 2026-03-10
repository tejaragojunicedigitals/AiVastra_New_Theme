package aivastra.nice.interactive.app

import aivastra.nice.interactive.customview.TryOnProcessingVideoDownloader
import aivastra.nice.interactive.utils.ViewControll
import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import coil.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyAPP : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        appContext = applicationContext
        ViewControll.clearAppCache(this)
        // Start download in background (non-blocking)
        CoroutineScope(Dispatchers.IO).launch {
            TryOnProcessingVideoDownloader.ensureVideoDownloaded(
                applicationContext,
                "https://api.aivastra.com/aivastravideo.mp4"
            )
        }
    }

    public override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    companion object {
        var appContext: Context? = null
    }

}

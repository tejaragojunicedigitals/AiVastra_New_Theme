package aivastra.nice.interactive.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object InternetSpeedChecker {

    suspend fun getInternetSpeedMbps(): Double = withContext(Dispatchers.IO) {

        try {

            val url = URL("https://speed.hetzner.de/1MB.bin")

            val start = System.nanoTime()

            val connection = url.openConnection()
            val input = connection.getInputStream()

            val buffer = ByteArray(8192)
            var totalBytes = 0
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
            }

            input.close()

            val end = System.nanoTime()

            val timeSeconds = (end - start) / 1_000_000_000.0

            val speedBps = totalBytes.toDouble() / timeSeconds
            val speedMbps = speedBps / (1024 * 1024)

            speedMbps

        } catch (e: Exception) {
            Log.e("InternetSpeedChecker", "Speed check failed", e)
            0.0
        }
    }
}
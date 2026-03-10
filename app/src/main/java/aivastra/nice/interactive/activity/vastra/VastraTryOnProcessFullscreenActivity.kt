package aivastra.nice.interactive.activity.vastra

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import aivastra.nice.interactive.R
import aivastra.nice.interactive.customview.VideoDialogController
import aivastra.nice.interactive.databinding.ActivityVastraTryOnProcessFullscreenBinding
import aivastra.nice.interactive.databinding.DialogVastraTryonProcessingNewBinding
import aivastra.nice.interactive.viewmodel.category.SareecategoryDataViewModel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch

class VastraTryOnProcessFullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVastraTryOnProcessFullscreenBinding

    private var exoPlayer: ExoPlayer? = null
    private var handler = Handler(Looper.getMainLooper())
    private var textRunnable: Runnable? = null
    private var lottieDrawable: LottieDrawable? = null
    private var isFinishingSafe = false
    private lateinit var sareeCatViewmodel: SareecategoryDataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVastraTryOnProcessFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        sareeCatViewmodel = ViewModelProvider(this).get(SareecategoryDataViewModel::class.java)
        playVideo()
        startTextProcessAnimation()
        startLoaderAnimation()
        sareeCatViewmodel.closeDialogCallback.observe(this) {isDialogClose->
            if(isDialogClose){
                finishSafely()
            }
        }
    }

    // 🔥 KIOSK SAFE PLAYER
    @OptIn(UnstableApi::class)
    private fun playVideo() {
        releasePlayer()
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.apply {
            setKeepContentOnPlayerReset(true)
            player = exoPlayer
        }

        val mediaItem = MediaItem.fromUri("https://api.aivastra.com/aivastravideo.mp4")
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL

        handler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
        }, 300)
    }

    private fun finishSafely() {
        if (isFinishingSafe) return
        isFinishingSafe = true
        textRunnable?.let { handler.removeCallbacks(it) }
        stopLottieAnimation()
        releasePlayer()
        sareeCatViewmodel.resetDialog()
        finish()
    }

    private fun releasePlayer() {
        try {
            binding.playerView.player = null
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        exoPlayer = null
    }

    private fun startLoaderAnimation() {
        lottieDrawable = LottieDrawable().apply {
            composition = LottieCompositionFactory
                .fromRawResSync(this@VastraTryOnProcessFullscreenActivity, R.raw.star_loader_animation)
                .value
            repeatMode = LottieDrawable.REVERSE
            repeatCount = LottieDrawable.INFINITE
        }
        binding.lottieView.setImageDrawable(lottieDrawable)
        lottieDrawable?.playAnimation()
    }

    private fun stopLottieAnimation() {
        lottieDrawable?.cancelAnimation()
        lottieDrawable?.clearComposition()
        lottieDrawable = null
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        textRunnable?.let { handler.removeCallbacks(it) }
        releasePlayer()
        stopLottieAnimation()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // disable back in kiosk
    }

    private fun startTextProcessAnimation() {
        val messages = listOf(
            "🧵 Setting up your style…",
            "💃 Matching perfect drape to your body shape…",
            "🎨 Adding finishing details…",
            "👗 Styling you virtually…",
            "💫 Almost done! Your look is ready…",
            "💫 Almost done! Your look is ready…",
            "💫 Almost done! Your look is ready…",
            "💫 Almost done! Your look is ready…",
            "💫 Almost done! Your look is ready…"
        )

        var index = 0
        textRunnable = object : Runnable {
            override fun run() {
                val tv = binding.loadingText
                tv.text = messages[index]

                tv.post {
                    val width = tv.paint.measureText(tv.text.toString())
                    tv.paint.shader = LinearGradient(
                        0f, 0f, width, tv.textSize,
                        intArrayOf(
                            getColor(R.color.gradiant1),
                            getColor(R.color.gradiant2),
                            getColor(R.color.gradiant3)
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    tv.invalidate()
                }

                tv.alpha = 0f
                tv.animate().alpha(1f).setDuration(1200).start()

                index = (index + 1) % messages.size
                handler.postDelayed(this, 4500)
            }
        }
        handler.post(textRunnable!!)
    }
}

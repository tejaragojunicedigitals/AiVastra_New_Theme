package aivastra.nice.interactive.dialog

import aivastra.nice.interactive.R
import aivastra.nice.interactive.databinding.DialogAppAlertBinding
import aivastra.nice.interactive.databinding.DialogVastraTryonProcessingNewBinding
import aivastra.nice.interactive.utils.ViewControll
import android.animation.Animator
import android.app.Dialog
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class VastraTryOnProcessingDialog() : BottomSheetDialogFragment() {

    private lateinit var binding: DialogVastraTryonProcessingNewBinding
    private var exoPlayer: ExoPlayer? = null
    private var handler = Handler(Looper.getMainLooper())
    private var textRunnable: Runnable? = null
    private var  lottieDrawable: LottieDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogVastraTryonProcessingNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {

            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    // ❌ Ignore back press
                    true
                } else {
                    false
                }
            }

            setOnShowListener { dialogInterface ->
                val bottomSheet = (dialogInterface as BottomSheetDialog)
                    .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

                bottomSheet?.background = ColorDrawable(Color.TRANSPARENT) // Force transparency
                if (bottomSheet != null) {
                    // Set the background to transparent
                    bottomSheet.setBackgroundColor(Color.TRANSPARENT)

                    // Force full height
                    val behavior = BottomSheetBehavior.from(bottomSheet)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    behavior.isDraggable = false
                    behavior.skipCollapsed = true
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Make the dialog full-screen immediately
        dialog?.let { dlg ->
            dlg.setCancelable(true)  // Prevents dismiss on back press
            dlg.setCanceledOnTouchOutside(false)  // Prevents dismiss on outside touch

            val bottomSheet =
                dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
            initView()
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogThemeFullWidth

    private fun initView() {
        playVideo()
        startTextProcessAnimation()
        startLoaderAnimation()
    }

    fun startLoaderAnimation(){
        lottieDrawable = LottieDrawable()
        val composition = LottieCompositionFactory.fromRawResSync(
            requireActivity(),
            R.raw.star_loader_animation
        ).value

        lottieDrawable?.composition = composition
        lottieDrawable?.repeatMode = LottieDrawable.REVERSE
        lottieDrawable?.repeatCount = LottieDrawable.INFINITE
        binding.lottieView.setImageDrawable(lottieDrawable)
        lottieDrawable?.playAnimation()
    }

    @OptIn(UnstableApi::class)
    private fun playVideo(){
        releasePlayer() // Ensure no leaks

        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = exoPlayer
        binding.playerView.setKeepContentOnPlayerReset(true)

        val mediaItem = MediaItem.fromUri("https://api.aivastra.com/aivastravideo.mp4")
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        exoPlayer?.prepare()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    binding.playerView.background = null
                    binding.playerView.isVisible = true
                    exoPlayer?.playWhenReady = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
            }
        })
    }

    private fun stopLottieAnimation(){
        lottieDrawable?.cancelAnimation()
        lottieDrawable?.clearComposition()
        lottieDrawable = null
    }

    private fun releasePlayer() {
        exoPlayer?.run {
            stop()
            release()
        }
        exoPlayer = null
    }

  /*  fun dismissDialog() {
        exoPlayer?.pause()
        stopLottieAnimation()
        dismissAllowingStateLoss()
    }*/

    fun dismissDialogSafe() {
        if (!isAdded || activity == null) return

        requireActivity().runOnUiThread {
            try {
                exoPlayer?.pause()
                stopLottieAnimation()

                val bottomSheet =
                    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

                bottomSheet?.let {
                    BottomSheetBehavior.from(it).state =
                        BottomSheetBehavior.STATE_HIDDEN
                }

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLottieAnimation()
        textRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }


    private fun startTextProcessAnimation(){
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

        handler = Handler(Looper.getMainLooper())
        var index = 0

        textRunnable = object : Runnable {
            override fun run() {
                val textView = binding.loadingText
                textView.text = messages[index]

                // Gradient each time to reset width
                textView.post {
                    val paint = textView.paint
                    val width = paint.measureText(textView.text.toString())
                    val shader = LinearGradient(
                        0f, 0f, width, textView.textSize,
                        intArrayOf(requireActivity().getColor(R.color.gradiant1),
                            requireActivity().getColor(R.color.gradiant2),
                            requireActivity().getColor(R.color.gradiant3)
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    textView.paint.shader = shader
                    textView.invalidate()
                }

                // Simple fade animation
                textView.alpha = 0f
                textView.animate().alpha(1f).setDuration(2000).start()

                index = (index + 1) % messages.size
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(textRunnable!!)
    }
}
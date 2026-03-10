package aivastra.nice.interactive.activity.launch

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import aivastra.nice.interactive.R
import aivastra.nice.interactive.activity.launch.screendemo.ImagePagerAdapter
import aivastra.nice.interactive.databinding.ActivityAivastraScreenDemoBinding
import android.graphics.Point
import android.util.Log
import android.view.Display
import androidx.viewpager2.widget.ViewPager2

class AivastraScreenDemoActivity : BaseActivity() {

    private lateinit var binding : ActivityAivastraScreenDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAivastraScreenDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        val imageRes = listOf(
            R.drawable.screen1,
            R.drawable.screen2,
            R.drawable.screen3,
            R.drawable.screen4,
            R.drawable.screen5,
            R.drawable.screen6,
            R.drawable.screen7,
            R.drawable.screen8
        )

        val display: Display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size) // gets full screen size including nav & status bars
        val screenWidth = size.x
        val screenHeight = size.y
        Log.e("screenratio:=" , "width:$screenWidth height: $screenHeight")
        binding.imageViewPager.adapter = ImagePagerAdapter(screenWidth,screenHeight,imageRes)
        binding.imageViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.imageViewPager.offscreenPageLimit = 3
    }
}
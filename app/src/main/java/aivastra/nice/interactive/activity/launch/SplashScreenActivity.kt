package aivastra.nice.interactive.activity.launch

import aivastra.nice.interactive.Loader.LoaderManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import aivastra.nice.interactive.R
import aivastra.nice.interactive.activity.Home.HomeDressesForActivity
import aivastra.nice.interactive.activity.auth.LoginActivity
import aivastra.nice.interactive.activity.profile.PofileActivity
import aivastra.nice.interactive.databinding.ActivitySplashScreenBinding
import aivastra.nice.interactive.utils.PrefsManager
import aivastra.nice.interactive.utils.ViewControll
import aivastra.nice.interactive.viewmodel.category.SareecategoryDataViewModel
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.facewixlatest.ApiUtils.APIConstant

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var  sareeCatViewmodel : SareecategoryDataViewModel
    private var isAnimComplete:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sareeCatViewmodel = ViewModelProvider(this).get(SareecategoryDataViewModel::class.java)
        binding.btnTryNow.setOnClickListener {
            if(PrefsManager.isUserExist){
                val intent = Intent(this@SplashScreenActivity, HomeDressesForActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.fade_and_scale_in, R.anim.fade_and_scale_out)
            }else{
                val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.fade_and_scale_in, R.anim.fade_and_scale_out)
//                callAppLoginAPI()
            }
        }
        binding.imgProfile.setOnClickListener {
            val intent = Intent(this@SplashScreenActivity, PofileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.fade_and_scale_in, R.anim.fade_and_scale_out)
        }
    }

    override fun onResume() {
        super.onResume()
        if(PrefsManager.isUserExist){
            ViewControll.setCompanyLogo(this,binding.appLogo)
            setUserProfilePhoto()
            binding.imgProfile.isVisible = true
        }
    }

    private fun callAppLoginAPI(){
        val deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        val userName = "paritha"
        val password = "paritha@123"
        LoaderManager.show(this,findViewById(android.R.id.content),false)
        sareeCatViewmodel.userAppLoginAPI(userName,password,deviceId)
        sareeCatViewmodel.userLoginInfo.observe(this, Observer {userLoginDataModel->
            LoaderManager.remove(this)
            if(userLoginDataModel!=null){
//                ViewControll.showMessage(this,userLoginDataModel.message)
                PrefsManager.saveLoginUserData(userLoginDataModel)
                val intent = Intent(this, HomeDressesForActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.fade_and_scale_in, R.anim.fade_and_scale_out)
            }
        })
        sareeCatViewmodel.error.observe(this, Observer { errorMsg->
            LoaderManager.remove(this)
            if(errorMsg!=null){
                sareeCatViewmodel.resetAppLoginData()
                ViewControll.showMessage(this,errorMsg)
                resetObserver()
            }
        })
    }

    private fun resetObserver(){
        sareeCatViewmodel.error.removeObservers(this)
        sareeCatViewmodel.userLoginInfo.removeObservers(this)
    }

    private fun setUserProfilePhoto(){
        if(PrefsManager.isUserExist){
            val userData = PrefsManager.loginUserInfo
            if(userData.user.merchantPhoto.isNotEmpty()){
                try{
                    Glide.with(this@SplashScreenActivity)
                        .load(APIConstant.BASE_URL + userData.user.merchantPhoto)
                        .placeholder(R.drawable.icon_profile)
                        .error(R.drawable.icon_profile)
                        .into(binding.imgProfile)
                }catch(e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }
}
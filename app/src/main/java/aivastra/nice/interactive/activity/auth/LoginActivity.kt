package aivastra.nice.interactive.activity.auth

import aivastra.nice.interactive.Loader.LoaderManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import aivastra.nice.interactive.R
import aivastra.nice.interactive.activity.Home.HomeDressesForActivity
import aivastra.nice.interactive.activity.launch.BaseActivity
import aivastra.nice.interactive.activity.vastra.VastraSubCategoryActivity
import aivastra.nice.interactive.databinding.ActivityLoginBinding
import aivastra.nice.interactive.utils.AppConstant
import aivastra.nice.interactive.utils.PrefsManager
import aivastra.nice.interactive.utils.ViewControll
import aivastra.nice.interactive.viewmodel.category.SareecategoryDataViewModel
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class LoginActivity : BaseActivity() {

    private lateinit var binding:ActivityLoginBinding
    private lateinit var sareeCatViewmodel: SareecategoryDataViewModel
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        sareeCatViewmodel = ViewModelProvider(this).get(SareecategoryDataViewModel::class.java)
        binding.btnLogin.setOnClickListener{
            if(checkValidation()){
                callAppLoginAPI()
            }
        }
        binding.etPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = 2
                val drawable = binding.etPassword.compoundDrawables[drawableEnd]

                if (drawable != null &&
                    event.rawX >= (binding.etPassword.right - drawable.bounds.width() - binding.etPassword.paddingEnd)
                ) {

                    togglePasswordVisibility(binding.etPassword)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText) {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_eye_on, 0
            )
        } else {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_eye_off, 0
            )
        }

        // Keep cursor at end
        editText.setSelection(editText.text.length)
    }

    private fun checkValidation(): Boolean {
        if(binding.etUsername.text.trim().isEmpty()){
            ViewControll.showMessage(this,"Please enter username")
            return false
        }else if(binding.etPassword.text.trim().isEmpty()){
            ViewControll.showMessage(this,"Please enter password")
            return false
        }else{
            return true
        }
    }

    private fun callAppLoginAPI(){
        val deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        val userName = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        LoaderManager.show(this,findViewById(android.R.id.content),false)
        sareeCatViewmodel.userAppLoginAPI(userName,password,deviceId)
        sareeCatViewmodel.userLoginInfo.observe(this, Observer { userLoginDataModel->
            LoaderManager.remove(this)
            if(userLoginDataModel!=null){
                ViewControll.showMessage(this,userLoginDataModel.message)
                PrefsManager.saveLoginUserData(userLoginDataModel)
                val intent = Intent(this, HomeDressesForActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                overridePendingTransition(R.anim.fade_and_scale_in, R.anim.fade_and_scale_out)
            }
        })
        sareeCatViewmodel.error.observe(this, Observer { errorMsg->
            LoaderManager.remove(this)
            if(errorMsg!=null){
                sareeCatViewmodel.resetAppLoginData()
                ViewControll.showSnackErrorMsg(this,errorMsg)
                resetObserver()
            }
        })
    }

    private fun resetObserver(){
        sareeCatViewmodel.error.removeObservers(this)
        sareeCatViewmodel.userLoginInfo.removeObservers(this)
    }

}
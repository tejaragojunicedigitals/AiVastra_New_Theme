package aivastra.nice.interactive.dialog

import aivastra.nice.interactive.R
import aivastra.nice.interactive.activity.Home.HomeDressesForActivity
import aivastra.nice.interactive.activity.Home.VastraFor.VastraForDataAdapter
import aivastra.nice.interactive.databinding.DialogSelectVastraGenderBinding
import aivastra.nice.interactive.viewmodel.Dress.DressesForDataModel
import aivastra.nice.interactive.viewmodel.category.SareecategoryDataViewModel
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectVastraGenderDialog(private val selectvastraForItem:(DressesForDataModel.Data)->Unit) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogSelectVastraGenderBinding
    private var vastraForAdapter: VastraForDataAdapter?= null
    private lateinit var sareeCatViewmodel: SareecategoryDataViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSelectVastraGenderBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
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
                    behavior.skipCollapsed = true
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Make the dialog full-screen immediately
        dialog?.let { dlg ->
            dlg.setCancelable(false)  // Prevents dismiss on back press
            dlg.setCanceledOnTouchOutside(false)  // Prevents dismiss on outside touch

            val bottomSheet =
                dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogThemeFullWidth


    private fun initView() {
        sareeCatViewmodel = ViewModelProvider(this).get(SareecategoryDataViewModel::class.java)
        setVastraForGenderDataList()
        binding.btnCancle.setOnClickListener{
            dismiss()
        }
    }

    private fun setVastraForGenderDataList(){
        sareeCatViewmodel.getDressesForList()
        sareeCatViewmodel.dressesForData.observe(this, Observer { dressesForList ->
            vastraForAdapter = VastraForDataAdapter(requireActivity(),dressesForList){dressesForItem->
                selectvastraForItem(dressesForItem)
            }
            binding.recyclerVastraForlist.adapter = vastraForAdapter
          /*  // Force RecyclerView to layout first
            binding.recyclerVastraForlist.post {
                binding.recyclerVastraForlist.requestLayout()
                binding.recyclerVastraForlist.invalidate()

                // Delay execution slightly to ensure measurement is complete
                binding.recyclerVastraForlist.postDelayed({
                    val height = binding.recyclerVastraForlist.height
                    if (height > 0) {
                        vastraForAdapter?.setRecyclerViewHeight(height)
                        binding.recyclerVastraForlist.adapter = vastraForAdapter
                    }
                }, 100) // Small delay
            }*/
        })
        // Observe error LiveData
        sareeCatViewmodel.error.observe(this, Observer { error ->
            Log.e("SareeStudio", requireActivity().localClassName + " Error:=" + error)
        })
    }

}
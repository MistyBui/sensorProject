package com.metropolia.sensorproject

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alert_dialog.view.*
import kotlinx.android.synthetic.main.fragment_progress.view.*


class ProgressFragment : Fragment() {
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout and set element for this fragment
        val rootView = inflater.inflate(R.layout.fragment_progress, container, false)
        //val floatingBtn: FloatingActionButton = rootView!!.findViewById(R.id.floating_action_button)

        // retrieve data
        preferences = getActivity()!!.getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        val goal = preferences.getInt("GOAL", 0)
        val name = preferences.getString("NAME", "")
        val height = preferences.getInt("HEIGHT", 0)
        val weight = preferences.getInt("WEIGHT", 0)

        rootView.floating_action_button.setOnClickListener{
            //Inflate the dialog and element with custom view
            val mDialogView = LayoutInflater.from(activity).inflate(R.layout.alert_dialog, null)

            //set input for each text field
            mDialogView.editTxtName.setText(name)
            mDialogView.editTxtWeight.setText(weight.toString())
            mDialogView.editTxtHeight.setText(height.toString())
            mDialogView.editTxtGoal.setText(goal.toString())

            //AlertDialogBuilder
            val mBuilder = AlertDialog.Builder(activity)
                .setView(mDialogView)
            //show dialog
            val  mAlertDialog = mBuilder.show()
            mDialogView.btnSave.setOnClickListener{

                val newName: String = mDialogView.editTxtName.text.toString()
                val newWeight: Int = mDialogView.editTxtWeight.text.toString().toInt()
                val newHeight: Int = mDialogView.editTxtHeight.text.toString().toInt()
                val newGoal: Int = mDialogView.editTxtGoal.text.toString().toInt()
                //validate
                /*if(newName.length >20 ){
                    layoutName.error = getString(R.string.name_input_error)
                } else if (newWeight.toString().length > 3 ){
                    layoutWeight.error = getString(R.string.input_error)
                }else if (newHeight.toString().length > 3) {
                    layoutHeight.error = getString(R.string.input_error)
                }else if (editName.text.toString().isEmpty()) {
                    layoutName.error = getString(R.string.input_empty)
                }else if (editWeight.text.toString().isEmpty()) {
                    layoutWeight.error = getString(R.string.input_empty)
                }else if (editHeight.text.toString().isEmpty()) {
                    layoutHeight.error = getString(R.string.input_empty)
                } else if (editGoal.text.toString().isEmpty()) {
                    layoutGoal.error = getString(R.string.input_empty)
                } else {*/
                    //save data
                    preferences =
                        getActivity()!!.getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor = preferences.edit()
                    editor.putString("NAME", newName)
                    editor.putInt("WEIGHT", newWeight)
                    editor.putInt("HEIGHT", newHeight)
                    editor.putInt("GOAL", newGoal)
                    editor.commit()

                    val intent = Intent(activity, StepTrackerActivity::class.java)
                    intent.putExtra("TabNumber", "1");
                    startActivity(intent)

            }
            mDialogView.btnCancel.setOnClickListener{
                mAlertDialog.dismiss()
            }
        }
        return rootView
    }
}
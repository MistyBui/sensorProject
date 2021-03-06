package com.metropolia.sensorproject

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.metropolia.sensorproject.database.AppDB
import com.metropolia.sensorproject.database.DayActivity
import com.metropolia.sensorproject.services.LocationService
import com.metropolia.sensorproject.utils.compareDate
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers.io
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.gif
import org.osmdroid.config.Configuration
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

/**
 *  - Checks required permissions
 *  - Compares current date to the date of the last entry in roomSQL (ignores < days)
 *  - If date is different reads the content of an internal storage file.
 *      - writes the values of the file to database
 *      - Resets the values to 0
 *  - Checks prefs if user is already created.
 *  - If no user prompts with user creation form with validation
 *  - Lunches the StepTrackerActivity
 * */
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var sharedPreferences: SharedPreferences
    private val checkedPermissionSubject: PublishSubject<Unit> = PublishSubject.create()
    private val appReadySubject: PublishSubject<DayActivity> = PublishSubject.create()
    private val unsubscribeOnDestroy = CompositeDisposable()
    private val db by lazy { AppDB.get(application) }
    private lateinit var locationService: LocationService
    private val gson = Gson()
    private var isLogedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = applicationContext
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(ctx))
        setContentView(R.layout.activity_main)

        locationService = LocationService(this)
        sharedPreferences= getSharedPreferences("SHARED_PREF", Context.MODE_PRIVATE)
        isLogedIn = sharedPreferences.getBoolean("CHECKBOX", false)

        loadGif()
        inputCheck()
        btnSave.setOnClickListener(this)
        setSubscriptions()
        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        unsubscribeOnDestroy.clear()
    }


    /**
     *  Sets up rx subscriptions
     *  streams disposed on destroy
     * */
    private fun setSubscriptions() {
        checkedPermissionSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                checkDateAndStart()
            }.addTo(unsubscribeOnDestroy)

        appReadySubject
            .delay(1, TimeUnit.SECONDS) // Delay just for to see more of the cool dog animation
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                StepApp.setValues(it)
                locationService.getLocation()
                if(isLogedIn){
                    val intent = Intent(this, StepTrackerActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    initial.visibility = View.GONE
                }
            }.addTo(unsubscribeOnDestroy)
    }

    /**
     *  Loads cure loading dog gif
     * */
    private fun loadGif() {
        Glide
            .with(this)
            .load(R.drawable.giphy)
            .into(gif)
    }

    //validation for user form
    private fun validate(): Boolean {
        if(editTxtName.text.toString().isEmpty()){
            layoutEditName.error = getString(R.string.input_empty)
            return false
        } else if (editTxtWeight.text.toString().isEmpty()){
            layoutEditWeight.error = getString(R.string.input_empty)
            return false
        }else if (editTxtHeight.text.toString().isEmpty()) {
            layoutEditHeight.error = getString(R.string.input_empty)
            return false
        }else if (editTxtGoal.text.toString().isEmpty()) {
            layoutEditGoal.error = getString(R.string.input_empty)
            return false
        }else if (editTxtName.text.toString().length>20) {
            layoutEditName.error = getString(R.string.name_input_error)
            return false
        }
        return true
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.btnSave -> {
                if (validate()) {
                    saveData()
                }
            }
        }
    }

    //check input error when typing
    private fun inputCheck() {
        editTxtName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > layoutEditName.counterMaxLength) {
                    Log.d("main", "$count")
                    layoutEditName.error = getString(R.string.name_input_error)
                } else {
                    layoutEditName.error = null
                }
            }
        })
        editTxtHeight.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 0) {
                    Log.d("main", "$count")
                    layoutEditHeight.error = getString(R.string.input_empty)
                } else {
                    layoutEditHeight.error = null
                }
            }
        })
        editTxtWeight.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 0) {
                    Log.d("main", "$count")
                    layoutEditWeight.error = getString(R.string.input_empty)
                } else {
                    layoutEditWeight.error = null
                }
            }
        })
        editTxtGoal.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 0) {
                    Log.d("main", "$count")
                    layoutEditGoal.error = getString(R.string.input_empty)
                } else {
                    layoutEditGoal.error = null
                }
            }
        })
    }

    //save data into shared preference
    private fun saveData() {
        val name: String = editTxtName.text.toString()
        val weight: Int = editTxtWeight.text.toString().toInt()
        val height: Int = editTxtHeight.text.toString().toInt()
        val goal: Int = editTxtGoal.text.toString().toInt()
        //save data
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("NAME", name)
        editor.putInt("WEIGHT", weight)
        editor.putInt("HEIGHT", height)
        editor.putInt("GOAL", goal)
        editor.putBoolean("CHECKBOX", true)
        editor.apply()

        val intent = Intent(this, StepTrackerActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     *  Checks permissions and if not granted presents a dialog asking for permissions.
     * */
    private fun checkPermissions(){
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_ALL_NEEDED_PERMISSIONS)
        } else {
            checkedPermissionSubject.onNext(Unit)
        }
    }

    /**
     *  Checks the result of permission request.
     *  If result contains denied alert dialog with explanation will be presented why the user should give permissions.
     * */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_ALL_NEEDED_PERMISSIONS) {
            if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                showPermissionsAlert()
            } else {
                checkedPermissionSubject.onNext(Unit)
            }
        }
    }

    /**
     *  Alert dialog with explanation why permissions are needed.
     * */
    private fun showPermissionsAlert() {
        AlertDialog
            .Builder(this)
            .apply {
                setTitle("Missing permissions")
                setMessage("Permissions are needed to use this app")
                setPositiveButton("I understand") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        PERMISSIONS,
                        REQUEST_ALL_NEEDED_PERMISSIONS
                    )
                }
            }
            .create()
            .show()
    }

    /**
     *  Tries to read values from file. Converts the json string from file to object.
     *  If failed return initial values.
     *  @return DayActivity
     * */
    private fun readValuesFromFile(): DayActivity {
        return try {
            val reader = openFileInput(DAY_VALUES_FILE)?.bufferedReader().use { it?.readText() }
            gson.fromJson(reader, DayActivity::class.java)
        } catch (e: Exception) {
            DayActivity(Date(), 0, 0, null, null, 0f)
        }
    }

    /**
     *  Compares current date to last entry date in database.
     *  if date is different writes the values from file to database and resets the file.
     * */
    private fun checkDateAndStart() {
        Observable
            .just { db.activityDao().getLimitedActivities(1) }
            .observeOn(io())
            .map { it() }
            .subscribe {
                if (!it.isNullOrEmpty()) {
                    if(!compareDate(it.first().date)) {
                        db.activityDao().insert( readValuesFromFile() )
                        this.openFileOutput(DAY_VALUES_FILE, Context.MODE_PRIVATE).use { os ->
                            os.write("".toByteArray())
                        }
                        appReadySubject.onNext(DayActivity(Date(), 0, 0, null, null, 0f))
                    } else {
                        appReadySubject.onNext(readValuesFromFile())
                    }
                } else {
                    appReadySubject.onNext(readValuesFromFile())
                }
            }
    }
}

package com.metropolia.sensorproject.models

import android.app.Application
import android.content.Context
import android.location.Location

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.metropolia.sensorproject.DAY_VALUES_FILE

import com.metropolia.sensorproject.StepApp

import com.metropolia.sensorproject.database.AppDB
import com.metropolia.sensorproject.database.DayActivity
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers.io
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit


class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { AppDB.get(application) }
    private val getAllActivities: PublishSubject<Unit> = PublishSubject.create()
    private val insertNewActivity: PublishSubject<DayActivity> = PublishSubject.create()
    private val getLimitedActivities: PublishSubject<Int> = PublishSubject.create()
    private val route = mutableListOf<GeoPoint>()

    private var stepWriter: Disposable? = null

    val allActivitiesSubject: PublishSubject<List<DayActivity>> = PublishSubject.create()
    val limitedActivitiesSubject: PublishSubject<Pair<MutableList<Int>, List<DayActivity>>> = PublishSubject.create()

    val context = getApplication<Application>().applicationContext
    val emitStepsCount: PublishSubject<Int> = PublishSubject.create()
    val strtingLocationSubject: ReplaySubject<Location> = ReplaySubject.create()
    val routeLocationSubject: PublishSubject<MutableList<GeoPoint>> = PublishSubject.create()
    var stepCount = StepApp.getStepCount()

    init {
        StepApp
            .stepCountSubject
            .subscribe {
                stepCount = it
                emitStepsCount.onNext(it)
            }

        StepApp
            .locationSubject
            .observeOn(io())
            .subscribe {
                // Use this for testing and generating random route.
                // val geoPoint = GeoPoint(it.latitude + Random.nextFloat(), it.longitude - Random.nextFloat())
                val geoPoint = GeoPoint(it.latitude, it.longitude)
                route.add(geoPoint)
                routeLocationSubject.onNext(route)
            }

        StepApp
            .locationSubject
            .take(1)
            .subscribe {
                strtingLocationSubject.onNext(it)
            }


        getAllActivities
            .observeOn(io())
            .subscribe {
                val activites = db.activityDao().getAll()
                allActivitiesSubject.onNext(activites)
            }

        insertNewActivity
            .observeOn(io())
            .subscribe { db.activityDao().insert(it) }

        getLimitedActivities
            .observeOn(io())
            .subscribe {
                val query = db.activityDao().getLimitedActivities(it)
                val steps = mutableListOf<Int>()
                query.forEach { day ->
                    day.Steps?.let { it1 -> steps.add(it1) }
                }
                limitedActivitiesSubject.onNext(Pair(steps, query))
            }

    }

    fun getAllActivities() {
        getAllActivities.onNext(Unit)
    }

    fun insertActivity(activity: DayActivity) {
        insertNewActivity.onNext(activity)
    }

    fun getLimitedActivities(limit: Int) {
        getLimitedActivities.onNext(limit)
    }

    fun startStepWriter() {
        stepWriter = Observable
            .interval(5, TimeUnit.SECONDS)
            .timeInterval()
            .observeOn(io())
            .subscribe {
                if (StepApp.getStepCount() != 0) {
                    context.openFileOutput(DAY_VALUES_FILE, Context.MODE_PRIVATE).use {
                        Log.i("XXX", "writing steps")
                        val json = Gson().toJson(StepApp.dayActivity)
                        it?.write(json.toByteArray())
                    }
                }
            }
    }
    fun disposeWriter() {
        stepWriter?.dispose()
    }
}
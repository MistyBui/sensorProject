package com.metropolia.sensorproject

import android.Manifest
import android.location.Location
import android.os.SystemClock
import android.widget.Chronometer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.metropolia.sensorproject.database.DayActivity
import com.metropolia.sensorproject.models.Weather
import com.metropolia.sensorproject.services.WeatherApi
import io.reactivex.rxjava3.kotlin.withLatestFrom
import io.reactivex.rxjava3.schedulers.Schedulers.io
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import org.osmdroid.util.GeoPoint
import java.lang.reflect.Type

/**
 *  Global object with datastreams and essential app data.
 * */
object StepApp {

    private val gson = Gson()
    private var stepCount = 0
    private var route = mutableListOf<GeoPoint>()
    private var distance: Float = 0f
    private var weather: Weather? = null
    private var timer: Long = 0
    private var startingLocation: Location? = null
    private var previousLocation: Location? = null
    private val api = WeatherApi().service
    private val getCurrentWeather: PublishSubject<Unit> = PublishSubject.create()

    val stepStream: PublishSubject<Int> = PublishSubject.create()
    val locationStream: ReplaySubject<Location> = ReplaySubject.create()
    val weatherStream: PublishSubject<Weather> = PublishSubject.create()
    val locationServiceStream: PublishSubject<Pair<String, MutableList<GeoPoint>>> = PublishSubject.create()
    var chronometer: Chronometer? = null

    /**
     *  Gets the weather on init
     * */
    init {
        getCurrentWeather
            .withLatestFrom(locationStream)
            .observeOn(io())
            .flatMap { (_, location) ->
                api.fetchWeather(location.latitude, location.longitude, WEATHER_API_KEY)
            }
            .subscribe {
                weather = it
                weatherStream.onNext(it)
            }
    }

    /**
     *  updates stepcount and passes the value to the stream
     *  @param steps Int
     * */
    fun updateStepCount(steps: Int? = null) {
        if (steps == null ) { stepCount ++ } else { stepCount = steps }
        timer = SystemClock.elapsedRealtime() - chronometer?.base!!
        stepStream.onNext(stepCount)
    }

    /**
     * sets starting location
     * @param location Location
     * */
    fun setStartingPoint(location: Location) {
        startingLocation = location
        previousLocation = location
    }

    /**
     *  Updates location and passes it to stream.
     *  also updates traveled distance.
     *  @param location Location
     * */
    fun updateLocation(location: Location) {
        if (previousLocation != null) {
            val distanceFromPrevious = location.distanceTo(previousLocation)
            if (distanceFromPrevious > 25) {
                distance += distanceFromPrevious
                previousLocation = location
            }
        }
        val (lat, lon) = Pair(location.latitude, location.longitude)
        route.add(GeoPoint(lat, lon))
        locationServiceStream.onNext(Pair(distance.toInt().toString(), route))
    }

    /**
     * getters for values
     * */
    fun getCurrentWeather() { getCurrentWeather.onNext(Unit) }
    fun getStepCount(): Int { return stepCount }
    fun getTimer(): Long { return timer }
    fun getWeather(): String { return gson.toJson(weather) }
    fun getRoute(): String { return gson.toJson(route) }
    fun getDistance(): Float { return distance}

    /**
     *  Setter for initial values.
     *  @param day DayActivity
     * */
    fun setValues(day: DayActivity) {
        val routeList: Type = object : TypeToken<MutableList<GeoPoint?>?>() {}.type
        stepCount = day.Steps
        timer = day.timer
        distance = day.distance
        weather = gson.fromJson(day.weather, Weather::class.java)
        val routeFromJson: MutableList<GeoPoint>? = gson.fromJson(day.route, routeList)
        route = routeFromJson ?: mutableListOf()
        stepStream.onNext(stepCount)
        locationServiceStream.onNext(Pair(distance.toString(), route))
    }

    fun setChronoListener() { chronometer?.onChronometerTickListener }

    fun setChronoBase() { chronometer?.base = SystemClock.elapsedRealtime() - timer }

}

/**
 *  App constant values
 * */
const val REQUEST_ALL_NEEDED_PERMISSIONS = 999
const val DAY_VALUES_FILE = "values.txt"
const val WEATHER_API_URL= "https://api.openweathermap.org/data/2.5/"
val PERMISSIONS =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
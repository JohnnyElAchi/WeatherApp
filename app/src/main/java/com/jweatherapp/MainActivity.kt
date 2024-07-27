package com.jweatherapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import android.location.Location
import android.view.View
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var locTextView: TextView
    private lateinit var tempTextView: TextView
    private lateinit var conditionTextView: TextView
    private lateinit var iconImageView: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize TextViews and ImageView
        locTextView = findViewById(R.id.loc)
        tempTextView = findViewById(R.id.temp)
        conditionTextView = findViewById(R.id.condition)
        iconImageView = findViewById(R.id.icon) // Update ID according to your ImageView's ID
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()

        // Check for permissions and request location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            startLocationUpdates()
            getLastLocation()
        }

//        val apiKey = "590bb090775143bda99102504242707"
//        val location = "lebanon"
//
//        ApiClient.retrofitInstance.getCurrentWeather(apiKey, location).enqueue(object : Callback<WeatherResponse> {
//            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
//                if (response.isSuccessful) {
//                    val weatherResponse = response.body()
//                    Log.d("MainActivitySuccess", "Weather: $weatherResponse")
//                    // Handle the weather data
//                    // Update UI with the location name
//                    locTextView.text = weatherResponse?.location?.name
//                    // Optionally update other TextViews with temperature and condition
//                    tempTextView.text = "${weatherResponse?.current?.temp_c} °C"
//                    conditionTextView.text = weatherResponse?.current?.condition?.text
//
//
//
//                } else {
//                    Log.e("MainActivityError", "Failed to get response")
//                }
//            }
//
//            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
//                Log.e("MainActivityFailed", "Error: ${t.message}")
//            }
//        })
    }



    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // High accuracy
        }
    }


    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location != null) {
                        // Use the updated location
                        getWeatherData(location.latitude, location.longitude)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Get the latitude and longitude
                val latitude = location.latitude
                val longitude = location.longitude
                // Use latitude and longitude to get weather information
                getWeatherData(latitude, longitude)
            } else {
                Log.e("MainActivity", "Location not available")
            }
        }
    }


    private fun getWeatherData(latitude: Double, longitude: Double) {
        val apiKey = "590bb090775143bda99102504242707"
        val location = "$latitude,$longitude" // Format for the API

        ApiClient.retrofitInstance.getCurrentWeather(apiKey, location).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    if (weatherResponse != null) {
                        locTextView.text = weatherResponse.location.name
                        tempTextView.text = "${weatherResponse.current.temp_c} °C"
                        conditionTextView.text = weatherResponse.current.condition.text
                        val iconUrl = "https:${weatherResponse.current.condition.icon}"
                        // Load the weather icon
                        Glide.with(this@MainActivity).load(iconUrl).into(iconImageView)

                        // Update background based on day/night and weather condition
                        val isDay = weatherResponse.current.is_day // Get the is_day property
                        val conditionCode = weatherResponse.current.condition.code
                        val backgroundResId = when {
                            isDay == 1 -> { // Daytime
                                when (conditionCode) {
                                    1000 -> R.drawable.sunybackground      // Clear day
                                    1003, 1006 -> R.drawable.cloudybackground // Cloudy conditions
                                    1009, 1030 -> R.drawable.foggybackground  // Foggy conditions
                                    1063, 1183 -> R.drawable.rainybackground  // Rainy conditions
                                    1135, 1192 -> R.drawable.snowybackground  // Snowy conditions
                                    else -> R.drawable.sunybackground // Default day background
                                }
                            }
                            else -> { // Nighttime
                                when (conditionCode) {
                                    1000 -> R.drawable.space_background     // Clear night
                                    1003, 1006 -> R.drawable.nightlycloudybackground // Cloudy night
                                    1009, 1030 -> R.drawable.foggybackground  // Foggy conditions
                                    1063, 1183 -> R.drawable.rainybackground  // Rainy night
                                    1135, 1192 -> R.drawable.snowybackground  // Snowy night
                                    else -> R.drawable.space_background // Default night background
                                }
                            }
                        }
                        findViewById<View>(R.id.main).setBackgroundResource(backgroundResId)
                    }
                } else {
                    Log.e("MainActivity", "Failed to get response")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("MainActivity", "Error: ${t.message}")
            }
        })
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            } else {
                Log.e("MainActivity", "Location permission denied")
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}
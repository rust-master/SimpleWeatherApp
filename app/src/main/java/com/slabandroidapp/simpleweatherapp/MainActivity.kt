package com.slabandroidapp.simpleweatherapp

import android.Manifest
import android.R
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.slabandroidapp.simpleweatherapp.Remote.GPSTracker
import com.slabandroidapp.simpleweatherapp.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // View Binding

    var gpsTracker: GPSTracker? = null // GPS Tracker Object
    var MY_PERMISSION_ACCESS_COURSE_LOCATION = 1 // Location Permission Code

    var check: Boolean = false;  // Check Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isNetworkConnected())  // Internet Connection Checking
        {
            gpsTracker = GPSTracker(applicationContext)
            if (gpsTracker!!.getIsGPSTrackingEnabled()) // Checking GPS/network in settings
            {
                check = checkLocationPermission() // Check and Ask for App Permission Location to Allow
            }
            else
            {
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                showSettingsAlert()
            }
            if (check) {
                binding.connection.isVisible = false
                binding.cardView.isVisible = true
                loadData()  // Load in Card View
                // Animation
                val animation: Animation = AnimationUtils.loadAnimation(this,R.anim.slide_in_left)
                binding.cardView.startAnimation(animation)
            }
            else
            {
                checkLocationPermission()
            }
        }
        else
        {
            binding.connection.isVisible = true
            binding.cardView.isVisible = false
            // Retrieving Data from Shared Preference When no internet to display
            val preferences = getSharedPreferences("WeatherPref", MODE_PRIVATE)
            val code = preferences.getString("remember", "")
            if (code == "true")
            {
                binding.connection.isVisible = false
                binding.cardView.isVisible = true
                val tempPref = preferences.getString("temp", "")
                val countryPref = preferences.getString("country", "")
                val cityPref = preferences.getString("city", "")
                val descPref = preferences.getString("desc", "")
                val addressPref = preferences.getString("address", "")

                binding.temp.setText(tempPref)
                binding.countaryTxt.setText(countryPref)
                binding.cityName.setText(cityPref)
                binding.tempDescribe.setText(descPref)
                binding.locTxt.setText(addressPref)
                val animation: Animation = AnimationUtils.loadAnimation(this,R.anim.slide_in_left)
                binding.cardView.startAnimation(animation)
            }
            else if (code == "false")
            {
                Toast.makeText(this, "No Data Stored in Pref", Toast.LENGTH_SHORT).show()
            }
        }

        // Swipe Refresh to load Data Again
        binding.pullToRefresh.setOnRefreshListener {

            if (isNetworkConnected())
            {
                gpsTracker = GPSTracker(applicationContext)
                if (gpsTracker!!.getIsGPSTrackingEnabled())
                {
                    check = checkLocationPermission()
                }
                else
                {
                    // can't get location
                    // GPS or Network is not enabled
                    // Ask user to enable GPS/network in settings
                    showSettingsAlert()
                }
                if (check) {
                    binding.connection.isVisible = false
                    binding.cardView.isVisible = true
                    loadData()
                    val animation: Animation = AnimationUtils.loadAnimation(this,R.anim.slide_in_left)
                    binding.cardView.startAnimation(animation)
                }
                else
                {
                    checkLocationPermission()
                }
            }
            else
            {
                binding.connection.isVisible = true
                binding.cardView.isVisible = false
                val preferences = getSharedPreferences("WeatherPref", MODE_PRIVATE)
                val code = preferences.getString("remember", "")
                if (code == "true")
                {
                    binding.connection.isVisible = false
                    binding.cardView.isVisible = true
                    val tempPref = preferences.getString("temp", "")
                    val countryPref = preferences.getString("country", "")
                    val cityPref = preferences.getString("city", "")
                    val descPref = preferences.getString("desc", "")
                    val addressPref = preferences.getString("address", "")

                    binding.temp.setText(tempPref)
                    binding.countaryTxt.setText(countryPref)
                    binding.cityName.setText(cityPref)
                    binding.tempDescribe.setText(descPref)
                    binding.locTxt.setText(addressPref)
                    val animation: Animation = AnimationUtils.loadAnimation(this,R.anim.slide_in_left)
                    binding.cardView.startAnimation(animation)

                }
                else if (code == "false")
                {
                    Toast.makeText(this, "No Data Stored in Pref", Toast.LENGTH_SHORT).show()
                }
            }

            binding.pullToRefresh.isRefreshing = false
        }

    }

    // Network Checking
    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo.isConnected
    }

    // Calling API, Getting Data, Loading in Screen and Storing in the Shared Preference
    @Suppress("DEPRECATION")
    private fun loadData() {
        val mDialog = ProgressDialog(this).also {
            it.setMessage("Please wait Updating Weather...")
            it.show()
        }


        var stringLatitude: String? = null
        var stringLongitude: String? = null
        var countryName: String? = null
        var addressLine: String? = null
        if (gpsTracker!!.getIsGPSTrackingEnabled()) {
            // Getting Log and Lat, Address and Country from GPS
            stringLatitude = gpsTracker!!.getLatitude().toString()
            stringLongitude = gpsTracker!!.getLongitude().toString()
            addressLine = gpsTracker!!.getAddressLine(applicationContext)
            countryName = gpsTracker!!.getCountryName(applicationContext)
        } else {

            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            showSettingsAlert()
        }
        val API_KEY = "1b64d48d618d9d9760b14d524944668d" // API key from Open Weather Map Website
        val webUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$stringLatitude&lon=$stringLongitude&appid=$API_KEY"
        val queue: RequestQueue = Volley.newRequestQueue(applicationContext) // Volley Request
        // Json Object Request
        val request = JsonObjectRequest(Request.Method.GET, webUrl, null, { response ->
            try {
                mDialog.dismiss()
                // Getting Data
                var objectJs: JSONObject = response.getJSONObject("main")
                var temperature: String = objectJs.getString("temp")
                var temp = temperature.toDouble() - 273.15
                val nameCity: String = response.getString("name")
                binding.temp.setText(temp.toString().substring(0, 5) + "°C")
                val arrayDes: JSONArray = response.getJSONArray("weather")
                val objectDes: JSONObject = arrayDes.getJSONObject(0)
                val describe: String = objectDes.getString("description")
                binding.countaryTxt.setText(countryName)
                binding.tempDescribe.setText(describe)
                binding.cityName.setText(nameCity)
                binding.locTxt.setText(addressLine)
                // Storing in Shared Preference
                val preferences = getSharedPreferences("WeatherPref", MODE_PRIVATE)
                val editor = preferences.edit()
                editor.putString("temp", temp.toString().substring(0, 5) + "°C")
                editor.putString("country", countryName)
                editor.putString("city", nameCity)
                editor.putString("desc", describe)
                editor.putString("address", addressLine)
                editor.putString("remember", "true")
                editor.apply()

            } catch (e: JSONException) {
                // If Exception Not Storing Weather Data
                val preferences = getSharedPreferences("WeatherPref", MODE_PRIVATE)
                val editor = preferences.edit()
                editor.putString("remember", "false")
                editor.apply()
                Toast.makeText(this, "Json Error : $e", Toast.LENGTH_SHORT).show()
            }
        },
            {
                // If Error Not Storing Weather Data
                Toast.makeText(this, "Not Working", Toast.LENGTH_SHORT).show()
                val preferences = getSharedPreferences("WeatherPref", MODE_PRIVATE)
                val editor = preferences.edit()
                editor.putString("remember", "false")
                editor.apply()
            }
        )
        queue.add(request)
    }

    fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(this)

        //Setting Dialog Title
        alertDialog.setTitle("GPS Setting")

        //Setting Dialog Message
        alertDialog.setMessage("Kindly Turn On The Location")


        //On Pressing Setting button
        alertDialog.setPositiveButton("Settings",
            DialogInterface.OnClickListener { dialog, which ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            })

        //On pressing cancel button
        alertDialog.setNegativeButton(R.string.cancel,
            DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
        alertDialog.show()
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                MY_PERMISSION_ACCESS_COURSE_LOCATION
            )
            return false
        } else {
            Toast.makeText(applicationContext, "Location On", Toast.LENGTH_SHORT).show()
            return true
        }
    }
}
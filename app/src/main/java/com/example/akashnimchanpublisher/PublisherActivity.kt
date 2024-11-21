package com.example.akashnimchanpublisher

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID
import org.json.JSONObject

class PublisherActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var client: Mqtt5BlockingClient? = null
    private var isLocationUpdatesEnabled = false
    private var clientID = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_publisher)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).apply {
            setMinUpdateIntervalMillis(5000L) // Minimum time interval for updates
            try {
                client?.connect()
            } catch (e:Exception){
                Toast.makeText(this@PublisherActivity,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
            }
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    val textToSend = JSONObject().apply {
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                        put("id", clientID)
                        put("timestamp", location.time)
                        put("speed", location.speed)
                    }.toString()

                    try{
                        client?.publishWith()?.topic("assignment/location")?.payload(textToSend.toByteArray())?.send()
                        println(textToSend)
                    } catch (e : Exception){
                        Toast.makeText(this@PublisherActivity, "An error occurred when sending a message to the broker", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isLocationUpdatesEnabled) {
            stopLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isLocationUpdatesEnabled) {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        isLocationUpdatesEnabled = true;
        if (client?.state?.isConnected == false) {
            try {
                client?.connect()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PublisherActivity,
                    "An error occurred when connecting to broker",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        isLocationUpdatesEnabled = false
        if (client?.state?.isConnected == true) {
            try {
                client?.disconnect()
            } catch (e:Exception){
                Toast.makeText(this,"An error occurred when disconnecting from broker", Toast.LENGTH_SHORT).show()
            }
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun stopLocationUpdates(view: View) {
        stopLocationUpdates()
    }

    fun startLocationUpdates(view: View) {
        val inputField = findViewById<EditText>(R.id.studentID_Input)
        val textInput = inputField.text.toString()

        if (textInput.isEmpty()) {
            Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show()
            return
        }

        clientID = textInput
        startLocationUpdates()
    }

}
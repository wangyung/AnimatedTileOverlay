package com.freddiew.animatedtile.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.freddiew.animatedtile.app.tileprovider.AnimatedTileProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

private const val LOCATION_PERMISSION_REQUEST_CODE = 1

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var animateTileProvider: AnimatedTileProvider

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tileAnimationController: TileAnimationController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        animateTileProvider = AnimatedTileProvider(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (hasLocationPermission()) {
            enableMyLocation()
            getLastKnowLocation()?.also {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(it.toLatLng(), 12f))
            }
            initUi()
        } else {
            requestLocationPermission()
        }
    }

    private fun initUi() {
        val overlay1CheckBox: CheckBox = findViewById(R.id.overlay1_check)

        tileAnimationController = TileAnimationController(map, animateTileProvider)

        overlay1CheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tileAnimationController.showTile()
                animateTile()
            } else {
                tileAnimationController.hideTile()
                handler.removeCallbacks(animatedTileRunnable)
            }
        }
    }

    private val animatedTileRunnable: Runnable = Runnable {
        animateTile()
    }

    private fun animateTile() {
        tileAnimationController.currentTimeStamp += 1
        handler.postDelayed(animatedTileRunnable, 1000)
    }

    private fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)

    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE)
    private fun getLastKnowLocation(): Location? {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> null
        }
        return if (hasLocationPermission()) {
            provider?.let { locationManager.getLastKnownLocation(provider) }
        } else {
            null
        }
    }

    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE)
    private fun enableMyLocation() {
        // Enable the location layer. Request the location permission if needed.
        map.isMyLocationEnabled = true
    }

    private fun hasLocationPermission(): Boolean =
        EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)

    private fun requestLocationPermission() =
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.permission_rationale_location),
            LOCATION_PERMISSION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    companion object {
        private const val TAG = "MapsActivity"
    }
}

package com.example.madadgarapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.madadgarapp.R
import com.example.madadgarapp.utils.LocationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Fragment for picking location with GPS and manual input
 */
class LocationPickerFragment : Fragment() {
    
    private lateinit var etLocation: TextInputEditText
    private lateinit var btnGetCurrentLocation: MaterialButton
    private lateinit var btnUseLocation: MaterialButton
    
    private var selectedCoordinates: LocationUtils.Coordinates? = null
    private var locationCallback: LocationCallback? = null
    
    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    interface LocationCallback {
        fun onLocationSelected(location: String, coordinates: LocationUtils.Coordinates?)
    }
    
    companion object {
        @JvmStatic
        fun newInstance(): LocationPickerFragment {
            return LocationPickerFragment()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_picker, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
    }
    
    private fun initViews(view: View) {
        etLocation = view.findViewById(R.id.et_location)
        btnGetCurrentLocation = view.findViewById(R.id.btn_get_current_location)
        btnUseLocation = view.findViewById(R.id.btn_use_location)
    }
    
    private fun setupListeners() {
        btnGetCurrentLocation.setOnClickListener {
            requestLocationPermissionAndGetLocation()
        }
        
        btnUseLocation.setOnClickListener {
            val location = etLocation.text.toString().trim()
            if (location.isNotEmpty()) {
                locationCallback?.onLocationSelected(location, selectedCoordinates)
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun requestLocationPermissionAndGetLocation() {
        if (context?.let { LocationUtils.hasLocationPermission(it) } == true) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(LocationUtils.REQUIRED_PERMISSIONS)
        }
    }
    
    private fun getCurrentLocation() {
        lifecycleScope.launch {
            try {
                btnGetCurrentLocation.isEnabled = false
                btnGetCurrentLocation.text = "Getting location..."
                
                val coordinates = context?.let { LocationUtils.getCurrentLocation(it) }
                
                if (coordinates != null) {
                    selectedCoordinates = coordinates
                    val address = context?.let {
                        LocationUtils.getAddressFromCoordinates(it, coordinates)
                    } ?: coordinates.toString()
                    
                    etLocation.setText(address)
                    Toast.makeText(context, "Location found!", Toast.LENGTH_SHORT).show()
                    
                    // Automatically trigger callback to update ShareItemFragment
                    locationCallback?.onLocationSelected(address, selectedCoordinates)
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnGetCurrentLocation.isEnabled = true
                btnGetCurrentLocation.text = "Get Current Location"
            }
        }
    }
    
    fun setLocationCallback(callback: LocationCallback) {
        this.locationCallback = callback
    }
}

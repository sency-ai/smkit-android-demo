package com.example.smkitdemoapp

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.smkitdemoapp.databinding.MainActivityBinding
import com.example.smkitdemoapp.fragments.ConfigureFragment
import com.example.smkitdemoapp.fragments.SelectExerciesFragment
import com.example.smkitdemoapp.fragments.WorkoutFragment
import com.example.smkitdemoapp.viewModels.ActivityViewModel

class MainActivity : FragmentActivity() {

    private var _binding: MainActivityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by viewModels()

    private val launcher = registerForActivityResult(RequestMultiplePermissions()) { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in PERMISSIONS_REQUIRED && !it.value) permissionGranted = false
        }
        if (permissionGranted && permissions.isNotEmpty()) {
            navigateToWorkout()
        }
        if (!permissionGranted) {
            Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToWorkout() {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.nav_host_fragment, ConfigureFragment())
        }.commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        if (!hasPermissions(baseContext)) {
            launcher.launch(PERMISSIONS_REQUIRED)
        } else {
            navigateToWorkout()
        }
    }

    companion object {
        private val PERMISSIONS_REQUIRED = arrayOf(permission.CAMERA)

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

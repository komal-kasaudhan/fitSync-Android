package com.esha.fitsync.screens.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.esha.fitsync.R
import com.esha.fitsync.screens.LoginActivity
import com.esha.fitsync.screens.Nutrition.NutritionActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvWelcomeUser = view.findViewById<TextView>(R.id.tvWelcomeUser)
        val sharedPreferences = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("USER_NAME", "Esha")
        tvWelcomeUser?.text = "Good Morning, $userName 👋"

        val btnManageNutrition = view.findViewById<Button>(R.id.btnManageNutrition)
        btnManageNutrition?.setOnClickListener {
            val intent = Intent(requireContext(), NutritionActivity::class.java)
            startActivity(intent)
        }

        // Sign Out Triggers: Profile Avatar & Bottom Sign Out Button
        val btnProfile = view.findViewById<View>(R.id.btnProfile)
        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)

        btnProfile?.setOnClickListener {
            showSignOutConfirmationDialog()
        }

        btnSignOut?.setOnClickListener {
            showSignOutConfirmationDialog()
        }
    }

    private fun showSignOutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out of FitSync?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Sign Out") { dialog, _ ->
                dialog.dismiss()
                performSignOut()
            }
            .show()
    }

    private fun performSignOut() {
        val sharedPreferences = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
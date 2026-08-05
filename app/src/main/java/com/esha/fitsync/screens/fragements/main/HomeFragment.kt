package com.esha.fitsync.screens.fragments

import android.R.attr.start
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.esha.fitsync.R
import com.esha.fitsync.screens.Nutrition.NutritionActivity
import kotlinx.coroutines.NonCancellable.start
import kotlin.jvm.java

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


        btnManageNutrition.setOnClickListener {
            val intent = Intent(requireContext(), NutritionActivity::class.java)
            startActivity(intent)
        }

    }
}
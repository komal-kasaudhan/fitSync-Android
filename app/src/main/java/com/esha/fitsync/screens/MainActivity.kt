package com.esha.fitsync.screens

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.esha.fitsync.R
import com.esha.fitsync.screens.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        val fabAiCoach = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAiCoach)
        fabAiCoach?.setOnClickListener {
            val aiChatSheet = com.esha.fitsync.screens.bottomsheet.AiChatBottomSheet()
            aiChatSheet.show(supportFragmentManager, "AiChatBottomSheet")
        }

        if (savedInstanceState == null) {
            val targetFragment = intent.getStringExtra("TARGET_FRAGMENT")
            val fromLoading = intent.getBooleanExtra("FROM_LOADING", false) // ✅ EXTRA EXTRACTED

            if (targetFragment == "WORKOUT_FRAGMENT") {
                val workoutFragment = WorkoutFragment()


                if (fromLoading) {
                    val bundle = Bundle().apply {
                        putBoolean("FORCE_LOAD_DASHBOARD", true)
                    }
                    workoutFragment.arguments = bundle
                }

                loadFragment(workoutFragment)
                bottomNavigationView.selectedItemId = R.id.nav_workout
            } else {
                loadFragment(HomeFragment())
                bottomNavigationView.selectedItemId = R.id.nav_home
            }
        }


        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_workout -> WorkoutFragment()
                R.id.nav_ai_coach -> ExploreFragment()
                R.id.nav_community -> CommunityFragment()
                else -> HomeFragment()
            }

            loadFragment(selectedFragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment) // Tumhara standard R.id.fragmentContainer container target matched
            .commit()
    }
}
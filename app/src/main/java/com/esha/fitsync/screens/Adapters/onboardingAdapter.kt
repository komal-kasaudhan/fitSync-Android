package com.esha.fitsync.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.esha.fitsync.screens.fragments.Onboarding.BioProfileFragment
import com.esha.fitsync.screens.fragments.Onboarding.GoalFragment
import com.esha.fitsync.screens.fragments.Onboarding.HealthFragment
import com.esha.fitsync.screens.fragments.Onboarding.LifestyleFragment

class OnboardingAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> BioProfileFragment()
            1 -> GoalFragment()
            2 -> LifestyleFragment()
            3 -> HealthFragment()
            else -> BioProfileFragment()
        }
    }
}
package com.esha.fitsync.viewmodels

import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {

    var gender = ""

    var age = 18

    var height = 170

    var currentWeight = 60

    var targetWeight = 60

    var timeline = ""

    var goal = ""

    var activityLevel = ""

    var workoutLocation = ""

    var selectedMedicalConditions = mutableListOf<String>()

    var physicalLimitations = ""

    var notes = ""

    var isHealthCleared = false

}
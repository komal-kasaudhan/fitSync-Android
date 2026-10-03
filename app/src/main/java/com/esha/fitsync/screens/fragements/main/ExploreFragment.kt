package com.esha.fitsync.screens.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.GymItem
import com.esha.fitsync.screens.gym.GymOwnerDashboardActivity
import com.esha.fitsync.screens.gym.MyGymBookingsActivity
import com.esha.fitsync.screens.gym.NearbyGymsFragment
import com.esha.fitsync.screens.gym.RegisterGymActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {

    private var myGym: GymItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardNearbyGyms = view.findViewById<MaterialCardView>(R.id.cardNearbyGyms)
        val cardMyBookings = view.findViewById<MaterialCardView>(R.id.cardMyBookings)
        val cardTrainers = view.findViewById<MaterialCardView>(R.id.cardTrainers)
        val cardShop = view.findViewById<MaterialCardView>(R.id.cardShop)
        val cardRegisterGym = view.findViewById<MaterialCardView>(R.id.cardRegisterGym)
        val cardAdminPortal = view.findViewById<MaterialCardView>(R.id.cardAdminPortal)
        cardAdminPortalRef = cardAdminPortal

        cardAdminPortal.setOnClickListener {
            startActivity(Intent(requireContext(), com.esha.fitsync.screens.admin.AdminDashboardActivity::class.java))
        }

        cardNearbyGyms.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NearbyGymsFragment())
                .addToBackStack("EXPLORE_TO_NEARBY")
                .commit()
        }

        cardMyBookings.setOnClickListener {
            startActivity(Intent(requireContext(), MyGymBookingsActivity::class.java))
        }

        cardTrainers.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Trainers & Nutritionists feature is coming soon in the next update!",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardShop.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "FitSync Store & Supplements feature is coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardRegisterGym.setOnClickListener {
            val gym = myGym
            if (gym != null && gym.status.equals("approved", ignoreCase = true)) {
                val intent = Intent(requireContext(), GymOwnerDashboardActivity::class.java).apply {
                    putExtra("GYM_ID", gym.id)
                    putExtra("GYM_NAME", gym.name)
                }
                startActivity(intent)
            } else {
                val intent = Intent(requireContext(), RegisterGymActivity::class.java).apply {
                    if (gym != null) {
                        putExtra("GYM_ID", gym.id)
                        putExtra("STATUS", gym.status)
                        putExtra("REASON", gym.rejectionReason)
                    }
                }
                startActivity(intent)
            }
        }

        fetchGymPartnerStatus(view)
        checkAdminStatus(cardAdminPortal)
    }

    private fun checkAdminStatus(cardAdmin: MaterialCardView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getAdminMe()
                if (response.isSuccessful && response.body()?.success == true) {
                    val roles = response.body()?.user?.roles ?: emptyList()
                    if (roles.contains("admin")) {
                        cardAdmin.visibility = View.VISIBLE
                    } else {
                        cardAdmin.visibility = View.GONE
                    }
                } else {
                    cardAdmin.visibility = View.GONE
                }
            } catch (_: Exception) {
                cardAdmin.visibility = View.GONE
            }
        }
    }

    private var cardAdminPortalRef: MaterialCardView? = null

    override fun onResume() {
        super.onResume()
        view?.let {
            fetchGymPartnerStatus(it)
            cardAdminPortalRef?.let { card -> checkAdminStatus(card) }
        }
    }

    private fun fetchGymPartnerStatus(rootView: View) {
        val tvTitle = rootView.findViewById<TextView>(R.id.tvGymPartnerTitle)
        val tvStatus = rootView.findViewById<TextView>(R.id.tvGymPartnerStatus)
        val tvSubtitle = rootView.findViewById<TextView>(R.id.tvGymPartnerSubtitle)
        val tvAction = rootView.findViewById<TextView>(R.id.tvGymPartnerAction)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMyGym()
                if (response.isSuccessful && response.body()?.gym != null) {
                    val gym = response.body()!!.gym!!
                    myGym = gym
                    when (gym.status.lowercase()) {
                        "approved" -> {
                            tvTitle.text = gym.name
                            tvStatus.text = "APPROVED"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_approved))
                            tvSubtitle.text = "Welcome partner! Manage your gym slots, verify 6-digit member check-ins, and view earnings."
                            tvAction.text = "Open Partner Dashboard →"
                        }
                        "pending" -> {
                            tvTitle.text = gym.name
                            tvStatus.text = "PENDING APPROVAL"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_pending))
                            tvSubtitle.text = "Your gym application is under review by FitSync admin. We will notify you once verified."
                            tvAction.text = "View Application →"
                        }
                        "rejected" -> {
                            tvTitle.text = gym.name
                            tvStatus.text = "REJECTED"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rejected))
                            val reason = gym.rejectionReason ?: "Details require updating"
                            tvSubtitle.text = "Application rejected: $reason. Tap to correct and resubmit."
                            tvAction.text = "Update & Resubmit →"
                        }
                        else -> {
                            setUnregisteredState(tvTitle, tvStatus, tvSubtitle, tvAction)
                        }
                    }
                } else {
                    myGym = null
                    setUnregisteredState(tvTitle, tvStatus, tvSubtitle, tvAction)
                }
            } catch (_: Exception) {
                // Keep default state or last good cache
            }
        }
    }

    private fun setUnregisteredState(
        tvTitle: TextView,
        tvStatus: TextView,
        tvSubtitle: TextView,
        tvAction: TextView
    ) {
        tvTitle.text = "Register Your Gym"
        tvStatus.text = "BECOME A PARTNER"
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gym_accent))
        tvSubtitle.text = "List your fitness center on FitSync to get instant footfall, seamless slot bookings, and verified attendance."
        tvAction.text = "Apply Now →"
    }
}

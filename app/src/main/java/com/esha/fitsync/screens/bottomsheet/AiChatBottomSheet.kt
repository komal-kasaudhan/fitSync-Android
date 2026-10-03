package com.esha.fitsync.screens.bottomsheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.AiChatMessage
import com.esha.fitsync.models.AskAiRequest
import com.esha.fitsync.screens.Adapters.AiChatAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class AiChatBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var llTyping: LinearLayout
    private lateinit var llErrorRetry: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var btnRetry: MaterialButton

    private lateinit var adapter: AiChatAdapter
    private var conversationId: String? = null
    private var lastFailedMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_ai_chat, container, false)
    }

    override fun onStart() {
        super.onStart()
        // Expand bottom sheet to take full height for smooth chat experience
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = true
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvMessages = view.findViewById(R.id.rvAiChatMessages)
        etInput = view.findViewById(R.id.etChatInput)
        btnSend = view.findViewById(R.id.btnSendChat)
        btnClose = view.findViewById(R.id.btnCloseChat)
        llTyping = view.findViewById(R.id.llTypingIndicator)
        llErrorRetry = view.findViewById(R.id.llErrorRetry)
        tvError = view.findViewById(R.id.tvErrorMessage)
        btnRetry = view.findViewById(R.id.btnRetryChat)

        adapter = AiChatAdapter()
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        btnClose.setOnClickListener { dismiss() }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        btnRetry.setOnClickListener {
            lastFailedMessage?.let { msg ->
                llErrorRetry.visibility = View.GONE
                sendMessage(msg)
            }
        }

        // Suggestion Chips
        view.findViewById<Chip>(R.id.chipProtein).setOnClickListener {
            sendMessage("What should I eat for protein?")
        }
        view.findViewById<Chip>(R.id.chipModifyWorkout).setOnClickListener {
            sendMessage("Can you modify today's workout plan for me?")
        }
        view.findViewById<Chip>(R.id.chipKneePain).setOnClickListener {
            sendMessage("I have knee pain, what exercises should I avoid?")
        }

        loadChatHistory()
    }

    private fun loadChatHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getAiHistory(conversationId)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    conversationId = data.conversationId
                    if (data.messages.isNotEmpty()) {
                        adapter.setMessages(data.messages)
                        rvMessages.scrollToPosition(data.messages.size - 1)
                    } else {
                        // Add introductory coach message if empty
                        adapter.addMessage(
                            AiChatMessage(
                                role = "assistant",
                                content = "Hello! I am your FitSync AI Coach ⚡. Ask me anything about your diet, remaining protein target, or workout routine."
                            )
                        )
                    }
                } else {
                    adapter.addMessage(
                        AiChatMessage(
                            role = "assistant",
                            content = "Hello! I am your FitSync AI Coach ⚡. How can I help you crush your fitness goals today?"
                        )
                    )
                }
            } catch (e: Exception) {
                adapter.addMessage(
                    AiChatMessage(
                        role = "assistant",
                        content = "Hello! I am your FitSync AI Coach ⚡. How can I assist you with your fitness goals today?"
                    )
                )
            }
        }
    }

    private fun sendMessage(userText: String) {
        etInput.text.clear()
        hideKeyboard()

        // Display user message in chat
        adapter.addMessage(AiChatMessage(role = "user", content = userText))
        rvMessages.smoothScrollToPosition(adapter.itemCount - 1)

        llTyping.visibility = View.VISIBLE
        llErrorRetry.visibility = View.GONE
        lastFailedMessage = null

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.askAi(
                    AskAiRequest(message = userText, conversationId = conversationId)
                )
                llTyping.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    conversationId = body.conversationId ?: conversationId
                    adapter.addMessage(
                        AiChatMessage(
                            role = "assistant",
                            content = body.reply
                        )
                    )
                    rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
                } else {
                    lastFailedMessage = userText
                    llErrorRetry.visibility = View.VISIBLE
                    tvError.text = "Coach could not respond (${response.code()}). Tap to retry."
                }
            } catch (e: Exception) {
                llTyping.visibility = View.GONE
                lastFailedMessage = userText
                llErrorRetry.visibility = View.VISIBLE
                tvError.text = "Connection timeout or error. Check network and tap to retry."
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(etInput.windowToken, 0)
    }
}

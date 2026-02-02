package com.multiprofile.browser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileListActivity : AppCompatActivity() {

    private lateinit var profileManager: ProfileManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfileAdapter
    private lateinit var emptyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_list)

        profileManager = ProfileManager(this)
        
        // Check if we need to switch profile immediately
        if (intent.hasExtra("SWITCH_PROFILE_ID")) {
            val switchId = intent.getStringExtra("SWITCH_PROFILE_ID")
            // 0=Main, 1=Beta, 2=Gamma. Default to 0 if not present, but check Beta flag for legacy
            var processType = intent.getIntExtra("TARGET_PROCESS_TYPE", 0)
            if (intent.getBooleanExtra("TARGET_PROCESS_BETA", false) && processType == 0) {
                processType = 1
            }
            
            val profiles = profileManager.getProfiles()
            val targetProfile = profiles.find { it.id == switchId }
            
            if (targetProfile != null) {
                openProfile(targetProfile, processType)
                // We don't finish() here because this is the main menu, 
                // it should stay in the back stack.
                // But we clear the intent extra so it doesn't loop on recreation
                intent.removeExtra("SWITCH_PROFILE_ID")
            }
        }

        recyclerView = findViewById(R.id.profileRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddProfile)

        recyclerView.layoutManager = LinearLayoutManager(this)
        
        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEditProfileActivity::class.java))
        }

        loadProfiles()
    }

    override fun onResume() {
        super.onResume()
        loadProfiles()
    }

    private fun loadProfiles() {
        val profiles = profileManager.getProfiles()

        if (profiles.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter = ProfileAdapter(profiles) { profile, action ->
                when (action) {
                    ProfileAction.OPEN -> openProfile(profile)
                    ProfileAction.EDIT -> editProfile(profile)
                    ProfileAction.DELETE -> deleteProfile(profile)
                }
            }
            recyclerView.adapter = adapter
        }
    }

    private fun openProfile(profile: Profile, processType: Int = 0) {
        android.util.Log.d("ProfileListActivity", "Opening profile - ID: ${profile.id}, Name: ${profile.name}, ProcessType: $processType")
        
        val targetClass = when (processType) {
            1 -> WebViewActivityBeta::class.java
            2 -> WebViewActivityGamma::class.java
            else -> WebViewActivity::class.java
        }
        
        val intent = Intent(this, targetClass)
        intent.putExtra("PROFILE_ID", profile.id)
        intent.putExtra("PROFILE_NAME", profile.name)
        intent.putExtra("PROFILE_URL", profile.url)
        
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun editProfile(profile: Profile) {
        val intent = Intent(this, AddEditProfileActivity::class.java)
        intent.putExtra("PROFILE_ID", profile.id)
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it) // Update the intent stored in this activity
            
            if (it.hasExtra("SWITCH_PROFILE_ID")) {
                val switchId = it.getStringExtra("SWITCH_PROFILE_ID")
                var processType = it.getIntExtra("TARGET_PROCESS_TYPE", 0)
                if (it.getBooleanExtra("TARGET_PROCESS_BETA", false) && processType == 0) {
                     processType = 1
                }
                
                val profiles = profileManager.getProfiles()
                val targetProfile = profiles.find { p -> p.id == switchId }
                
                if (targetProfile != null) {
                   openProfile(targetProfile, processType)
                   it.removeExtra("SWITCH_PROFILE_ID")
                }
            }
        }
    }

    private fun deleteProfile(profile: Profile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Profile")
            .setMessage("Delete \"${profile.name}\"? This will clear all session data.")
            .setPositiveButton("Delete") { _, _ ->
                profileManager.deleteProfile(profile.id)
                loadProfiles()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

enum class ProfileAction {
    OPEN, EDIT, DELETE
}

class ProfileAdapter(
    private val profiles: List<Profile>,
    private val onAction: (Profile, ProfileAction) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconText: TextView = view.findViewById(R.id.profileIcon)
        val nameText: TextView = view.findViewById(R.id.profileName)
        val urlText: TextView = view.findViewById(R.id.profileUrl)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]

        // Set profile data
        holder.nameText.text = profile.name
        holder.urlText.text = profile.url
        
        // Set icon based on profile name or URL
        holder.iconText.text = when {
            profile.name.contains("work", ignoreCase = true) -> "💼"
            profile.name.contains("personal", ignoreCase = true) -> "✉️"
            profile.name.contains("shop", ignoreCase = true) -> "🛒"
            profile.url.contains("slack") -> "💬"
            profile.url.contains("gmail") || profile.url.contains("mail") -> "📧"
            profile.url.contains("twitter") || profile.url.contains("x.com") -> "🐦"
            profile.url.contains("github") -> "👨‍💻"
            profile.url.contains("amazon") -> "📦"
            profile.url.contains("meesho") -> "🛍️"
            else -> "🌐"
        }
        
        // Click to open profile
        holder.itemView.setOnClickListener {
            onAction(profile, ProfileAction.OPEN)
        }
        
        // Long press to show edit/delete menu
        holder.itemView.setOnLongClickListener {
            showContextMenu(holder.itemView, profile)
            true
        }
    }
    
    private fun showContextMenu(view: View, profile: Profile) {
        val popup = android.widget.PopupMenu(view.context, view)
        popup.menu.add("Edit")
        popup.menu.add("Delete")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edit" -> onAction(profile, ProfileAction.EDIT)
                "Delete" -> onAction(profile, ProfileAction.DELETE)
            }
            true
        }
        popup.show()
    }

    override fun getItemCount() = profiles.size
}


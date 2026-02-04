package com.multiprofile.browser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

open class WebViewActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var profileId: String? = null
    private var profileName: String? = null
    private var profileUrl: String? = null
    private var isWebViewCreated = false
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get profile data from Intent (works across processes)
        profileId = intent.getStringExtra("PROFILE_ID")
        profileName = intent.getStringExtra("PROFILE_NAME")
        profileUrl = intent.getStringExtra("PROFILE_URL")
        
        // Debug logging
        android.util.Log.d("WebViewActivity", "Received - ID: $profileId, Name: $profileName, URL: $profileUrl")
        
        if (profileId == null || profileName == null || profileUrl == null) {
            val errorMsg = "Invalid profile data - ID: $profileId, Name: $profileName, URL: $profileUrl"
            android.util.Log.e("WebViewActivity", errorMsg)
            Toast.makeText(this, "Invalid profile data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // CRITICAL: Set data directory suffix BEFORE any WebView creation
        // This MUST be called before setContentView() because the layout has a WebView
        try {
            WebView.setDataDirectorySuffix(profileId!!)
            registerMyState(profileId!!)
            isWebViewCreated = true
            android.util.Log.d("WebViewActivity", "setDataDirectorySuffix successful for: $profileId")
        } catch (e: Exception) {
            android.util.Log.e("WebViewActivity", "Failed to set data directory suffix (dirty process): ${e.message}. Killing process to restart.")
            // Instead of showing error, we kill the process. Android will restart the Activity in a fresh process.
            android.os.Process.killProcess(android.os.Process.myPid())
            return
        }

        setContentView(R.layout.activity_webview)

        val titleText = findViewById<TextView>(R.id.webViewTitle)
        val progressBar = findViewById<ProgressBar>(R.id.webViewProgress)
        val closeButton = findViewById<TextView>(R.id.btnClose)
        val backButton = findViewById<TextView>(R.id.btnBack)
        val forwardButton = findViewById<TextView>(R.id.btnForward)
        val refreshButton = findViewById<TextView>(R.id.btnRefresh)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        titleText.text = profileName

        // Mark profile as active
        val profileManager = ProfileManager(this)
        profileManager.setActiveProfile(profileId)

        // Show loading overlay initially
        loadingOverlay.visibility = View.VISIBLE

        // Delay WebView configuration slightly to avoid choppy transition
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Configure WebView (suffix already set above)
                webView = findViewById<WebView>(R.id.webView).apply {
                    visibility = View.INVISIBLE // Hide until first content loads
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }

                    // CRITICAL: Configure cookie manager to save cookies
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    cookieManager.flush()

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Hide loading overlay and show WebView after first page load
                            loadingOverlay.visibility = View.GONE
                            webView?.visibility = View.VISIBLE
                            
                            // CRITICAL: Save cookies to disk after each page load
                            android.webkit.CookieManager.getInstance().flush()
                            
                            // Update navigation button states
                            updateNavigationButtons()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progressBar.progress = newProgress
                            if (newProgress == 100) {
                                progressBar.visibility = ProgressBar.GONE
                            } else {
                                progressBar.visibility = ProgressBar.VISIBLE
                            }
                            
                            // Update tab loading spinner
                            try {
                                val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.profileSwitcherList)
                                val adapter = rv?.adapter as? ProfileSwitcherAdapter
                                adapter?.setLoadingState(newProgress < 100)
                            } catch (e: Exception) {
                                // Ignore UI update errors during init
                            }
                        }
                    }

                    loadUrl(profileUrl!!)
                }
            } catch (e: Exception) {
                android.util.Log.e("WebViewActivity", "Error configuring WebView: ${e.message}")
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }, 150) // Small delay for smooth transition


        val btnMenu = findViewById<android.widget.ImageView>(R.id.btnMenu)
        val profileSwitcherList = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.profileSwitcherList)

        // Setup Profile Switcher
        setupProfileSwitcher(profileSwitcherList)

        btnMenu.setOnClickListener {
            if (profileSwitcherList.visibility == View.VISIBLE) {
                profileSwitcherList.visibility = View.GONE
            } else {
                profileSwitcherList.visibility = View.VISIBLE
            }
        }

        closeButton.setOnClickListener {
            finish()
        }
        
        // Navigation controls
        backButton.setOnClickListener {
            if (webView?.canGoBack() == true) {
                webView?.goBack()
            }
        }
        
        forwardButton.setOnClickListener {
            if (webView?.canGoForward() == true) {
                webView?.goForward()
            }
        }
        
        refreshButton.setOnClickListener {
            webView?.reload()
        }
        
        // Update button states
        updateNavigationButtons()
        
        // Register cleanup receiver
        val filter = android.content.IntentFilter("com.multiprofile.browser.ACTION_KILL_PROFILE")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(cleanupReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(cleanupReceiver, filter)
        }
    }

    private fun setupProfileSwitcher(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        val manager = ProfileManager(this)
        val profiles = manager.getProfiles()
        
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        
        recyclerView.adapter = ProfileSwitcherAdapter(profiles, profileId) { newProfile ->
            switchProfile(newProfile)
        }
        
        // Auto-scroll to active profile
        val activeIndex = profiles.indexOfFirst { it.id == profileId }
        if (activeIndex != -1) {
            recyclerView.post {
                recyclerView.scrollToPosition(activeIndex)
            }
        }
    }

    private fun getMySlotType(): Int {
        return when (this) {
            is WebViewActivityGamma -> ProcessStateManager.SLOT_GAMMA
            is WebViewActivityBeta -> ProcessStateManager.SLOT_BETA
            else -> ProcessStateManager.SLOT_MAIN
        }
    }

    private fun registerMyState(id: String) {
        ProcessStateManager(this).updateSlotState(getMySlotType(), id)
    }

    private fun switchProfile(newProfile: Profile) {
        if (newProfile.id == profileId) return // Already on this profile

        // Smart Switch: Check if this profile is ALREADY loaded in another process
        val processManager = ProcessStateManager(this)
        val existingSlot = processManager.findSlotForProfile(newProfile.id)

        // We defer the actual switch slightly to let the UI update
        Handler(Looper.getMainLooper()).postDelayed({
            
            val targetClass: Class<*>
            
            if (existingSlot != null) {
                // INSTANT HIT: Profile is already running in a background process!
                android.util.Log.d("WebViewActivity", "SmartSwitch: Found ${newProfile.name} in slot $existingSlot")
                targetClass = when (existingSlot) {
                    ProcessStateManager.SLOT_GAMMA -> WebViewActivityGamma::class.java
                    ProcessStateManager.SLOT_BETA -> WebViewActivityBeta::class.java
                    else -> WebViewActivity::class.java
                }
            } else {
                // MISS: Profile is not loaded. We must overwrite a slot.
                // We rotate: Main -> Beta -> Gamma -> Main
                // This ensures we always keep 3 profiles (Current + 2 Others)
                targetClass = when (this) {
                    is WebViewActivityGamma -> WebViewActivity::class.java // Gamma -> Main
                    is WebViewActivityBeta -> WebViewActivityGamma::class.java // Beta -> Gamma
                    else -> WebViewActivityBeta::class.java // Main -> Beta
                }
                android.util.Log.d("WebViewActivity", "SmartSwitch: Loading new profile ${newProfile.name} into next slot")
            }
            
            val intent = Intent(this, targetClass)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("PROFILE_ID", newProfile.id)
            intent.putExtra("PROFILE_NAME", newProfile.name)
            intent.putExtra("PROFILE_URL", newProfile.url)
            
            startActivity(intent)
            overridePendingTransition(0, 0) // No animation
            
            // WE DO NOT FINISH HERE. Keeping activity alive for instant back/forward.
        }, 50)
    }

    private val cleanupReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.multiprofile.browser.ACTION_KILL_PROFILE") {
                val targetId = intent.getStringExtra("TARGET_ID")
                if (targetId == profileId) {
                    android.util.Log.d("WebViewActivity", "Received KILL signal for FAR profile: $profileId")
                    finish() // onDestroy will kill process
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        
        // Ensure loading overlay is hidden if we are returning to an already loaded tab
        if (isWebViewCreated && webView?.url != null) {
            loadingOverlay.visibility = View.GONE
        }
        
        // ENFORCE WINDOW: Keep only Previous, Current, Next (Distance <= 1)
        // Kill any profile that is "FAR"
        try {
            val allProfiles = ProfileManager(this).getProfiles()
            val currentIndex = allProfiles.indexOfFirst { it.id == profileId }
            
            if (currentIndex != -1) {
                val safeIds = mutableSetOf<String>()
                safeIds.add(profileId!!)
                // Previous
                if (currentIndex > 0) safeIds.add(allProfiles[currentIndex - 1].id)
                // Next
                if (currentIndex < allProfiles.size - 1) safeIds.add(allProfiles[currentIndex + 1].id)
                
                // Check other slots and kill if not safe
                val pm = ProcessStateManager(this)
                checkAndKill(pm.getProfileInSlot(ProcessStateManager.SLOT_MAIN), safeIds)
                checkAndKill(pm.getProfileInSlot(ProcessStateManager.SLOT_BETA), safeIds)
                checkAndKill(pm.getProfileInSlot(ProcessStateManager.SLOT_GAMMA), safeIds)
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewActivity", "Error enforcing window: ${e.message}")
        }
    }
    
    private fun checkAndKill(id: String?, safeIds: Set<String>) {
        if (id != null && !safeIds.contains(id)) {
            android.util.Log.d("WebViewActivity", "Killing FAR profile: $id")
            val intent = Intent("com.multiprofile.browser.ACTION_KILL_PROFILE")
            intent.putExtra("TARGET_ID", id)
            sendBroadcast(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        val newProfileId = intent?.getStringExtra("PROFILE_ID")
        
        // Register our new state if we are accepting this intent
        if (newProfileId != null) {
             registerMyState(newProfileId)
        }

        // If we are reused for a DIFFERENT profile, we must die and restart.
        // This is because WebView.setDataDirectorySuffix can only be called once per process.
        if (newProfileId != null && newProfileId != profileId) {
             android.util.Log.d("WebViewActivity", "Profile mismatch: Current=$profileId, New=$newProfileId. Restarting process.")
             
             // 1. Launch a new instance (the system will start a new process as we die)
             val restartIntent = Intent(intent)
             // These flags help ensure we don't just loop back to onNewIntent of the dying process
             restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
             
             startActivity(restartIntent)
             
             // 2. Kill ourselves immediately
             finish()
        }
    }
    
    private fun updateNavigationButtons() {
        val backButton = findViewById<TextView>(R.id.btnBack)
        val forwardButton = findViewById<TextView>(R.id.btnForward)
        
        backButton?.let {
            it.isEnabled = webView?.canGoBack() == true
            it.alpha = if (webView?.canGoBack() == true) 1.0f else 0.3f
        }
        
        forwardButton?.let {
            it.isEnabled = webView?.canGoForward() == true
            it.alpha = if (webView?.canGoForward() == true) 1.0f else 0.3f
        }
    }


    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            // Call finish() to properly trigger onDestroy
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
        // CRITICAL: Save cookies when activity goes to background
        android.webkit.CookieManager.getInstance().flush()
    }



    override fun onDestroy() {
        try {
            unregisterReceiver(cleanupReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }

        android.util.Log.d("WebViewActivity", "onDestroy called - cleaning up session preserved")
        
        // Clear active profile status
        try {
            val profileManager = ProfileManager(this)
            profileManager.setActiveProfile(null)
        } catch (e: Exception) {
            android.util.Log.w("WebViewActivity", "Error clearing active profile: ${e.message}")
        }
        
        // Destroy WebView WITHOUT clearing cookies/cache (preserve session)
        try {
            webView?.apply {
                stopLoading()
                // DO NOT clear cache or cookies - user stays logged in!
                removeAllViews()
                destroy()
            }
            webView = null
        } catch (e: Exception) {
            // Ignore cleanup errors
            android.util.Log.w("WebViewActivity", "Error during cleanup: ${e.message}")
        }

        super.onDestroy()
        
        // CRITICAL: Kill the WebView process completely
        // This ensures the next profile gets a fresh process and can call setDataDirectorySuffix()
        // Since this activity runs in a separate process (:webview), this only kills that process
        android.os.Process.killProcess(android.os.Process.myPid())
    }
    
    // Inner Adapter Class for Switcher
    inner class ProfileSwitcherAdapter(
        private val profiles: List<Profile>,
        private val currentProfileId: String?,
        private val onProfileClick: (Profile) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<ProfileSwitcherAdapter.ViewHolder>() {

        private var isLoadingCurrent = false

        fun setLoadingState(isLoading: Boolean) {
            if (this.isLoadingCurrent != isLoading) {
                this.isLoadingCurrent = isLoading
                // Find index of current profile
                val index = profiles.indexOfFirst { it.id == currentProfileId }
                if (index != -1) notifyItemChanged(index)
            }
        }

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val iconText: TextView = view.findViewById(R.id.profileIcon)
            val loadingProgress: ProgressBar = view.findViewById(R.id.tabLoadingProgress)
            val nameText: TextView = view.findViewById(R.id.profileName)
            val closeButton: TextView = view.findViewById(R.id.btnCloseTab)
            val container: View = view as View
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile_pill, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val profile = profiles[position]
            holder.nameText.text = profile.name
            
            // Get first letter of profile name
            val firstLetter = profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.iconText.text = firstLetter
            
            // Color coding based on profile name
            val (iconBgColor, iconTextColor) = when {
                profile.name.contains("meesho", ignoreCase = true) || 
                profile.name.contains("seller", ignoreCase = true) -> 
                    Pair(0xFFF3E8FF.toInt(), 0xFF7C3AED.toInt()) // Purple
                profile.name.contains("flipkart", ignoreCase = true) -> 
                    Pair(0xFFFED7AA.toInt(), 0xFFEA580C.toInt()) // Orange
                profile.name.contains("personal", ignoreCase = true) -> 
                    Pair(0xFFDBEAFE.toInt(), 0xFF2563EB.toInt()) // Blue
                else -> 
                    Pair(0xFFE0E7FF.toInt(), 0xFF4F46E5.toInt()) // Indigo
            }
            
            holder.iconText.backgroundTintList = android.content.res.ColorStateList.valueOf(iconBgColor)
            holder.iconText.setTextColor(iconTextColor)
            
            // Tab styling - active/inactive
            if (profile.id == currentProfileId) {
                // Active tab
                holder.container.setBackgroundResource(R.drawable.tab_active_bg)
                holder.nameText.setTextColor(0xFF000000.toInt())
                holder.closeButton.visibility = View.VISIBLE
                
                // Loading State logic
                if (isLoadingCurrent) {
                    holder.iconText.visibility = View.INVISIBLE
                    holder.loadingProgress.visibility = View.VISIBLE
                } else {
                    holder.iconText.visibility = View.VISIBLE
                    holder.loadingProgress.visibility = View.GONE
                }
                
                // Elevation for active tab
                holder.container.elevation = 4f
            } else {
                // Inactive tab
                holder.container.setBackgroundResource(R.drawable.tab_inactive_bg)
                holder.nameText.setTextColor(0xFF666666.toInt())
                holder.closeButton.visibility = View.GONE
                holder.loadingProgress.visibility = View.GONE
                holder.iconText.visibility = View.VISIBLE
                
                // Less elevation for inactive tabs
                holder.container.elevation = 0f
                holder.container.alpha = 0.7f
            }
            
            // Click listener for tab
            holder.itemView.setOnClickListener {
                if (profile.id != currentProfileId) {
                    onProfileClick(profile)
                }
            }
            
            // Close button listener (optional - currently just prevents switching)
            holder.closeButton.setOnClickListener {
                // TODO: Implement close tab functionality
                Toast.makeText(this@WebViewActivity, "Close tab: ${profile.name}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount() = profiles.size
    }

}

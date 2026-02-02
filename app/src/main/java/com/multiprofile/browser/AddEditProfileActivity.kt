package com.multiprofile.browser

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddEditProfileActivity : AppCompatActivity() {

    private lateinit var profileManager: ProfileManager
    private var editingProfileId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_profile)

        profileManager = ProfileManager(this)
        editingProfileId = intent.getStringExtra("PROFILE_ID")

        val nameInput = findViewById<EditText>(R.id.inputProfileName)
        val urlInput = findViewById<EditText>(R.id.inputProfileUrl)
        val saveButton = findViewById<Button>(R.id.btnSave)
        val cancelButton = findViewById<android.view.View>(R.id.btnCancel)

        // URL shortcut chips
        findViewById<android.view.View>(R.id.chipMeesho).setOnClickListener {
            urlInput.setText("https://supplier.meesho.com")
        }
        findViewById<android.view.View>(R.id.chipFlipkart).setOnClickListener {
            urlInput.setText("https://seller.flipkart.com")
        }
        findViewById<android.view.View>(R.id.chipAmazon).setOnClickListener {
            urlInput.setText("https://seller.amazon.in")
        }
        findViewById<android.view.View>(R.id.chipMyntra).setOnClickListener {
            urlInput.setText("https://seller.myntra.com")
        }
        findViewById<android.view.View>(R.id.chipWhatsApp).setOnClickListener {
            urlInput.setText("https://business.whatsapp.com")
        }
        findViewById<android.view.View>(R.id.chipGoogle).setOnClickListener {
            urlInput.setText("https://google.com")
        }

        // If editing, load existing profile
        editingProfileId?.let { id ->
            profileManager.getProfile(id)?.let { profile ->
                nameInput.setText(profile.name)
                urlInput.setText(profile.url)
                title = "Edit Profile"
            }
        } ?: run {
            title = "Add Profile"
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val url = urlInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Profile name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (url.isEmpty()) {
                Toast.makeText(this, "Website URL required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add http:// if no protocol specified
            val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

            editingProfileId?.let { id ->
                profileManager.updateProfile(id, name, finalUrl)
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            } ?: run {
                profileManager.addProfile(name, finalUrl)
                Toast.makeText(this, "Profile created", Toast.LENGTH_SHORT).show()
            }

            finish()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }
}

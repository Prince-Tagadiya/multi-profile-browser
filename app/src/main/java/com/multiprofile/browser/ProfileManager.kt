package com.multiprofile.browser

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProfileManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Get all profiles
    fun getProfiles(): List<Profile> {
        val json = prefs.getString("profile_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Profile>>() {}.type
        return gson.fromJson(json, type)
    }
    
    // Save all profiles
    private fun saveProfiles(profiles: List<Profile>) {
        val json = gson.toJson(profiles)
        prefs.edit().putString("profile_list", json).apply()
    }
    
    // Add new profile with auto-generated ID
    fun addProfile(name: String, url: String): Profile {
        val profiles = getProfiles().toMutableList()
        val newId = "profile_${System.currentTimeMillis()}"
        val profile = Profile(newId, name, url)
        profiles.add(profile)
        saveProfiles(profiles)
        return profile
    }
    
    // Update existing profile
    fun updateProfile(profileId: String, newName: String, newUrl: String) {
        val profiles = getProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profileId }
        if (index != -1) {
            profiles[index] = Profile(profileId, newName, newUrl)
            saveProfiles(profiles)
        }
    }
    
    // Delete profile
    fun deleteProfile(profileId: String) {
        val profiles = getProfiles().toMutableList()
        profiles.removeIf { it.id == profileId }
        saveProfiles(profiles)
    }
    
    // Get single profile by ID
    fun getProfile(profileId: String): Profile? {
        return getProfiles().find { it.id == profileId }
    }
    
    // Track active profile
    fun setActiveProfile(profileId: String?) {
        prefs.edit().putString("active_profile", profileId).apply()
    }
    
    fun getActiveProfile(): String? {
        return prefs.getString("active_profile", null)
    }
}

data class Profile(
    val id: String,
    val name: String,
    val url: String
)

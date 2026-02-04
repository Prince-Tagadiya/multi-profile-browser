package com.multiprofile.browser

import android.content.Context
import android.content.SharedPreferences

class ProcessStateManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("process_state", Context.MODE_PRIVATE)

    companion object {
        const val SLOT_MAIN = 0
        const val SLOT_BETA = 1
        const val SLOT_GAMMA = 2
        const val SLOT_DELTA = 3
        
        private const val KEY_MAIN_PROFILE = "slot_main_profile"
        private const val KEY_BETA_PROFILE = "slot_beta_profile"
        private const val KEY_GAMMA_PROFILE = "slot_gamma_profile"
        private const val KEY_DELTA_PROFILE = "slot_delta_profile"
    }

    fun updateSlotState(slotType: Int, profileId: String) {
        val key = when(slotType) {
            SLOT_MAIN -> KEY_MAIN_PROFILE
            SLOT_BETA -> KEY_BETA_PROFILE
            SLOT_GAMMA -> KEY_GAMMA_PROFILE
            SLOT_DELTA -> KEY_DELTA_PROFILE
            else -> return
        }
        prefs.edit().putString(key, profileId).apply()
    }

    fun findSlotForProfile(profileId: String): Int? {
        if (prefs.getString(KEY_MAIN_PROFILE, null) == profileId) return SLOT_MAIN
        if (prefs.getString(KEY_BETA_PROFILE, null) == profileId) return SLOT_BETA
        if (prefs.getString(KEY_GAMMA_PROFILE, null) == profileId) return SLOT_GAMMA
        if (prefs.getString(KEY_DELTA_PROFILE, null) == profileId) return SLOT_DELTA
        return null
    }

    fun getProfileInSlot(slotType: Int): String? {
        return when(slotType) {
            SLOT_MAIN -> prefs.getString(KEY_MAIN_PROFILE, null)
            SLOT_BETA -> prefs.getString(KEY_BETA_PROFILE, null)
            SLOT_GAMMA -> prefs.getString(KEY_GAMMA_PROFILE, null)
            SLOT_DELTA -> prefs.getString(KEY_DELTA_PROFILE, null)
            else -> null
        }
    }
}

package com.cameronbatten.dadmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Decides whether the user should see onboarding or the main navigator on
 * launch. [state] is [AppStartState.Unknown] until the first DataStore read
 * resolves, then flips to [AppStartState.Onboarding] or [AppStartState.Ready].
 */
enum class AppStartState { Unknown, Onboarding, Ready }

@HiltViewModel
class AppStartViewModel @Inject constructor(
    userPreferences: UserPreferences,
) : ViewModel() {

    val state: StateFlow<AppStartState> = userPreferences.onboardingCompleted
        .map { if (it) AppStartState.Ready else AppStartState.Onboarding }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppStartState.Unknown)
}

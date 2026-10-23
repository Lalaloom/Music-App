package com.cameronbatten.dadmusic.ui.pedal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cameronbatten.dadmusic.pedal.PedalAction
import com.cameronbatten.dadmusic.pedal.PedalBinding
import com.cameronbatten.dadmusic.pedal.PedalBindings
import com.cameronbatten.dadmusic.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PedalBindingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    val bindings: StateFlow<List<PedalBinding>> = prefs.pedalBindingsText
        .map { PedalBindings.decode(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun bind(action: PedalAction, keyCode: Int) = viewModelScope.launch {
        // Remove any existing binding on the same key OR the same action.
        val next = bindings.value
            .filterNot { it.keyCode == keyCode || it.action == action }
            .toMutableList()
        next += PedalBinding(keyCode, action)
        prefs.setPedalBindingsText(PedalBindings.encode(next))
    }

    fun clear(action: PedalAction) = viewModelScope.launch {
        val snap = bindings.value.filterNot { it.action == action }
        prefs.setPedalBindingsText(PedalBindings.encode(snap))
    }
}

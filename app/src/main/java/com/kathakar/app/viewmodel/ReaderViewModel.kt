package com.kathakar.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kathakar.app.domain.model.Episode
import com.kathakar.app.repository.StoryRepository
import com.kathakar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val storyRepo: StoryRepository
) : ViewModel() {

    private val _episode = MutableStateFlow<Episode?>(null)
    val episode: StateFlow<Episode?> = _episode.asStateFlow()

    fun load(episodeId: String) = viewModelScope.launch {
        _episode.value = (storyRepo.getEpisode(episodeId) as? Resource.Success)?.data
    }
}

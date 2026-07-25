package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.paging.compose.collectAsLazyPagingItems
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.viewModel.RecentlySongsViewModel
import com.maxrave.simpmusic.viewModel.SearchScreenUIState
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.search
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamAddSongBottomSheet(
    onDismissRequest: () -> Unit,
    jamViewModel: JamViewModel,
    recentlySongsViewModel: RecentlySongsViewModel = koinViewModel(),
    searchViewModel: SearchViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    
    val recentlyItems = recentlySongsViewModel.recentlySongs.collectAsLazyPagingItems()
    val searchState by searchViewModel.searchScreenState.collectAsState()
    val searchUIState by searchViewModel.searchScreenUIState.collectAsState()

    // Trigger search after debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(500) // Debounce 500ms
            searchViewModel.searchSongs(searchQuery)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (searchQuery.isBlank()) {
                Text("Recently Played", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                    items(
                        count = recentlyItems.itemCount,
                    ) { index ->
                        val item = recentlyItems[index]
                        if (item is SongEntity) {
                                SongFullWidthItems(
                                    songEntity = item,
                                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                                    isPlaying = false,
                                    onClickListener = {
                                        jamViewModel.playNow(item.videoId, item.title, item.artistsName, item.thumbnailUrl, item.durationSeconds.toLong() * 1000L)
                                        onDismissRequest()
                                    },
                                    onAddToQueue = {
                                        jamViewModel.addToQueue(item.videoId, item.title, item.artistsName, item.thumbnailUrl, item.durationSeconds.toLong() * 1000L)
                                        sharedViewModel.makeToast("Song added to Jam queue")
                                    }
                                )
                        }
                    }
                }
            } else {
                Text("Search Results", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (searchUIState == SearchScreenUIState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        items(searchState.searchSongsResult) { song ->
                            SongFullWidthItems(
                                track = song.toTrack(),
                                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                                isPlaying = false,
                                onClickListener = {
                                    jamViewModel.playNow(song.videoId, song.title ?: "", song.artists?.joinToString(", ") { it.name } ?: "", song.thumbnails?.lastOrNull()?.url, (song.durationSeconds?.toLong() ?: 0L) * 1000L)
                                    onDismissRequest()
                                },
                                onAddToQueue = {
                                    jamViewModel.addToQueue(song.videoId, song.title ?: "", song.artists?.joinToString(", ") { it.name } ?: "", song.thumbnails?.lastOrNull()?.url, (song.durationSeconds?.toLong() ?: 0L) * 1000L)
                                    sharedViewModel.makeToast("Song added to Jam queue")
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.toArrayListTrack
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.FourImagesMosaic
import com.maxrave.simpmusic.ui.component.ImageData
import com.maxrave.simpmusic.ui.component.playlistTitleGradient
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistFullWidthItems
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.PlayCircle
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.screen.home.analytics.monthFullName
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AnalyticsViewModel
import com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album_length
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.followed
import simpmusic.composeapp.generated.resources.lower_plays
import simpmusic.composeapp.generated.resources.most_played
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.seconds
import simpmusic.composeapp.generated.resources.wrapped
import simpmusic.composeapp.generated.resources.wrapped_recap_month
import simpmusic.composeapp.generated.resources.wrapped_recap_month_year
import simpmusic.composeapp.generated.resources.wrapped_recap_subtitle
import simpmusic.composeapp.generated.resources.your_recently_played
import simpmusic.composeapp.generated.resources.your_top_albums
import simpmusic.composeapp.generated.resources.your_top_artists
import simpmusic.composeapp.generated.resources.your_top_tracks

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
@ExperimentalMaterial3Api
fun LibraryDynamicPlaylistScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    type: String,
    viewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
    analyticsViewModel: AnalyticsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val nowPlayingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    var chosenSong: SongEntity? by remember { mutableStateOf(null) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }

    val favorite by viewModel.listFavoriteSong.collectAsStateWithLifecycle()
    var tempFavorite by remember { mutableStateOf(emptyList<SongEntity>()) }
    val followed by viewModel.listFollowedArtist.collectAsStateWithLifecycle()
    var tempFollowed by remember { mutableStateOf(emptyList<ArtistEntity>()) }
    val mostPlayed by viewModel.listMostPlayedSong.collectAsStateWithLifecycle()
    var tempMostPlayed by remember { mutableStateOf(emptyList<SongEntity>()) }
    val downloaded by viewModel.listDownloadedSong.collectAsStateWithLifecycle()
    var tempDownloaded by remember { mutableStateOf(emptyList<SongEntity>()) }
    val monthlyRecap by viewModel.listMonthlyRecapSong.collectAsStateWithLifecycle()
    var tempMonthlyRecap by remember { mutableStateOf(emptyList<SongEntity>()) }
    val recentlyPlayed by viewModel.listRecentlyPlayedSong.collectAsStateWithLifecycle()
    var tempRecentlyPlayed by remember { mutableStateOf(emptyList<SongEntity>()) }
    val analyticsUIState by analyticsViewModel.analyticsUIState.collectAsStateWithLifecycle()
    var tempTopTracks by remember { mutableStateOf(analyticsUIState.topTracks.data ?: emptyList()) }
    var tempTopArtists by remember { mutableStateOf(analyticsUIState.topArtists.data ?: emptyList()) }
    var tempTopAlbums by remember { mutableStateOf(analyticsUIState.topAlbums.data ?: emptyList()) }
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    // The other lists are observed from the database and are already loaded by the time this
    // screen opens; a recap is one month's ranking, so it can only be fetched once the route says
    // which month. Keyed on the route argument rather than the parsed type so re-entering the same
    // month does not re-query.
    LaunchedEffect(type) {
        (LibraryDynamicPlaylistType.toType(type) as? LibraryDynamicPlaylistType.MonthlyRecap)
            ?.let { viewModel.getMonthlyRecapSong(it) }
    }

    LaunchedEffect(query) {
        tempFavorite = favorite.filter { it.matches(query) }
        tempFollowed = followed.filter { it.name.contains(query, ignoreCase = true) }
        tempMostPlayed = mostPlayed.filter { it.matches(query) }
        tempDownloaded = downloaded.filter { it.matches(query) }
        tempRecentlyPlayed = recentlyPlayed.filter { it.matches(query) }
        tempMonthlyRecap = monthlyRecap.filter { it.matches(query) }
        tempTopTracks =
            analyticsUIState.topTracks.data?.filter {
                it.second.matches(query)
            } ?: emptyList()
        tempTopArtists =
            analyticsUIState.topArtists.data
                ?.filter { it.second.name.contains(query, ignoreCase = true) }
                ?: emptyList()
        tempTopAlbums =
            analyticsUIState.topAlbums.data
                ?.filter { matchesQuery(it.second.title, it.second.artistName, query) }
                ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.hazeSource(hazeState),
        contentPadding = innerPadding,
    ) {
        val typeObj = LibraryDynamicPlaylistType.toType(type)
        val isSongType = typeObj != LibraryDynamicPlaylistType.Followed && typeObj != LibraryDynamicPlaylistType.TopArtists && typeObj != LibraryDynamicPlaylistType.TopAlbums
        item(contentType = "header") {
            val subtitle = when (typeObj) {
                LibraryDynamicPlaylistType.Favorite -> stringResource(Res.string.album_length, favorite.size.toString(), "")
                LibraryDynamicPlaylistType.MostPlayed -> stringResource(Res.string.album_length, mostPlayed.size.toString(), "")
                LibraryDynamicPlaylistType.RecentlyPlayed -> stringResource(Res.string.album_length, recentlyPlayed.size.toString(), "")
                LibraryDynamicPlaylistType.Downloaded -> stringResource(Res.string.album_length, downloaded.size.toString(), "")
                LibraryDynamicPlaylistType.Followed -> "${followed.size} ${stringResource(Res.string.artists)}"
                is LibraryDynamicPlaylistType.MonthlyRecap -> stringResource(Res.string.wrapped_recap_subtitle)
                else -> null
            }
            val images = when (typeObj) {
                LibraryDynamicPlaylistType.Favorite -> tempFavorite.take(4).map { ImageData(it.thumbnails ?: "", it.title, it.artistName?.joinToString(", ") ?: "", onClick = {}) }
                LibraryDynamicPlaylistType.RecentlyPlayed -> tempRecentlyPlayed.take(4).map { ImageData(it.thumbnails ?: "", it.title, it.artistName?.joinToString(", ") ?: "", onClick = {}) }
                LibraryDynamicPlaylistType.MostPlayed -> tempMostPlayed.take(4).map { ImageData(it.thumbnails ?: "", it.title, it.artistName?.joinToString(", ") ?: "", onClick = {}) }
                LibraryDynamicPlaylistType.Downloaded -> tempDownloaded.take(4).map { ImageData(it.thumbnails ?: "", it.title, it.artistName?.joinToString(", ") ?: "", onClick = {}) }
                LibraryDynamicPlaylistType.TopTracks -> tempTopTracks.take(4).map { ImageData(it.second.thumbnails ?: "", it.second.title, it.second.artistName?.joinToString(", ") ?: "", onClick = {}) }
                else -> emptyList()
            }
            LibraryDynamicPlaylistHeader(
                type = typeObj,
                subtitle = subtitle,
                images = images,
                isSongType = isSongType,
                showSearchBar = showSearchBar,
                onShowSearchBarChange = { showSearchBar = it },
                onPlayAll = {
                    if (typeObj == LibraryDynamicPlaylistType.TopTracks) {
                        val data = analyticsUIState.topTracks.data
                        if (!data.isNullOrEmpty()) {
                            val first = data.first().second
                            sharedViewModel.setQueueData(QueueData.Data(data.map { it.second }.toArrayListTrack(), first.toTrack(), null, "Your Top Tracks", PlaylistType.RADIO, null))
                            sharedViewModel.loadMediaItem(first.toTrack(), Config.PLAYLIST_CLICK, 0)
                        }
                    } else { viewModel.playAll(typeObj) }
                },
                onShuffle = {
                    if (typeObj == LibraryDynamicPlaylistType.TopTracks) {
                        val data = analyticsUIState.topTracks.data
                        if (!data.isNullOrEmpty()) {
                            val shuffled = data.shuffled()
                            val first = shuffled.first().second
                            sharedViewModel.setQueueData(QueueData.Data(shuffled.map { it.second }.toArrayListTrack(), first.toTrack(), null, "Your Top Tracks", PlaylistType.RADIO, null))
                            sharedViewModel.loadMediaItem(first.toTrack(), Config.PLAYLIST_CLICK, 0)
                        }
                    } else { viewModel.shuffle(typeObj) }
                },
                navController = navController
            )
        }
        item {
            AnimatedVisibility(showSearchBar) {
                Spacer(Modifier.height(55.dp))
            }
        }
        val type = LibraryDynamicPlaylistType.toType(type)
        if (type == LibraryDynamicPlaylistType.Followed) {
            items(
                if (query.isNotEmpty() && showSearchBar) {
                    tempFollowed
                } else {
                    followed
                },
                key = { it.channelId },
            ) { artist ->
                ArtistFullWidthItems(
                    artist,
                    onClickListener = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = artist.channelId,
                            ),
                        )
                    },
                )
            }
        } else if (type == LibraryDynamicPlaylistType.TopArtists) {
            when (analyticsUIState.topArtists) {
                is LocalResource.Success if (!analyticsUIState.topArtists.data.isNullOrEmpty()) -> {
                    val data = analyticsUIState.topArtists.data ?: emptyList()
                    items(
                        if (query.isNotEmpty() && showSearchBar) {
                            tempTopArtists
                        } else {
                            data
                        },
                        key = { it.first.hashCode() },
                    ) { artist ->
                        ArtistFullWidthItems(
                            artist.second,
                            rightView = {
                                Box(Modifier.padding(horizontal = 8.dp)) {
                                    Text(
                                        text = "${artist.first.playCount} ${stringResource(Res.string.lower_plays)}",
                                        style = typo().bodySmall,
                                    )
                                }
                            },
                            onClickListener = {
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = artist.second.channelId,
                                    ),
                                )
                            },
                        )
                    }
                }

                else -> {}
            }
        } else if (type == LibraryDynamicPlaylistType.TopAlbums) {
            when (analyticsUIState.topAlbums) {
                is LocalResource.Success if (!analyticsUIState.topAlbums.data.isNullOrEmpty()) -> {
                    val data = analyticsUIState.topAlbums.data ?: emptyList()
                    items(
                        if (query.isNotEmpty() && showSearchBar) {
                            tempTopAlbums
                        } else {
                            data
                        },
                        key = { it.first.hashCode() },
                    ) { album ->
                        PlaylistFullWidthItems(
                            album.second,
                            rightView = {
                                Box(Modifier.padding(horizontal = 8.dp)) {
                                    Text(
                                        text = "${album.first.playCount} ${stringResource(Res.string.lower_plays)}",
                                        style = typo().bodySmall,
                                    )
                                }
                            },
                            onClickListener = {
                                navController.navigate(
                                    AlbumDestination(
                                        browseId = album.second.browseId,
                                    ),
                                )
                            },
                        )
                    }
                }

                else -> {}
            }
        } else if (type == LibraryDynamicPlaylistType.TopTracks) {
            when (analyticsUIState.topTracks) {
                is LocalResource.Success if (!analyticsUIState.topTracks.data.isNullOrEmpty()) -> {
                    val data = analyticsUIState.topTracks.data ?: emptyList()
                    items(
                        if (query.isNotEmpty() && showSearchBar) {
                            tempTopTracks
                        } else {
                            data
                        },
                        key = { it.hashCode() },
                    ) { song ->
                        SongFullWidthItems(
                            songEntity = song.second,
                            isPlaying = song.second.videoId == nowPlayingVideoId,
                            modifier = Modifier.fillMaxWidth(),
                            onMoreClickListener = {
                                chosenSong = song.second
                                showBottomSheet = true
                            },
                            onClickListener = { videoId ->
                                val targetList = data.map { it.second }
                                val playTrack = song.second
                                with(sharedViewModel) {
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = targetList.toArrayListTrack(),
                                            firstPlayedTrack = playTrack.toTrack(),
                                            playlistId = null,
                                            playlistName = getStringBlocking(Res.string.your_top_tracks),
                                            playlistType = PlaylistType.RADIO,
                                            continuation = null,
                                        ),
                                    )
                                    loadMediaItem(
                                        playTrack.toTrack(),
                                        Config.PLAYLIST_CLICK,
                                        targetList.indexOf(playTrack).coerceAtLeast(0),
                                    )
                                }
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(song.second.toTrack()),
                                )
                            },
                            selectionMode = selectionState.isActive,
                            isSelected = selectionState.isSelected(song.second.videoId),
                            onLongClick = { selectionState.start(it) },
                            onSelectToggle = { selectionState.toggle(it) },
                            rightView = {
                                Column(
                                    modifier = Modifier.wrapContentWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "${song.first.totalListeningTime} ${stringResource(Res.string.seconds)}",
                                        style = typo().bodySmall,
                                    )
                                    Text(
                                        text = "${song.first.playCount} ${stringResource(Res.string.lower_plays)}",
                                        style = typo().bodySmall,
                                    )
                                }
                            },
                        )
                    }
                }

                else -> {}
            }
        } else {
            items(
                when (type) {
                    LibraryDynamicPlaylistType.Downloaded -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempDownloaded
                        } else {
                            downloaded
                        }
                    }

                    LibraryDynamicPlaylistType.Favorite -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempFavorite
                        } else {
                            favorite
                        }
                    }

                    LibraryDynamicPlaylistType.MostPlayed -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempMostPlayed
                        } else {
                            mostPlayed
                        }
                    }

                    LibraryDynamicPlaylistType.RecentlyPlayed -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempRecentlyPlayed
                        } else {
                            recentlyPlayed
                        }
                    }

                    // Kept as an explicit branch rather than an `else`: the three cases above are
                    // reachable only because the `if` chain around this block has already ruled
                    // out every other object, and an `else` here would silently swallow the next
                    // case someone adds to the sealed class.
                    is LibraryDynamicPlaylistType.MonthlyRecap -> {
                        if (query.isNotEmpty() && showSearchBar) {
                            tempMonthlyRecap
                        } else {
                            monthlyRecap
                        }
                    }
                },
                key = { it.hashCode() },
            ) { song ->
                SongFullWidthItems(
                    songEntity = song,
                    isPlaying = song.videoId == nowPlayingVideoId,
                    modifier = Modifier.fillMaxWidth(),
                    onMoreClickListener = {
                        chosenSong = song
                        showBottomSheet = true
                    },
                    onClickListener = { videoId ->
                        viewModel.playSong(videoId, type = type)
                    },
                    onAddToQueue = {
                        sharedViewModel.addListToQueue(
                            arrayListOf(song.toTrack()),
                        )
                    },
                    selectionMode = selectionState.isActive,
                    isSelected = selectionState.isSelected(song.videoId),
                    onLongClick = { selectionState.start(it) },
                    onSelectToggle = { selectionState.toggle(it) },
                )
            }
        }
        item {
            EndOfPage()
        }
    }
    if (showSelectionSheet) {
        val selectedIds = selectionState.selected.toList()
        SelectedSongsBottomSheet(
            count = selectedIds.size,
            onDismiss = { showSelectionSheet = false },
            onPlayNext = {
                selectionViewModel.playNext(selectedIds)
                selectionState.exit()
            },
            onAddToQueue = {
                selectionViewModel.addToQueue(selectedIds)
                selectionState.exit()
            },
            onAddToPlaylist = { showSelectionAddToPlaylist = true },
            onDownload = {
                selectionViewModel.download(selectedIds)
                selectionState.exit()
            },
            onAddToFavorite = {
                selectionViewModel.addToFavorite(selectedIds)
                selectionState.exit()
            },
        )
    }
    if (showSelectionAddToPlaylist) {
        val selectedIds = selectionState.selected.toList()
        val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = localPlaylists,
            listYouTubePlaylist = emptyList(),
            onDismiss = { showSelectionAddToPlaylist = false },
            onClick = { playlist ->
                selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                selectionState.exit()
            },
            onYTPlaylistClick = {},
        )
    }
    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                chosenSong = null
            },
            navController = navController,
            song = chosenSong ?: return,
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            // Drawn inside the overlay Box, with an opaque colour, so it covers the normal bar
            if (selectionState.isActive) {
                SongSelectionTopAppBar(
                    state = selectionState,
                    onSelectAll = {
                        val visible =
                            when (LibraryDynamicPlaylistType.toType(type)) {
                                LibraryDynamicPlaylistType.TopTracks -> {
                                    val list = if (query.isNotEmpty() && showSearchBar) {
                                        tempTopTracks
                                    } else {
                                        analyticsUIState.topTracks.data ?: emptyList()
                                    }
                                    list.map { it.second.videoId }
                                }

                                LibraryDynamicPlaylistType.Downloaded -> {
                                    val list = if (query.isNotEmpty() && showSearchBar) tempDownloaded else downloaded
                                    list.map { it.videoId }
                                }

                                LibraryDynamicPlaylistType.Favorite -> {
                                    val list = if (query.isNotEmpty() && showSearchBar) tempFavorite else favorite
                                    list.map { it.videoId }
                                }

                                LibraryDynamicPlaylistType.MostPlayed -> {
                                    val list = if (query.isNotEmpty() && showSearchBar) tempMostPlayed else mostPlayed
                                    list.map { it.videoId }
                                }

                                LibraryDynamicPlaylistType.RecentlyPlayed -> {
                                    val list = if (query.isNotEmpty() && showSearchBar) tempRecentlyPlayed else recentlyPlayed
                                    list.map { it.videoId }
                                }

                                else -> emptyList()
                            }
                        selectionState.toggleSelectAll(visible)
                    },
                    onOpenActions = { showSelectionSheet = true },
                    containerColor = Color.Black,
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showSearchBar,
            enter = SearchBarEnter,
            exit = SearchBarExit,
        ) {
            SearchBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(horizontal = 12.dp),
                inputField = {
                    CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { showSearchBar = false },
                            expanded = showSearchBar,
                            onExpandedChange = { showSearchBar = it },
                            placeholder = {
                                Text(
                                    stringResource(Res.string.search),
                                    style = typo().bodySmall,
                                )
                            },
                            leadingIcon = { Icon(SimpIcons.Search, contentDescription = null) },
                        )
                    }
                },
                expanded = false,
                onExpandedChange = {},
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
            }
        }
    }
}

sealed class LibraryDynamicPlaylistType {
    data object Favorite : LibraryDynamicPlaylistType()

    data object Followed : LibraryDynamicPlaylistType()

    data object MostPlayed : LibraryDynamicPlaylistType()

    data object RecentlyPlayed : LibraryDynamicPlaylistType()

    data object Downloaded : LibraryDynamicPlaylistType()

    data object TopTracks : LibraryDynamicPlaylistType()

    data object TopArtists : LibraryDynamicPlaylistType()

    data object TopAlbums : LibraryDynamicPlaylistType()

    /**
     * One calendar month's top songs — the Wrapped tab's "Recap January".
     *
     * The only case carrying data, which is the whole reason [title] exists: every other entry
     * names itself with a fixed [StringResource], and a month cannot.
     *
     * [month] is 1..12, matching `kotlinx.datetime.Month.number`, so the round trip stays readable
     * in a URL and needs no enum lookup to serialise.
     */
    data class MonthlyRecap(
        val year: Int,
        val month: Int,
    ) : LibraryDynamicPlaylistType()

    /**
     * The fixed name, for the callers that resolve a resource without composition.
     *
     * [MonthlyRecap] has no honest answer here — its name is assembled from a month and sometimes
     * a year — so it falls back to the tab it belongs to. Nothing renders that fallback as a
     * heading: the screen prints [title], which knows the month. It exists because the `when` must
     * cover the sealed hierarchy, and because
     * [com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel] still needs *some* answer
     * on the path where it names a queue — that view model resolves the recap's real name itself.
     */
    fun name(): StringResource =
        when (this) {
            Favorite -> Res.string.favorite
            Followed -> Res.string.followed
            MostPlayed -> Res.string.most_played
            RecentlyPlayed -> Res.string.your_recently_played
            Downloaded -> Res.string.downloaded
            TopAlbums -> Res.string.your_top_albums
            TopArtists -> Res.string.your_top_artists
            TopTracks -> Res.string.your_top_tracks
            is MonthlyRecap -> Res.string.wrapped
        }

    /**
     * What the header actually shows.
     *
     * A `@Composable` rather than a [StringResource] because "Recap January" is a format string
     * plus an argument, and a resource reference cannot carry the argument. The year is printed
     * only when it is not the current one — two Januaries a year apart would otherwise be the same
     * playlist as far as the reader can tell.
     */
    @Composable
    fun title(): String =
        when (this) {
            is MonthlyRecap ->
                if (year == now().date.year) {
                    stringResource(Res.string.wrapped_recap_month, monthFullName(Month(month)))
                } else {
                    stringResource(
                        Res.string.wrapped_recap_month_year,
                        monthFullName(Month(month)),
                        year.toString(),
                    )
                }

            else -> stringResource(name())
        }

    // For serialization and navigation
    fun toStringParams(): String =
        when (this) {
            Favorite -> "favorite"
            Followed -> "followed"
            MostPlayed -> "most_played"
            RecentlyPlayed -> "recently_played"
            Downloaded -> "downloaded"
            TopAlbums -> "top_albums"
            TopArtists -> "top_artists"
            TopTracks -> "top_tracks"
            // Zero-padded so the strings sort the way the months do, which makes a list of these
            // readable in a log or a deep link without parsing it back.
            is MonthlyRecap -> "${RECAP_PREFIX}${year}_${month.toString().padStart(2, '0')}"
        }

    companion object {
        private const val RECAP_PREFIX = "recap_"

        fun toType(input: String): LibraryDynamicPlaylistType =
            when (input) {
                "favorite" -> Favorite
                "followed" -> Followed
                "most_played" -> MostPlayed
                "recently_played" -> RecentlyPlayed
                "downloaded" -> Downloaded
                "top_albums" -> TopAlbums
                "top_artists" -> TopArtists
                "top_tracks" -> TopTracks
                else -> parseMonthlyRecap(input) ?: throw IllegalArgumentException("Unknown type: $input")
            }

        /**
         * `recap_2026_01` back into a [MonthlyRecap], or null for anything else.
         *
         * Validated rather than trusted: this arrives from a persisted navigation argument and,
         * through the `simpmusic://library?type=` deep link, from outside the app entirely. A month
         * of 0 or 13 would reach `Month(month)` and throw somewhere far away from here.
         */
        private fun parseMonthlyRecap(input: String): MonthlyRecap? {
            if (!input.startsWith(RECAP_PREFIX)) return null
            val parts = input.removePrefix(RECAP_PREFIX).split("_")
            if (parts.size != 2) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
            return MonthlyRecap(year = year, month = month)
        }
    }
}

/**
 * Whether a title or any of its artists contains [query].
 *
 * Artist names count as well as the title: people look for "everything by X" at least as often as
 * for one track, and typing an artist into a search box that only reads titles looks broken.
 *
 * [artists] is a converted `List<String>`, so every entry is checked — a featured artist matches
 * as readily as the lead.
 */
private fun matchesQuery(
    title: String,
    artists: List<String>?,
    query: String,
): Boolean =
    title.contains(query, ignoreCase = true) ||
        artists?.any { it.contains(query, ignoreCase = true) } == true

private fun SongEntity.matches(query: String): Boolean = matchesQuery(title, artistName, query)

@Composable
fun LibraryDynamicPlaylistHeader(
    type: LibraryDynamicPlaylistType,
    subtitle: String?,
    images: List<ImageData>,
    isSongType: Boolean,
    showSearchBar: Boolean,
    onShowSearchBarChange: (Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    navController: NavController
) {
    val screenInfo = getScreenSizeInfo()
    val isPortrait = screenInfo.wDP < screenInfo.hDP
    val titleText = type.title()
    val mutedPaletteBg = run {
        val titleColors = playlistTitleGradient(titleText)
        val base = if (titleColors.size >= 2) {
            androidx.compose.ui.graphics.lerp(titleColors[0], titleColors[1], 0.5f)
        } else {
            titleColors.firstOrNull() ?: Color.Black
        }
        androidx.compose.ui.graphics.lerp(base, Color.Black, 0.3f)
    }

    if (isPortrait) {
        val headerBackdrop = rememberBackdrop(mutedPaletteBg)
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().layerBackdrop(headerBackdrop)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassIconButton(
                        backdrop = headerBackdrop,
                        imageVector = SimpIcons.ArrowBackIosNew,
                        modifier = Modifier.size(48.dp),
                        onClick = { navController.navigateUp() }
                    )
                    Spacer(Modifier.weight(1f))
                    LiquidGlassIconButton(
                        backdrop = headerBackdrop,
                        imageVector = if (showSearchBar) SimpIcons.Close else SimpIcons.Search,
                        modifier = Modifier.size(48.dp),
                        onClick = { onShowSearchBarChange(!showSearchBar) }
                    )
                }
                Box(
                    modifier = Modifier.size(250.dp).clip(RoundedCornerShape(8.dp))
                ) {
                    FourImagesMosaic(images = images)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = titleText, style = typo().headlineMedium, color = Color.White, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                if (subtitle != null) {
                    Text(text = subtitle, style = typo().bodyMedium, color = Color(0xC4FFFFFF), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (isSongType) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                                .clickable { onShuffle() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = SimpIcons.Shuffle, contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Box(
                            modifier = Modifier.height(56.dp).widthIn(min = 140.dp).clip(CircleShape).background(Color.White)
                                .clickable { onPlayAll() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = SimpIcons.PlayCircle, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(24.dp))
                                Text(text = "Play", style = typo().labelLarge, color = Color.Black)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    } else {
        val headerBackdrop = rememberBackdrop(mutedPaletteBg)
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().layerBackdrop(headerBackdrop)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.size(280.dp).clip(RoundedCornerShape(8.dp))) {
                        FourImagesMosaic(images = images)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = titleText, style = typo().headlineSmall, color = Color.White, maxLines = 2)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (subtitle != null) {
                            Text(text = subtitle, style = typo().bodyMedium, color = Color(0xC4FFFFFF))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        if (isSongType) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                                        .clickable { onShuffle() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(imageVector = SimpIcons.Shuffle, contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Box(
                                    modifier = Modifier.height(48.dp).widthIn(min = 110.dp).clip(CircleShape).background(Color.White)
                                        .clickable { onPlayAll() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(imageVector = SimpIcons.PlayCircle, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(20.dp))
                                        Text(text = "Play", style = typo().labelLarge, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LiquidGlassIconButton(
                    backdrop = headerBackdrop,
                    imageVector = SimpIcons.ArrowBackIosNew,
                    modifier = Modifier.size(48.dp),
                    onClick = { navController.navigateUp() }
                )
                LiquidGlassIconButton(
                    backdrop = headerBackdrop,
                    imageVector = if (showSearchBar) SimpIcons.Close else SimpIcons.Search,
                    modifier = Modifier.size(48.dp),
                    onClick = { onShowSearchBarChange(!showSearchBar) }
                )
            }
        }
    }
}

package me.aikovdp.dionysus.ui.screens.movie

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.aikovdp.dionysus.R
import me.aikovdp.dionysus.data.DiaryRepository
import me.aikovdp.dionysus.data.MovieDetails
import me.aikovdp.dionysus.data.MovieRepository
import me.aikovdp.dionysus.data.WatchlistRepository
import me.aikovdp.dionysus.ui.DionysusDestinationArgs
import me.aikovdp.dionysus.util.Async
import me.aikovdp.dionysus.util.WhileUiSubscribed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class MovieDetailUiState(
    val movie: MovieDetails? = null,
    val isInWatchlist: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: Int? = null
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    movieRepository: MovieRepository,
    private val watchlistRepository: WatchlistRepository,
    private val diaryRepository: DiaryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val movieIdArg: String = savedStateHandle[DionysusDestinationArgs.MOVIE_ID_ARG]!!
    private val movieId = movieIdArg.toInt()

    private val _userMessage: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val _isLoading = MutableStateFlow(false)
    private val _movieAsync = movieRepository.getMovieStream(movieId)
        .map { handleMovie(it) }
        .catch {
            emit(Async.Error(R.string.loading_movie_error))
            Log.e("MovieDetailViewModel", "Issue getting movie details", it)
        }
    private val _isInWatchlistAsync = watchlistRepository.containsMovieStream(movieId)
        .map { handleBoolean(it) }
        .catch {
            emit(Async.Error(R.string.loading_movie_error))
            Log.e("MovieDetailViewModel", "Issue getting watchlist state", it)
        }

    val uiState: StateFlow<MovieDetailUiState> = combine(
        _userMessage, _isLoading, _movieAsync, _isInWatchlistAsync
    ) { userMessage, isLoading, movieAsync, isInWatchlistAsync ->
        when (movieAsync) {
            Async.Loading -> {
                MovieDetailUiState(isLoading = true)
            }

            is Async.Error -> {
                MovieDetailUiState(
                    userMessage = movieAsync.errorMessage
                )
            }

            is Async.Success -> {
                MovieDetailUiState(
                    movie = movieAsync.data,
                    isInWatchlist = (isInWatchlistAsync is Async.Success) && isInWatchlistAsync.data,
                    isLoading = isLoading,
                    userMessage = userMessage
                )
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = WhileUiSubscribed,
            initialValue = MovieDetailUiState(isLoading = true)
        )

    private fun handleMovie(movie: MovieDetails?): Async<MovieDetails?> {
        if (movie == null) {
            return Async.Error(R.string.movie_not_found)
        }
        return Async.Success(movie)
    }

    private fun handleBoolean(bool: Boolean): Async<Boolean> {
        return Async.Success(bool)
    }

    fun toggleInWatchlist() {
        viewModelScope.launch {
            if (uiState.value.isInWatchlist) {
                watchlistRepository.removeEntry(movieId)
            } else {
                watchlistRepository.createEntry(movieId)
            }
        }
    }

    fun addToWatchlist(selectedDateMillis: Long?) {
        val selectedDate = LocalDate.ofInstant(
            Instant.ofEpochMilli(selectedDateMillis ?: throw IllegalArgumentException()),
            ZoneId.of("UTC")

        )
        viewModelScope.launch {
            diaryRepository.createEntry(movieId, selectedDate)
        }
    }
}

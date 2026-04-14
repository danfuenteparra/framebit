package com.example.moviebox.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.dto.GenreDto
import com.example.moviebox.data.remote.dto.PersonCreditDto
import com.example.moviebox.data.remote.dto.PersonDto
import com.example.moviebox.ui.screens.home.SectionTitle
import com.example.moviebox.ui.theme.MovieBoxBackground
import com.example.moviebox.ui.theme.MovieBoxOnBackground
import com.example.moviebox.ui.theme.MovieBoxPrimary
import com.example.moviebox.ui.theme.MovieBoxSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val mediaTypeFilter by viewModel.mediaTypeFilter.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Si estamos en vista expandida, mostrar título de categoría
                    val title = when (uiState) {
                        is SearchUiState.ExpandedCategory -> {
                            val data = uiState as SearchUiState.ExpandedCategory
                            data.person.name
                        }
                        is SearchUiState.PersonDetail -> (uiState as SearchUiState.PersonDetail).person.name
                        else -> "Buscar"
                    }
                    Text(title, color = MovieBoxOnBackground, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (uiState) {
                            is SearchUiState.ExpandedCategory -> viewModel.onBackFromExpanded()
                            is SearchUiState.PersonDetail -> {
                                // Volver a lista de personas
                                viewModel.onQueryChange(query)
                            }
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MovieBoxOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MovieBoxBackground)
            )
        },
        containerColor = MovieBoxBackground
    ) { innerPadding ->

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // No mostrar filtros si estamos en detalle de persona o expandido
            if (uiState !is SearchUiState.PersonDetail && uiState !is SearchUiState.ExpandedCategory) {
                // Filtros principales
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = filter == SearchFilter.TITLE, onClick = { viewModel.onFilterChange(SearchFilter.TITLE) }, label = { Text("Título") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MovieBoxPrimary, selectedLabelColor = MovieBoxBackground, containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground))
                    FilterChip(selected = filter == SearchFilter.PERSON, onClick = { viewModel.onFilterChange(SearchFilter.PERSON) }, label = { Text("Actor / Director") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MovieBoxPrimary, selectedLabelColor = MovieBoxBackground, containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground))
                    FilterChip(selected = filter == SearchFilter.GENRE, onClick = { viewModel.onFilterChange(SearchFilter.GENRE) }, label = { Text("Género") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MovieBoxPrimary, selectedLabelColor = MovieBoxBackground, containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground))
                }

                // Sub-filtro Películas/Series (solo género)
                if (filter == SearchFilter.GENRE) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mediaTypeFilter == MediaTypeFilter.MOVIES, onClick = { viewModel.onMediaTypeChange(MediaTypeFilter.MOVIES) }, label = { Text("Películas") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MovieBoxPrimary, selectedLabelColor = MovieBoxBackground, containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground))
                        FilterChip(selected = mediaTypeFilter == MediaTypeFilter.TV_SHOWS, onClick = { viewModel.onMediaTypeChange(MediaTypeFilter.TV_SHOWS) }, label = { Text("Series") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MovieBoxPrimary, selectedLabelColor = MovieBoxBackground, containerColor = MovieBoxSurface, labelColor = MovieBoxOnBackground))
                    }
                }

                // Barra de búsqueda
                if (filter != SearchFilter.GENRE) {
                    OutlinedTextField(
                        value = query, onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text(if (filter == SearchFilter.TITLE) "Buscar por título..." else "Buscar actor o director...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MovieBoxOnBackground) },
                        singleLine = true, shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MovieBoxOnBackground, unfocusedTextColor = MovieBoxOnBackground, focusedBorderColor = MovieBoxPrimary, unfocusedBorderColor = MovieBoxSurface),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Contenido
            when (uiState) {
                is SearchUiState.Idle -> {
                    if (filter == SearchFilter.GENRE) GenreGrid(genres = genres, onGenreClick = { viewModel.onGenreSelected(it) })
                    else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Escribe para buscar...", color = MovieBoxOnBackground.copy(alpha = 0.5f)) }
                }
                is SearchUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MovieBoxPrimary) }
                is SearchUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((uiState as SearchUiState.Error).message, color = MovieBoxOnBackground) }
                is SearchUiState.TitleResults -> TitleResultsList(data = uiState as SearchUiState.TitleResults, onMovieClick = onMovieClick, onTvShowClick = onTvShowClick)
                is SearchUiState.PersonList -> PersonListView(persons = (uiState as SearchUiState.PersonList).persons, onPersonClick = { viewModel.onPersonSelected(it) })
                is SearchUiState.PersonDetail -> PersonDetailView(data = uiState as SearchUiState.PersonDetail, onMovieClick = onMovieClick, onTvShowClick = onTvShowClick, onExpandCategory = { viewModel.onExpandCategory(it) })
                is SearchUiState.ExpandedCategory -> ExpandedCategoryView(data = uiState as SearchUiState.ExpandedCategory, onMovieClick = onMovieClick, onTvShowClick = onTvShowClick)
                is SearchUiState.GenreMovieResults -> GenreMovieGrid(data = uiState as SearchUiState.GenreMovieResults, onMovieClick = onMovieClick)
                is SearchUiState.GenreTvShowResults -> GenreTvShowGrid(data = uiState as SearchUiState.GenreTvShowResults, onTvShowClick = onTvShowClick)
            }
        }
    }
}

// ========== TÍTULO ==========

@Composable
fun TitleResultsList(data: SearchUiState.TitleResults, onMovieClick: (Int) -> Unit, onTvShowClick: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (data.movies.isNotEmpty()) {
            item { SectionTitle(title = "Películas") }
            items(data.movies) { movie -> SearchResultItem(title = movie.title, overview = movie.overview, rating = movie.voteAverage, onClick = { onMovieClick(movie.id) }) }
        }
        if (data.tvShows.isNotEmpty()) {
            item { SectionTitle(title = "Series") }
            items(data.tvShows) { tvShow -> SearchResultItem(title = tvShow.name, overview = tvShow.overview, rating = tvShow.voteAverage, onClick = { onTvShowClick(tvShow.id) }) }
        }
        if (data.movies.isEmpty() && data.tvShows.isEmpty()) {
            item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Sin resultados", color = MovieBoxOnBackground.copy(alpha = 0.5f)) } }
        }
    }
}

// ========== LISTA DE PERSONAS ==========

@Composable
fun PersonListView(persons: List<PersonDto>, onPersonClick: (PersonDto) -> Unit) {
    if (persons.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin resultados", color = MovieBoxOnBackground.copy(alpha = 0.5f)) }; return }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(persons) { person ->
            Card(onClick = { onPersonClick(person) }, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(model = TmdbApiService.getImageUrl(person.profilePath, TmdbApiService.PROFILE_SIZE), contentDescription = person.name, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = person.name, color = MovieBoxOnBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = person.knownForDepartment, color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 13.sp)
                        if (person.knownFor.isNotEmpty()) {
                            Text(text = person.knownFor.take(3).mapNotNull { it.title ?: it.name }.joinToString(", "), color = MovieBoxOnBackground.copy(alpha = 0.4f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ========== DETALLE PERSONA (4 categorías con preview) ==========

@Composable
fun PersonDetailView(data: SearchUiState.PersonDetail, onMovieClick: (Int) -> Unit, onTvShowClick: (Int) -> Unit, onExpandCategory: (FilmCategory) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                AsyncImage(model = TmdbApiService.getImageUrl(data.person.profilePath, TmdbApiService.PROFILE_SIZE), contentDescription = data.person.name, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                Column {
                    Text(text = data.person.name, color = MovieBoxOnBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = data.person.knownForDepartment, color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }

        // Actuación en Películas
        if (data.actedMovies.isNotEmpty()) {
            item { FilmCategorySection(title = "Actuación en Películas", count = data.actedMovies.size, items = data.actedMovies.take(8), onMovieClick = onMovieClick, onTvShowClick = onTvShowClick, onSeeAll = { onExpandCategory(FilmCategory.ACTED_MOVIES) }) }
        }

        // Actuación en Series
        if (data.actedTvShows.isNotEmpty()) {
            item { FilmCategorySection(title = "Actuación en Series", count = data.actedTvShows.size, items = data.actedTvShows.take(8), onMovieClick = onMovieClick, onTvShowClick = onTvShowClick, onSeeAll = { onExpandCategory(FilmCategory.ACTED_TV) }) }
        }

        // Dirección en Películas
        if (data.directedMovies.isNotEmpty()) {
            item { FilmCategorySection(title = "Dirección en Películas", count = data.directedMovies.size, items = data.directedMovies.take(8), onMovieClick = onMovieClick, onTvShowClick = onTvShowClick, onSeeAll = { onExpandCategory(FilmCategory.DIRECTED_MOVIES) }) }
        }

        // Dirección en Series
        if (data.directedTvShows.isNotEmpty()) {
            item { FilmCategorySection(title = "Dirección en Series", count = data.directedTvShows.size, items = data.directedTvShows.take(8), onMovieClick = onMovieClick, onTvShowClick = onTvShowClick, onSeeAll = { onExpandCategory(FilmCategory.DIRECTED_TV) }) }
        }

        if (data.actedMovies.isEmpty() && data.actedTvShows.isEmpty() && data.directedMovies.isEmpty() && data.directedTvShows.isEmpty()) {
            item { Text("Sin filmografía disponible", color = MovieBoxOnBackground.copy(alpha = 0.5f)) }
        }
    }
}

@Composable
fun FilmCategorySection(title: String, count: Int, items: List<PersonCreditDto>, onMovieClick: (Int) -> Unit, onTvShowClick: (Int) -> Unit, onSeeAll: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Header con título y count
        Text(text = "$title ($count)", color = MovieBoxPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        // Grid de posters (2 filas x 4 columnas max)
        val rows = items.chunked(4)
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { credit ->
                    CreditPosterItem(credit = credit, onMovieClick = onMovieClick, onTvShowClick = onTvShowClick, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Botón "Ver todas"
        if (count > 8) {
            TextButton(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) {
                Text("Ver todas", color = MovieBoxPrimary, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MovieBoxPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ========== VISTA EXPANDIDA (todas las obras de una categoría) ==========

@Composable
fun ExpandedCategoryView(data: SearchUiState.ExpandedCategory, onMovieClick: (Int) -> Unit, onTvShowClick: (Int) -> Unit) {
    val title = when (data.category) {
        FilmCategory.ACTED_MOVIES -> "Actuación en Películas"
        FilmCategory.ACTED_TV -> "Actuación en Series"
        FilmCategory.DIRECTED_MOVIES -> "Dirección en Películas"
        FilmCategory.DIRECTED_TV -> "Dirección en Series"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "$title (${data.items.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MovieBoxPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data.items) { credit ->
                CreditPosterItem(credit = credit, onMovieClick = onMovieClick, onTvShowClick = onTvShowClick, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ========== COMPONENTES COMPARTIDOS ==========

@Composable
fun CreditPosterItem(credit: PersonCreditDto, onMovieClick: (Int) -> Unit, onTvShowClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(2f / 3f).clickable { if (credit.mediaType == "movie") onMovieClick(credit.id) else onTvShowClick(credit.id) },
        colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)
    ) {
        AsyncImage(model = TmdbApiService.getImageUrl(credit.posterPath), contentDescription = credit.title ?: credit.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun GenreGrid(genres: List<GenreDto>, onGenreClick: (GenreDto) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(genres.size) { index ->
            val genre = genres[index]
            Card(onClick = { onGenreClick(genre) }, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = genre.name, color = MovieBoxOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
fun GenreMovieGrid(data: SearchUiState.GenreMovieResults, onMovieClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = data.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(data.movies.filter { it.posterPath != null }) { movie ->
                Card(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clickable { onMovieClick(movie.id) }, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)) {
                    AsyncImage(model = TmdbApiService.getImageUrl(movie.posterPath), contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

@Composable
fun GenreTvShowGrid(data: SearchUiState.GenreTvShowResults, onTvShowClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = data.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MovieBoxOnBackground, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(data.tvShows.filter { it.posterPath != null }) { tvShow ->
                Card(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clickable { onTvShowClick(tvShow.id) }, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface)) {
                    AsyncImage(model = TmdbApiService.getImageUrl(tvShow.posterPath), contentDescription = tvShow.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(title: String, overview: String, rating: Double, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MovieBoxSurface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = MovieBoxOnBackground, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = overview, color = MovieBoxOnBackground.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "\u2B50 ${String.format("%.1f", rating)}", color = MovieBoxPrimary, fontSize = 12.sp)
            }
        }
    }
}
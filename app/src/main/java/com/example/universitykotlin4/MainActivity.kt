package com.example.universitykotlin4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.universitykotlin4.ui.theme.UniversityKotlin4Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniversityKotlin4Theme {
                RepoSearchScreen()
            }
        }
    }
}

suspend fun SearchRepos(query: String, allRepos: List<Repo>): List<Repo> =
    withContext(Dispatchers.Default) {
        delay(1000)
        allRepos.filter { it.full_name.contains(query, ignoreCase = true) }
    }

fun <T> CoroutineScope.debounce(
    waitMs: Long = 500L, destinationFunction: suspend (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun RepoSearchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Repo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val allRepos = remember {
        val raw = context.resources.openRawResource(R.raw.github_repos)
            .bufferedReader().readText()
        Json.decodeFromString<List<Repo>>(raw)
    }

    val searchRepos = remember {
        scope.debounce<String>(waitMs = 500L) { q ->
            isLoading = true
            results = SearchRepos(q, allRepos)
            isLoading = false
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        OutlinedTextField(
            value = query,
            onValueChange = { newQuery ->
                query = newQuery
                if (newQuery.isNotBlank()) {
                    isLoading = true
                    searchRepos(newQuery)
                } else {
                    isLoading = false
                    results = emptyList()
                }
            },
            label = { Text("Поиск репозиториев") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn {
                items(results) { repo -> RepoCard(repo) }
            }
        }
    }
}


@Composable
fun RepoCard(repo: Repo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                repo.full_name,
                style = MaterialTheme.typography.titleMedium
            )
            repo.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                Text(
                    "⭐ ${repo.stargazers_count}",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.width(12.dp))
                Text(repo.language, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

package com.example.modul_4

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.modul_4.ui.theme.Modul_4Theme
import com.google.gson.Gson
import kotlinx.coroutines.*

data class Repo(
    val id: Int,
    val full_name: String,
    val description: String?,
    val stargazers_count: Int,
    val language: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme {
                SearchScreen(context = this)
            }
        }
    }
}

@Composable
fun SearchScreen(context: Context) {
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Repo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun onQueryChanged(newQuery: String) {
        query = newQuery

        searchJob?.cancel()

        if (newQuery.isBlank()) {
            results = emptyList()
            isLoading = false
            return
        }

        searchJob = scope.launch {
            isLoading = true

            delay(500L)

            val found = withContext(Dispatchers.IO) {
                val json = context.assets.open("github_repos.json")
                    .bufferedReader()
                    .use { it.readText() }

                val allRepos = Gson().fromJson(json, Array<Repo>::class.java)

                delay(300L)

                allRepos.filter { repo ->
                    repo.full_name.contains(newQuery, ignoreCase = true) ||
                            repo.description?.contains(newQuery, ignoreCase = true) == true ||
                            repo.language?.contains(newQuery, ignoreCase = true) == true
                }
            }

            results = found
            isLoading = false
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChanged(it) },
                label = { Text("Поиск репозиториев...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { repo ->
                    RepoCard(repo)
                }
            }
        }
    }
}

@Composable
fun RepoCard(repo: Repo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = repo.full_name,
                style = MaterialTheme.typography.titleMedium
            )
            repo.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${repo.stargazers_count}")
                repo.language?.let { Text(" $it") }
            }
        }
    }
}

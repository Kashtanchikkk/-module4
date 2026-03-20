package com.example.modul_4

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.modul_4.ui.theme.Modul_4Theme
import com.google.gson.Gson
import kotlinx.coroutines.*


data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    val avatarUrl: String
)

data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val body: String
)

data class PostState(
    val post: Post,
    val avatarColor: String? = null,
    val comments: List<Comment>? = null,
    val avatarStatus: LoadStatus = LoadStatus.Loading,
    val commentsStatus: LoadStatus = LoadStatus.Loading
)

enum class LoadStatus { Loading, Ready, Error }


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Modul_4Theme {
                SocialFeedScreen(context = this)
            }
        }
    }
}


@Composable
fun SocialFeedScreen(context: Context) {
    val scope = rememberCoroutineScope()

    var postStates by remember { mutableStateOf<List<PostState>>(emptyList()) }
    var isLoadingPosts by remember { mutableStateOf(true) }

    var loadingJob by remember { mutableStateOf<Job?>(null) }

    fun loadFeed() {
        loadingJob?.cancel()
        isLoadingPosts = true
        postStates = emptyList()

        loadingJob = scope.launch {

            val (posts, allComments) = withContext(Dispatchers.IO) {
                val gson = Gson()

                val postsJson = context.assets.open("social_posts.json")
                    .bufferedReader().use { it.readText() }
                val posts = gson.fromJson(postsJson, Array<Post>::class.java).toList()

                val commentsJson = context.assets.open("comments.json")
                    .bufferedReader().use { it.readText() }
                val comments = gson.fromJson(commentsJson, Array<Comment>::class.java).toList()

                posts to comments
            }

            postStates = posts.map { PostState(post = it) }
            isLoadingPosts = false

            posts.forEach { post ->
                launch {
                    supervisorScope {

                        val avatarDeferred = async {
                            withContext(Dispatchers.IO) {
                                delay(500L + (post.id * 100L))
                                if (post.id % 7 == 0) throw RuntimeException("Ошибка аватарки")
                                listOf("#FF6B6B","#4ECDC4","#45B7D1","#96CEB4","#FFEAA7",
                                    "#DDA0DD","#98D8C8","#F7DC6F","#BB8FCE","#85C1E9")
                                    .random()
                            }
                        }

                        val commentsDeferred = async {
                            withContext(Dispatchers.IO) {
                                delay(800L + (post.id * 150L))
                                if (post.id % 9 == 0) throw RuntimeException("Ошибка комментариев")
                                allComments.filter { it.postId == post.id }
                            }
                        }

                        val avatarColor = try {
                            avatarDeferred.await()
                        } catch (e: Exception) { null }

                        val comments = try {
                            commentsDeferred.await()
                        } catch (e: Exception) { null }

                        postStates = postStates.map { state ->
                            if (state.post.id == post.id) {
                                state.copy(
                                    avatarColor = avatarColor,
                                    comments = comments,
                                    avatarStatus = if (avatarColor != null) LoadStatus.Ready else LoadStatus.Error,
                                    commentsStatus = if (comments != null) LoadStatus.Ready else LoadStatus.Error
                                )
                            } else state
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFeed()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Социальная лента", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { loadFeed() }) {
                    Text("Обновить")
                }
            }

            if (isLoadingPosts) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(postStates) { state ->
                        PostCard(state)
                    }
                }
            }
        }
    }
}


@Composable
fun PostCard(state: PostState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                when (state.avatarStatus) {
                    LoadStatus.Loading -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    LoadStatus.Ready -> {
                        val color = parseColor(state.avatarColor ?: "#CCCCCC")
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.post.userId.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    LoadStatus.Error -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color.Red)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text("Пользователь ${state.post.userId}",
                    style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(state.post.title, style = MaterialTheme.typography.titleMedium)
            Text(state.post.body, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            when (state.commentsStatus) {
                LoadStatus.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Загружаем комментарии...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray)
                    }
                }
                LoadStatus.Error -> {
                    Text("Комментарии не загрузились",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                LoadStatus.Ready -> {
                    val comments = state.comments ?: emptyList()
                    if (comments.isEmpty()) {
                        Text("Комментариев пока нет",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray)
                    } else {
                        Text("Комментарии (${comments.size}):",
                            style = MaterialTheme.typography.labelMedium)
                        comments.forEach { comment ->
                            Text(
                                "• ${comment.name}: ${comment.body}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val r = clean.substring(0, 2).toInt(16) / 255f
        val g = clean.substring(2, 4).toInt(16) / 255f
        val b = clean.substring(4, 6).toInt(16) / 255f
        Color(r, g, b)
    } catch (e: Exception) {
        Color.Gray
    }
}

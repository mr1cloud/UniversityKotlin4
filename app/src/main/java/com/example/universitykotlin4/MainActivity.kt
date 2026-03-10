package com.example.universitykotlin4

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.example.universitykotlin4.ui.theme.UniversityKotlin4Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniversityKotlin4Theme {
                SocialFeedScreen()
            }
        }
    }
}

suspend fun loadSocialPosts(context: Context): List<SocialPost> = withContext(Dispatchers.IO) {
    val jsonString = context.resources.openRawResource(R.raw.social_posts).bufferedReader().use { it.readText() }
    Json.decodeFromString(jsonString)
}

suspend fun loadComments(context: Context): List<Comment> = withContext(Dispatchers.IO) {
    val jsonString = context.resources.openRawResource(R.raw.comments).bufferedReader().use { it.readText() }
    Json.decodeFromString(jsonString)
}

suspend fun fetchCommentsForPostWithError(comments: List<Comment>, postId: Int): List<Comment> {
    delay((500L..1500L).random())
    if ((1..5).random() == 1) throw Exception("Failed to load comments")
    return comments.filter { it.postId == postId }
}

@Preview(showBackground = true)
@Composable
fun SocialFeedScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var postStates by remember { mutableStateOf<List<PostState>>(emptyList()) }
    var loadingJob by remember { mutableStateOf<Job?>(null) }

    fun load() {
        loadingJob?.cancel()

        loadingJob = scope.launch {
            val postsDeferred = async { loadSocialPosts(context) }
            val commentsDeferred = async { loadComments(context) }

            val posts = postsDeferred.await()
            val comments = commentsDeferred.await()

            // change all post state to loading
            postStates = posts.map { PostState(post = it) }

            coroutineScope {
                posts.forEachIndexed { index, post ->
                    launch {
                        val postComments = try {
                            fetchCommentsForPostWithError(comments, post.id)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            null
                        }

                        postStates = postStates.toMutableList().also {
                            it[index] = if (postComments == null) {
                                PostState(post, emptyList(), LoadState.Error)
                            } else {
                                PostState(post, postComments, LoadState.Success)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Лента", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { load() }) {
                Text("Обновить")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(postStates, key = { it.post.id }) { state ->
                PostCard(state)
            }
        }
    }

}

@Composable
fun PostCard(state: PostState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = state.post.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state.collectAsState().value) {
                            is AsyncImagePainter.State.Loading ->
                                CircularProgressIndicator()
                            is AsyncImagePainter.State.Error ->
                                Image(
                                    painter = painterResource(R.drawable.error),
                                    contentDescription = "Error",
                                )
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))
                Text(
                    text = state.post.title,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(state.post.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            CommentsSection(state)
        }
    }
}

@Composable
fun CommentsSection(state: PostState) {
    when (state.status) {
        LoadState.Loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Загрузка комментариев...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        LoadState.Error -> {
            Text(
                text = "⚠️ Не удалось загрузить комментарии",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        LoadState.Success -> {
            if (state.comments.isEmpty()) {
                Text(
                    text = "Комментариев пока нет",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Комментарии (${state.comments.size})",
                        style = MaterialTheme.typography.labelMedium
                    )
                    state.comments.forEach { comment ->
                        Row {
                            Text(
                                text = "${comment.name}: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = comment.body,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

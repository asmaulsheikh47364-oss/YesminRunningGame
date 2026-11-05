package com.yesmin.runninggame.ui

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class ObstacleType {
    TRAIN, BARRIER
}

data class Obstacle(
    val lane: Int, // 0: Left, 1: Middle, 2: Right
    var yPos: Float, // distance down the road
    val type: ObstacleType
)

@Composable
fun GameScreen() {
    var gameState by remember { mutableStateOf("START") } // START, PLAYING, GAMEOVER
    var score by remember { mutableStateOf(0f) }
    var highScore by remember { mutableStateOf(0f) }
    var playerLane by remember { mutableIntStateOf(1) } // 0, 1, 2
    var isJumping by remember { mutableStateOf(false) }
    var jumpProgress by remember { mutableStateOf(0f) }
    var speed by remember { mutableStateOf(5f) }
    
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    
    // Game loop
    LaunchedEffect(gameState) {
        if (gameState == "PLAYING") {
            score = 0f
            speed = 6f
            obstacles.clear()
            playerLane = 1
            isJumping = false
            
            var lastObstacleTime = 0L
            while (gameState == "PLAYING") {
                val frameTime = 16L
                delay(frameTime)
                
                // Increase speed gradually
                speed += 0.001f
                score += speed * 0.1f
                
                // Move obstacles
                val iterator = obstacles.iterator()
                while (iterator.hasNext()) {
                    val obs = iterator.next()
                    obs.yPos += speed * 1.5f
                    
                    // Check collision
                    // Player is near the bottom of the screen (e.g., yPos between 700f and 850f)
                    if (obs.lane == playerLane && obs.yPos >= 720f && obs.yPos <= 860f) {
                        if (!(isJumping && obs.type == ObstacleType.BARRIER)) {
                            gameState = "GAMEOVER"
                            if (score > highScore) {
                                highScore = score
                            }
                        }
                    }
                    
                    // Remove off-screen obstacles
                if (obs.yPos > 1100f) {
                        iterator.remove()
                    }
                }
                
                // Spawn obstacles
                lastObstacleTime += frameTime
                val spawnInterval = maxOf(600L, (2000L - speed * 80L).toLong())
                if (lastObstacleTime > spawnInterval) {
                    lastObstacleTime = 0L
                    val lane = Random.nextInt(3)
                    val type = if (Random.nextFloat() > 0.4f) ObstacleType.TRAIN else ObstacleType.BARRIER
                    obstacles.add(Obstacle(lane = lane, yPos = -200f, type = type))
                }
                
                // Handle jump progress
                if (isJumping) {
                    jumpProgress += 0.05f
                    if (jumpProgress >= 1f) {
                        jumpProgress = 0f
                        isJumping = false
                    }
                }
            }
        }
    }
    
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF1E1E28))
        .pointerInput(gameState) {
            if (gameState == "PLAYING") {
                detectDragGestures {
                    change, dragAmount ->
                    change.consume()
                    if (dragAmount.x < -40 && playerLane > 0) {
                        playerLane--
                    } else if (dragAmount.x > 40 && playerLane < 2) {
                        playerLane++
                    } else if (dragAmount.y < -40 && !isJumping) {
                        isJumping = true
                        jumpProgress = 0f
                    }
                }
            }
        }
        .pointerInput(gameState) {
            if (gameState == "PLAYING") {
                detectTapGestures(
                    onTap = {
                        // Tap to jump or tap side
                        if (!isJumping) {
                            isJumping = true
                            jumpProgress = 0f
                        }
                    }
                )
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw 3 Lanes
            val laneWidth = width / 3f
            for (i in 0..3) {
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(i * laneWidth, 0f),
                    end = Offset(i * laneWidth, height),
                    strokeWidth = 4f
                )
            }
            
            // Draw Obstacles (Trains and Barriers)
            for (obs in obstacles) {
                val lWidth = laneWidth
                val left = obs.lane * lWidth + 20f
                val top = obs.yPos
                val rWidth = lWidth - 40f
                val rHeight = if (obs.type == ObstacleType.TRAIN) 220f else 80f
                
                if (obs.type == ObstacleType.TRAIN) {
                    // Train body
                    drawRoundRect(
                        color = Color(0xFFD32F2F),
                        topLeft = Offset(left, top),
                        size = Size(rWidth, rHeight),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    // Train windows
                    drawRect(
                        color = Color(0xFFBBDEFB),
                        topLeft = Offset(left + 20f, top + 30f),
                        size = Size(rWidth - 40f, 50f)
                    )
                    drawRect(
                        color = Color(0xFFBBDEFB),
                        topLeft = Offset(left + 20f, top + 100f),
                        size = Size(rWidth - 40f, 50f)
                    )
                } else {
                    // Barrier
                    drawRoundRect(
                        color = Color(0xFFFFA000),
                        topLeft = Offset(left, top + 40f),
                        size = Size(rWidth, 40f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left + 10f, top + 45f),
                        size = Size(rWidth - 20f, 10f)
                    )
                }
            }
            
            // Draw Player
            val pLeft = playerLane * laneWidth + laneWidth / 2f - 40f
            val jumpOffset = if (isJumping) kotlin.math.sin(jumpProgress * Math.PI).toFloat() * 100f else 0f
            val pTop = 800f - jumpOffset
            
            // Player body
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = 35f,
                center = Offset(pLeft + 40f, pTop - jumpOffset)
            )
            // Player torso
            drawRoundRect(
                color = Color(0xFF03A9F4),
                topLeft = Offset(pLeft + 20f, pTop - jumpOffset + 35f),
                size = Size(40f, 70f),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
        
        // UI Overlays
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (gameState == "PLAYING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Score: ${score.toInt()}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Speed: ${String.format("%.1f", speed)}", color = Color.Yellow, fontSize = 16.sp)
                    Text(text = "High: ${highScore.toInt()}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            } else if (gameState == "START") {
                Spacer(modifier = Modifier.height(150.dp))
                Text(text = "TRAIN RUNNER", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Swipe Left/Right to change lanes\nSwipe Up or Tap to Jump\nAvoid Trains & Barriers!", color = Color.LightGray, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = { gameState = "PLAYING" }) {
                    Text(text = "START GAME", fontSize = 20.sp)
                }
            } else if (gameState == "GAMEOVER") {
                Spacer(modifier = Modifier.height(200.dp))
                Text(text = "GAME OVER", color = Color.Red, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Score: ${score.toInt()}", color = Color.White, fontSize = 24.sp)
                Text(text = "High Score: ${highScore.toInt()}", color = Color.Yellow, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = { gameState = "PLAYING" }) {
                    Text(text = "PLAY AGAIN", fontSize = 20.sp)
                }
            }
        }
    }
}

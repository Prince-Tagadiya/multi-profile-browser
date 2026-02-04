package com.multiprofile.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multiprofile.browser.ui.theme.ProfileBrowserTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ProfileBrowserTheme {
                SplashScreen(
                    onNavigateToMain = {
                        startActivity(Intent(this, ProfileListActivity::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    // Shimmer animation for loading bar
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // Navigate after delay
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateToMain()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F7F8)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main Content - Centered
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // App Icon
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(96.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // App Title
                Text(
                    text = "Profile Browser",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111418),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = "Multiple profiles. One browser.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF617589),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Loading Indicator with shimmer animation
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(6.dp)
                        .background(
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(999.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .offset(x = (128.dp * shimmerTranslate.value * 0.5f))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF137FEC),
                                        Color(0xFF137FEC),
                                    )
                                ),
                                shape = RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
            
            // Footer - Positioned at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "BUILT BY PRINCE NARESHBHAI TAGADIYA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9AAAB9),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    ProfileBrowserTheme {
        SplashScreen(onNavigateToMain = {})
    }
}

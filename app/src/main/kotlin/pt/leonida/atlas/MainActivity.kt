package pt.leonida.atlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  val splash=installSplashScreen()
  splash.setOnExitAnimationListener { screen ->
   if(android.animation.ValueAnimator.areAnimatorsEnabled()) screen.view.animate().alpha(0f).scaleX(1.05f).scaleY(1.05f).setDuration(180).withEndAction { screen.remove() }.start()
   else screen.remove()
  }
  super.onCreate(savedInstanceState); enableEdgeToEdge()
  setContent { AtlasRoot() }
 }
}

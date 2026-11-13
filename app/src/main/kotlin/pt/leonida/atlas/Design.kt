package pt.leonida.atlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

val Coral=Color(0xFFFFA18D)
val Aqua=Color(0xFF8BD9DA)
val Night=Color(0xFF0B0E19)
private val Dark=darkColorScheme(primary=Coral,onPrimary=Color(0xFF392019),secondary=Aqua,onSecondary=Night,
 background=Night,onBackground=Color(0xFFF2EEE9),surface=Color(0xFF131827),onSurface=Color(0xFFF2EEE9),
 surfaceVariant=Color(0xFF232C40),onSurfaceVariant=Color(0xFFB8C2D2),outline=Color(0xFF6C7890),tertiary=Color(0xFFD4B8FF))
private val Light=lightColorScheme(primary=Color(0xFF99432E),onPrimary=Color.White,secondary=Color(0xFF17696B),
 background=Color(0xFFF7F4F1),onBackground=Color(0xFF182137),surface=Color.White,onSurface=Color(0xFF182137),
 surfaceVariant=Color(0xFFE7E9EF),onSurfaceVariant=Color(0xFF4D5B70),outline=Color(0xFF69758A),tertiary=Color(0xFF694A98))
@Composable fun AtlasTheme(dark: Boolean,content: @Composable ()->Unit) {
 MaterialTheme(colorScheme=if(dark) Dark else Light, typography=Typography(
  displayLarge=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Black,fontSize=52.sp,lineHeight=51.sp,letterSpacing=(-2).sp),
  headlineLarge=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=36.sp,letterSpacing=(-1).sp),
  headlineMedium=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=24.sp,lineHeight=30.sp),
  titleLarge=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=20.sp,lineHeight=26.sp),
  bodyLarge=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=25.sp),
  labelSmall=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace,fontSize=11.sp,lineHeight=16.sp,letterSpacing=1.sp)
 ),shapes=Shapes(medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(26.dp)),content=content)
}
@Composable fun Eyebrow(text: String,modifier: Modifier=Modifier) {
 Text(text.uppercase(),modifier,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
}
// Original resolution-independent artwork. This is atmosphere, never game imagery or cartography.
@Composable fun HorizonArt(modifier: Modifier=Modifier) {
 Canvas(modifier) {
  val w=size.width; val h=size.height
  drawRect(Brush.verticalGradient(listOf(Color(0xFF152347),Color(0xFF64456D),Color(0xFFDF827D),Color(0xFFF3B68E))))
  drawCircle(Brush.radialGradient(listOf(Color(0xFFFCD8A0),Color(0xFFFFAF83))),w*.20f,Offset(w*.72f,h*.36f))
  for(i in 0..6) drawLine(Color(0xFF7A5271).copy(alpha=.18f),Offset(0f,h*.57f+i*h*.012f),Offset(w,h*.57f+i*h*.012f),h*.004f)
  drawRect(Color(0xFF172E43),Offset(0f,h*.70f),Size(w,h*.30f))
  for(i in 0..23) {
   val bw=w/26;val bh=h*(.05f+((i*13)%9)*.012f);val x=i*w/23
   drawRect(Color(0xFF273047),Offset(x,h*.7f-bh),Size(bw,bh))
   if(i%3==0) drawLine(Color(0xFFFFC0A0).copy(alpha=.55f),Offset(x+bw*.5f,h*.7f-bh*.8f),Offset(x+bw*.5f,h*.69f),w*.002f)
  }
  for(i in 0..18) {
   val y=h*.73f+i*h*.012f; val length=w*(.1f+(i%4)*.09f)
   drawLine(Color(0xFFEBAC98).copy(alpha=.2f),Offset(w*.7f-length*.5f,y),Offset(w*.7f+length*.5f,y),h*.002f)
  }
  fun palm(x: Float,y: Float,scale: Float) {
   val trunk=Path().apply { moveTo(x,y); cubicTo(x+scale*.08f,y-scale*.4f,x-scale*.06f,y-scale*.75f,x+scale*.08f,y-scale) }
   drawPath(trunk,Color(0xFF101D31),style=Stroke(scale*.035f))
   val crown=Offset(x+scale*.08f,y-scale)
   for(a in listOf(-170,-140,-105,-65,-25,12,40)) {
    val rad=a*PI/180; val dx=cos(rad).toFloat()*scale*.42f;val dy=sin(rad).toFloat()*scale*.3f
    val leaf=Path().apply { moveTo(crown.x,crown.y); quadraticTo(crown.x+dx*.7f,crown.y+dy-scale*.16f,crown.x+dx,crown.y+dy+scale*.12f); quadraticTo(crown.x+dx*.55f,crown.y+dy-scale*.08f,crown.x,crown.y) }
    drawPath(leaf,Color(0xFF101D31))
   }
  }
  palm(w*.12f,h*.85f,h*.63f);palm(w*.97f,h*.84f,h*.85f)
  drawRect(Brush.verticalGradient(listOf(Color.Transparent,Night.copy(alpha=.98f)),startY=h*.40f))
 }
}
@Composable fun AtlasLogo(modifier: Modifier=Modifier) {
 Canvas(modifier.size(40.dp)) {
  val p=Path().apply { moveTo(size.width*.5f,0f); lineTo(size.width,size.height);lineTo(size.width*.72f,size.height);lineTo(size.width*.5f,size.height*.48f);lineTo(size.width*.28f,size.height);lineTo(0f,size.height);close() }
  drawPath(p,Coral);drawLine(Aqua,Offset(0f,size.height*.6f),Offset(size.width,size.height*.6f),size.width*.06f)
 }
}

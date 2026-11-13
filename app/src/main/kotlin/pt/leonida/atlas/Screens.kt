@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package pt.leonida.atlas

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.leonida.atlas.core.*
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

val LocalLanguage=staticCompositionLocalOf { "en" }
@Composable fun t(en: String,pt: String)=if(LocalLanguage.current=="pt") pt else en
@Composable fun category(key: String): String = when(key) {
 "all"->t("All","Tudo"); "people"->t("People","Personagens"); "regions"->t("Regions","Regiões")
 "news"->t("News","Notícias"); "trailers"->t("Videos","Vídeos"); "vehicles"->t("Vehicles","Veículos")
 "weapons"->t("Weapons","Armas"); "business"->t("Businesses","Negócios"); "wildlife"->t("Wildlife","Fauna")
 "culture"->t("Culture","Cultura"); "radio"->t("Radio & music","Rádio e música")
 "mechanics"->t("Mechanics","Mecânicas"); "story"->t("Story","História"); else->key
}

@Composable fun AtlasRoot(vm: AtlasViewModel=viewModel()) {
 val state by vm.state.collectAsStateWithLifecycle()
 val prefs=state.prefs
 val dark=when(prefs.theme) { "dark"->true; "light"->false; else->isSystemInDarkTheme() }
 AtlasTheme(dark) {
  CompositionLocalProvider(LocalLanguage provides prefs.language) {
   val activity=LocalContext.current as? android.app.Activity
   SideEffect { activity?.window?.let { window ->
    androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
     isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark
    }
   } }
   Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
    when {
     state.error -> Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),verticalArrangement=Arrangement.Center) {
      AtlasLogo(); Spacer(Modifier.height(24.dp)); Text(t("The atlas could not open.","Não foi possível abrir o atlas."),style=MaterialTheme.typography.headlineMedium)
      Text(t("Your data is preserved. Try opening it again.","Os teus dados estão guardados. Tenta abrir novamente."));Button(onClick={vm.retry()}) { Text(t("Try again","Tentar novamente")) }
     }
     !state.ready || state.pack==null -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
      AtlasLogo(); Spacer(Modifier.height(24.dp)); Text("LEONIDA ATLAS",style=MaterialTheme.typography.labelSmall);Spacer(Modifier.height(24.dp));LinearProgressIndicator(Modifier.width(120.dp))
     }
     !prefs.onboarding -> Onboarding { vm.preference("onboarding",true) }
     else -> AtlasNavigation(state,vm)
    }
   }
  }
 }
}
@Composable fun Onboarding(done: ()->Unit) {
 Box(Modifier.fillMaxSize()) {
  HorizonArt(Modifier.fillMaxSize())
  Column(Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),verticalArrangement=Arrangement.SpaceBetween) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    AtlasLogo();TextButton(onClick=done) { Text(t("Skip","Saltar"),color=Color.White) }
   }
   Column {
    Text("LEONIDA ATLAS",style=MaterialTheme.typography.labelSmall,color=Aqua)
    Spacer(Modifier.height(20.dp))
    Text(t("Your own\nway through\nLeonida.","Leonida.\nÀ tua\nmaneira."),style=MaterialTheme.typography.displayLarge,color=Color.White)
    Spacer(Modifier.height(24.dp))
    Text(t("Verified sources. Your plans. Always with you, even offline.","Fontes verificadas. Os teus planos. Sempre contigo, mesmo offline."),color=Color(0xFFE5E0E4),style=MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(16.dp))
    Text(t("Independent, unofficial GTA VI companion. No account needed. Story spoilers are hidden by default.","Companion independente e não oficial de GTA VI. Sem conta. Os spoilers de história ficam ocultos por defeito."),color=Color(0xFFD0CEDA),style=MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(28.dp))
    Button(onClick=done,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text(t("Open my atlas","Abrir o meu atlas"));Spacer(Modifier.width(12.dp));Icon(Icons.Outlined.ArrowForward,null) }
   }
  }
 }
}
private data class Destination(val route: String,val en: String,val pt: String,val icon: ImageVector)
private val destinations=listOf(
 Destination("home","Home","Início",Icons.Outlined.Home),Destination("map","Atlas","Atlas",Icons.Outlined.Explore),
 Destination("discover","Discover","Explorar",Icons.Outlined.Search),Destination("news","News","Notícias",Icons.Outlined.Article),Destination("vault","My vault","Arquivo",Icons.Outlined.Bookmarks))

@Composable fun AtlasNavigation(state: State,vm: AtlasViewModel) {
 val nav=rememberNavController();val back by nav.currentBackStackEntryAsState();val route=back?.destination?.route ?: "home"
 val snackbar=remember { SnackbarHostState() }; val scope=rememberCoroutineScope(); val context=LocalContext.current
 val savedMessage=t("Saved","Guardado"); val failureMessage=t("Could not complete this action. Check your input and try again.","Não foi possível concluir. Verifica os dados e tenta novamente.")
 val syncMessage=t("Content updated","Conteúdo atualizado")
 val noSync=t("Live updates are not configured. The bundled atlas remains available offline.","As atualizações remotas ainda não estão configuradas. O atlas incluído continua disponível offline.")
 val syncFailure=t("Update failed. Your offline content is preserved. Check your connection or contact the publisher.","A atualização falhou. O conteúdo offline foi preservado. Verifica a ligação ou contacta o editor.")
 LaunchedEffect(vm,failureMessage) { vm.messages.collect { code -> snackbar.showSnackbar(when(code) { "synced"->syncMessage;"no-sync"->noSync;"sync-error"->syncFailure;"saved"->savedMessage;else->failureMessage }) } }
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) scope.launch {
  try { val text=vm.export();withContext(Dispatchers.IO) { checkNotNull(context.contentResolver.openOutputStream(uri,"wt")).use { it.write(text.toByteArray()) } };vm.notify("saved") }
  catch(_: Exception) { vm.notify("error") }
 } }
 val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) scope.launch {
  try {
   val text=withContext(Dispatchers.IO) { checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var count=input.read(buffer)
    while(count!=-1) { require(output.size()+count<=2_000_000);output.write(buffer,0,count);count=input.read(buffer) };output.toString("UTF-8")
   } };vm.importBackup(text);vm.notify("saved")
  } catch(_: Exception) { vm.notify("error") }
 } }
 val go: (String)->Unit={ target -> nav.navigate(target) { launchSingleTop=true;restoreState=true;popUpTo("home") { saveState=true } } }
 val open: (String)->Unit={ id -> vm.visit(id);nav.navigate("entry/$id") }
 BoxWithConstraints(Modifier.fillMaxSize()) {
  val wide=maxWidth>=840.dp
  Row(Modifier.fillMaxSize()) {
   if(wide) NavigationRail(Modifier.fillMaxHeight().safeDrawingPadding(),containerColor=MaterialTheme.colorScheme.surface) {
    Spacer(Modifier.height(24.dp));AtlasLogo();Spacer(Modifier.height(40.dp))
    destinations.forEach { d -> NavigationRailItem(selected=route==d.route,onClick={go(d.route)},icon={Icon(d.icon,null)},label={Text(t(d.en,d.pt))}) }
   }
   Scaffold(Modifier.weight(1f),containerColor=MaterialTheme.colorScheme.background,snackbarHost={SnackbarHost(snackbar)},
    topBar={
     Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=20.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
      if(route.startsWith("entry") || route=="settings") IconButton(onClick={nav.popBackStack()}) { Icon(Icons.Outlined.ArrowBack,t("Back","Voltar")) }
      else AtlasLogo(Modifier.size(32.dp))
      Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)) {
       Text("LEONIDA ATLAS",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
       Text(t("INDEPENDENT FIELD GUIDE","GUIA INDEPENDENTE"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp)
      }
      IconButton(onClick={go("settings")}) { Icon(Icons.Outlined.Tune,t("Settings","Definições")) }
     }
    },bottomBar={ if(!wide) NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp) {
     destinations.forEach { d -> NavigationBarItem(selected=route==d.route,onClick={go(d.route)},icon={Icon(d.icon,null)},label={Text(t(d.en,d.pt),maxLines=1,fontSize=11.sp)}) }
    } }
   ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize(),contentAlignment=Alignment.TopCenter) {
     NavHost(nav,startDestination="home",modifier=Modifier.widthIn(max=1100.dp).fillMaxSize(),
      enterTransition={fadeIn(androidx.compose.animation.core.tween(180))},exitTransition={fadeOut(androidx.compose.animation.core.tween(100))}) {
      composable("home") { HomeScreen(state,open,go) }
      composable("discover") { DiscoverScreen(state,open) }
      composable("news") { NewsScreen(state,vm,open) }
      composable("map") { MapScreen(state,open) }
      composable("vault") { VaultScreen(state,vm,open,{exporter.launch("leonida-atlas-backup.json")},{importer.launch(arrayOf("application/json","text/plain"))}) }
      composable("entry/{id}") { b -> state.pack!!.entries.find { it.id==b.arguments?.getString("id") }?.let { DetailScreen(it,state,vm,open) } }
      composable("settings") { SettingsScreen(state,vm,{exporter.launch("leonida-atlas-backup.json")},{importer.launch(arrayOf("application/json","text/plain"))}) }
     }
    }
   }
  }
 }
}

@Composable fun Page(content: LazyListScope.()->Unit) {
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=24.dp,end=24.dp,top=16.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
}
@Composable fun PageTitle(eyebrow: String,title: String,description: String="") {
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { Eyebrow(eyebrow);Text(title,style=MaterialTheme.typography.headlineLarge);if(description.isNotBlank()) Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant) }
}
@Composable fun Section(title: String,action: String="",onAction: ()->Unit={}) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
  if(action.isNotBlank()) TextButton(onClick=onAction) { Text(action);Icon(Icons.Outlined.ChevronRight,null,Modifier.size(16.dp)) }
 }
}
@Composable fun EmptyState(title: String,body: String,icon: ImageVector=Icons.Outlined.BookmarkBorder) {
 Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Icon(icon,null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(32.dp));Text(title,style=MaterialTheme.typography.titleLarge);Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
@Composable fun EntryCard(entry: Entry,state: State,onClick: ()->Unit) {
 val hidden=entry.spoiler in state.hidden
 Card(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag("entry-${entry.id}"),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Row(Modifier.padding(20.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
   Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center) {
    Icon(when(entry.category) { "people"->Icons.Outlined.PersonOutline;"regions"->Icons.Outlined.Explore;"news"->Icons.Outlined.Article;else->Icons.Outlined.PlayCircleOutline },null,tint=MaterialTheme.colorScheme.secondary)
   }
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Eyebrow(category(entry.category));if(entry.published.isNotBlank()) Text(entry.published,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(entry.title.inLanguage(state.prefs.language),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    Text(if(hidden) t("Protected by Spoiler Shield","Protegido pelo Spoiler Shield") else entry.summary.inLanguage(state.prefs.language),style=MaterialTheme.typography.bodyMedium,maxLines=3,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   if(state.saved(entry.id)) Icon(Icons.Outlined.Bookmark,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(18.dp))
   else Icon(Icons.Outlined.ChevronRight,null,modifier=Modifier.size(18.dp))
  }
 }
}
@Composable fun HomeScreen(state: State,open: (String)->Unit,go: (String)->Unit) {
 val pack=state.pack!!; val today by produceState(LocalDate.now()) { while(true) { value=LocalDate.now();kotlinx.coroutines.delay(60_000) } }
 val days=Rules.daysUntil(pack.launchDate,today);val launched=!today.isBefore(LocalDate.parse(pack.launchDate))
 val recommendations=Discovery.recommended(pack.entries,state.local,state.hidden)
 Page {
  item {
   Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
    HorizonArt(Modifier.matchParentSize())
    Column(Modifier.padding(24.dp).padding(top=100.dp)) {
     Surface(color=Night.copy(alpha=.65f),shape=RoundedCornerShape(50)) { Text(t("YOUR INDEPENDENT ATLAS","O TEU ATLAS INDEPENDENTE"),Modifier.padding(horizontal=12.dp,vertical=8.dp),color=Aqua,style=MaterialTheme.typography.labelSmall) }
     Spacer(Modifier.height(20.dp))
     Text(t("Every corner.\nYour story.","Um mundo.\nO teu caminho."),style=MaterialTheme.typography.displayLarge,color=Color.White)
     Spacer(Modifier.height(16.dp))
     Text(t("Explore what is known. Plan what comes next.","Explora o que já se sabe. Planeia o que vem a seguir."),color=Color(0xFFE2E4EE),style=MaterialTheme.typography.bodyLarge)
     Spacer(Modifier.height(22.dp))
     Button(onClick={go(state.prefs.shortcut)},colors=ButtonDefaults.buttonColors(containerColor=Coral,contentColor=Night)) { Text(t("Open ","Abrir ")+when(state.prefs.shortcut) {"discover"->t("discovery","exploração");"news"->t("news","notícias");"vault"->t("my vault","o meu arquivo");else->t("the atlas","o atlas") });Spacer(Modifier.width(12.dp));Icon(Icons.Outlined.ArrowForward,null) }
     Spacer(Modifier.height(12.dp));Text(t("Original editorial artwork · Not game footage","Arte editorial original · Não é uma imagem do jogo"),style=MaterialTheme.typography.labelSmall,color=Color(0xFFCED3E1),fontSize=9.sp)
    }
   }
  }
  item {
   Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
    Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
     Text(if(launched) "19.11" else days.toString().padStart(2,'0'),style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.primary)
     Column(Modifier.weight(1f)) {
      Eyebrow(if(launched) t("ANNOUNCED DATE REACHED","DATA ANUNCIADA ATINGIDA") else t("DAYS TO THE ANNOUNCED DATE","DIAS ATÉ À DATA ANUNCIADA"))
      Text(pack.launchDate,style=MaterialTheme.typography.titleMedium)
      Text(t("Local calendar · Check the source for changes","Calendário local · Consulta a fonte para alterações"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
    TextButton(onClick={open("release")},modifier=Modifier.padding(horizontal=12.dp)) { Text(t("View release announcement","Ver anúncio de lançamento")) }
   }
  }
  item { Section(t("On the wire","Em destaque"),t("All news","Ver notícias")) {go("news")} }
  items(pack.entries.filter { it.category=="news" }.sortedByDescending { it.published }.take(2),key={it.id}) { EntryCard(it,state) {open(it.id)} }
  item { Section(t("Jason & Lucia","Jason e Lucia"),t("Explore","Explorar")) {go("discover")} }
  item {
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { listOf("jason","lucia").forEach { id ->
    val e=pack.entries.firstOrNull {it.id==id};if(e!=null) Card(onClick={open(id)},modifier=Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) {
     Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Icon(Icons.Outlined.PersonOutline,null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(32.dp));Text(e.title.inLanguage(state.prefs.language),style=MaterialTheme.typography.titleLarge);Text(t("Open dossier","Abrir ficha"),color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
   } }
  }
  item { Section(t("Discover next","A seguir no teu atlas")) }
  items(recommendations.take(3),key={"rec-${it.id}"}) { EntryCard(it,state) {open(it.id)} }
  val recent=state.local.filter { it.kind=="favorite" }.sortedByDescending { it.updated }.take(3).mapNotNull { i -> pack.entries.find { it.id==i.target } }
  if(recent.isNotEmpty()) { item {Section(t("Recently saved","Guardados recentemente"))};items(recent,key={"saved-${it.id}"}) { EntryCard(it,state){open(it.id)} } }
  item { Text(t("Content checked: ","Conteúdo verificado: ")+pack.verified+" · v${pack.version}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
 }
}

@Composable fun DiscoverScreen(state: State,open: (String)->Unit) {
 var query by rememberSaveable { mutableStateOf("") };var cat by rememberSaveable { mutableStateOf("all") }
 val entries=remember(state.pack,query,cat,state.hidden) { Discovery.search(state.pack!!.entries,query,cat,state.hidden) }
 Page {
  item { PageTitle(t("UNIVERSAL SEARCH","PESQUISA GLOBAL"),t("Find your next\ndiscovery.","O próximo\ndescobrimento.")) }
  item { OutlinedTextField(value=query,onValueChange={query=it.take(200)},modifier=Modifier.fillMaxWidth(),label={Text(t("Search the atlas","Pesquisar no atlas"))},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,trailingIcon={if(query.isNotBlank()) IconButton(onClick={query=""}) {Icon(Icons.Outlined.Close,t("Clear search","Limpar pesquisa"))}}) }
  item { LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(listOf("all")+Rules.categories.toList()) { c -> FilterChip(selected=cat==c,onClick={cat=c},label={Text(category(c))}) } } }
  item { Text(t("${entries.size} results · Story protection follows your settings","${entries.size} resultados · Proteção de história conforme as definições"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  if(entries.isEmpty()) item { EmptyState(t("No verified results here yet","Ainda sem resultados verificados"),t("Try another term or category. Story content may be hidden by your spoiler settings.","Experimenta outro termo ou categoria. As definições de spoilers podem ocultar conteúdo de história."),Icons.Outlined.Search) }
  items(entries,key={it.id}) { EntryCard(it,state){open(it.id)} }
 }
}
@Composable fun NewsScreen(state: State,vm: AtlasViewModel,open: (String)->Unit) {
 var query by rememberSaveable { mutableStateOf("") };var unread by rememberSaveable {mutableStateOf(false)}
 val syncing by vm.syncing.collectAsStateWithLifecycle()
 val read=state.local.filter { it.kind=="history" }.map { it.target }.toSet()
 val entries=Discovery.search(state.pack!!.entries,query,"news",state.hidden).filter { !unread || it.id !in read }.sortedByDescending {it.published}
 Page {
  item { PageTitle("NEWSWIRE",t("Stay in the know.","A par de Leonida."),t("Original summaries with direct links to verified sources.","Resumos próprios com ligações diretas a fontes verificadas.")) }
  item { Row(verticalAlignment=Alignment.CenterVertically) {
   FilterChip(unread,{unread=!unread},label={Text(t("Unread","Por ler"))});Spacer(Modifier.weight(1f))
   TextButton(onClick={vm.sync()},enabled=!syncing) { if(syncing) CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp) else Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(t("Check updates","Ver atualizações")) }
  } }
  item { OutlinedTextField(query,{query=it.take(200)},Modifier.fillMaxWidth(),label={Text(t("Search news","Pesquisar notícias"))},singleLine=true) }
  item { Text(t("Offline edition checked ","Edição offline verificada em ")+state.pack.verified,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  if(entries.isEmpty()) item {EmptyState(t("All caught up","Tudo em dia"),t("Change the unread filter or try another search.","Altera o filtro de leitura ou experimenta outra pesquisa."))}
  items(entries,key={it.id}) { e -> Column(verticalArrangement=Arrangement.spacedBy(6.dp)) { EntryCard(e,state){open(e.id)};if(e.id in read) Text(t("Read","Lido"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
 }
}

@Composable fun MapScreen(state: State,open: (String)->Unit) {
 var filter by rememberSaveable { mutableStateOf("all") };var query by rememberSaveable { mutableStateOf("") }
 var scale by remember {mutableFloatStateOf(1f)};var offset by remember {mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)}
 val entries=Discovery.search(state.pack!!.entries,query,"regions",state.hidden).filter {filter=="all" || (filter=="saved" && state.saved(it.id)) || (filter=="visited" && state.visited(it.id))}
 Page {
  item {PageTitle(t("LEONIDA ATLAS","LEONIDA ATLAS"),t("Places to know.","Lugares a descobrir."),t("Editorial atlas of confirmed regions. No authorized geographic map or coordinates are bundled.","Atlas editorial das regiões confirmadas. Não inclui mapa geográfico autorizado nem coordenadas."))}
  item {OutlinedTextField(query,{query=it.take(200)},Modifier.fillMaxWidth(),label={Text(t("Find a region","Encontrar uma região"))},singleLine=true)}
  item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("all","saved","visited").forEach { f -> FilterChip(filter==f,{filter=f},label={Text(when(f){"saved"->t("Saved","Guardados");"visited"->t("Marked visited","Visita assinalada");else->t("All","Tudo")})})}}}
  item {
   Column(Modifier.clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) {Eyebrow(t("EDITORIAL BOARD","PAINEL EDITORIAL"),Modifier.weight(1f));Text("${(scale*100).roundToInt()}%",style=MaterialTheme.typography.labelSmall)}
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.background)
     .pointerInput(Unit) { detectTransformGestures { _,pan,zoom,_ -> scale=(scale*zoom).coerceIn(1f,2.5f);offset=androidx.compose.ui.geometry.Offset((offset.x+pan.x).coerceIn(-250f,250f),(offset.y+pan.y).coerceIn(-250f,250f)) } },contentAlignment=Alignment.Center) {
     Column(Modifier.graphicsLayer {scaleX=scale;scaleY=scale;translationX=offset.x;translationY=offset.y}.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      entries.chunked(2).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach { e ->
       OutlinedCard(onClick={open(e.id)},modifier=Modifier.weight(1f),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Icon(if(state.visited(e.id)) Icons.Outlined.CheckCircle else Icons.Outlined.Explore,null,tint=MaterialTheme.colorScheme.secondary);Text(e.title.inLanguage(state.prefs.language),style=MaterialTheme.typography.titleSmall) }
       }
      };if(row.size==1) Spacer(Modifier.weight(1f)) } }
     }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
     IconButton(onClick={scale=(scale-.25f).coerceAtLeast(1f)}) { Icon(Icons.Outlined.Remove,t("Zoom out","Reduzir zoom")) }
     IconButton(onClick={scale=(scale+.25f).coerceAtMost(2.5f)}) {Icon(Icons.Outlined.Add,t("Zoom in","Aumentar zoom"))}
     TextButton(onClick={scale=1f;offset=androidx.compose.ui.geometry.Offset.Zero}) {Text(t("Reset","Repor"))}
    }
    Text(t("Layout is an index, not geography. No distances, roads or location claims are implied. Pinch to zoom; open a card to save it or mark a manual visit.","A disposição é um índice, sem significado geográfico. Não indica distâncias, estradas ou posições. Usa os gestos de zoom; abre uma ficha para guardar ou assinalar uma visita manual."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  if(entries.isEmpty()) item {EmptyState(t("No regions match","Nenhuma região encontrada"),t("Clear your search or choose All.","Limpa a pesquisa ou escolhe Tudo."),Icons.Outlined.Explore)}
  items(entries,key={it.id}) {EntryCard(it,state){open(it.id)}}
 }
}
@Composable fun DetailScreen(entry: Entry,state: State,vm: AtlasViewModel,open: (String)->Unit) {
 var revealed by rememberSaveable(entry.id) {mutableStateOf(false)}
 var collectionDialog by remember {mutableStateOf(false)}
 val haptic=LocalHapticFeedback.current;val context=LocalContext.current;val hidden=entry.spoiler in state.hidden && !revealed
 val lang=state.prefs.language
 val invalidLink=t("Cannot open the official source on this device.","Não é possível abrir a fonte oficial neste dispositivo.")
 fun source() { try { require(Rules.safeSource(entry.source));context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(entry.source))) } catch(_: Exception){vm.notify("error")} }
 Page {
  item {PageTitle(category(entry.category),entry.title.inLanguage(lang))}
  item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   AssistChip(onClick={source()},label={Text(when(entry.evidence){"official"->t("Official source","Fonte oficial");"confirmed"->t("Confirmed","Confirmado");"editorial"->t("Editorial interpretation","Interpretação editorial");else->t("Unconfirmed","Não confirmado")})},leadingIcon={Icon(Icons.Outlined.Verified,null,Modifier.size(16.dp))})
   if(state.visited(entry.id)) AssistChip(onClick={vm.toggle("visited",entry.id)},label={Text(t("Visited manually","Visita manual"))})
  }}
  if(hidden) item {
   EmptyState(t("Spoiler Shield is on","Spoiler Shield ativo"),t("This dossier contains story details. Reveal it for this visit or change protection in Settings.","Esta ficha contém detalhes da história. Revela apenas nesta visita ou altera a proteção nas Definições."),Icons.Outlined.Shield)
   TextButton(onClick={revealed=true}) {Text(t("Reveal this dossier","Revelar esta ficha"))}
  } else {
   item { Text(entry.summary.inLanguage(lang),style=MaterialTheme.typography.bodyLarge) }
   item { Text(t("Verified on ","Verificado em ")+entry.verified+if(entry.published.isNotBlank()) "\n"+t("Published on ","Publicado em ")+entry.published else "",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall) }
   if(entry.related.isNotEmpty()) {
    item {Section(t("Connected dossiers","Fichas relacionadas"))}
    items(entry.related.mapNotNull { id -> state.pack!!.entries.find { it.id==id } },key={it.id}) {EntryCard(it,state){open(it.id)}}
   }
  }
  item {HorizontalDivider()}
  item {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Button(onClick={vm.toggle("favorite",entry.id);haptic.performHapticFeedback(HapticFeedbackType.LongPress)},modifier=Modifier.fillMaxWidth()) {Icon(if(state.saved(entry.id)) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,null);Spacer(Modifier.width(8.dp));Text(if(state.saved(entry.id)) t("Remove from saved","Remover dos guardados") else t("Save offline","Guardar offline"))}
   if(state.saved(entry.id)) OutlinedButton(onClick={collectionDialog=true},Modifier.fillMaxWidth()) {Text(t("Add to a collection","Adicionar a uma coleção"))}
   if(entry.category=="regions") OutlinedButton(onClick={vm.toggle("visited",entry.id)},Modifier.fillMaxWidth()) {Text(if(state.visited(entry.id)) t("Unmark visit","Remover visita") else t("Mark a manual visit","Assinalar visita manual"))}
   OutlinedButton(onClick={source()},Modifier.fillMaxWidth()) {Icon(Icons.Outlined.OpenInNew,null);Spacer(Modifier.width(8.dp));Text(t("Read the official source","Ler a fonte oficial"))}
   OutlinedButton(onClick={try {context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/plain";putExtra(Intent.EXTRA_TEXT,entry.title.inLanguage(lang)+"\n"+entry.source)},null))}catch(_: Exception){vm.notify("error")}},Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Share,null);Spacer(Modifier.width(8.dp));Text(t("Share source","Partilhar fonte"))}
   Text(entry.source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }}
 }
 if(collectionDialog) {
  var value by remember {mutableStateOf(state.local.firstOrNull {it.kind=="favorite" && it.target==entry.id}?.collection ?: "")}
  AlertDialog(onDismissRequest={collectionDialog=false},title={Text(t("Collection","Coleção"))},text={OutlinedTextField(value,{value=it.take(80)},label={Text(t("Collection name (empty to remove)","Nome da coleção (vazio para remover)"))})},confirmButton={TextButton(onClick={vm.moveFavorite(entry.id,value.trim());collectionDialog=false}){Text(t("Save","Guardar"))}},dismissButton={TextButton(onClick={collectionDialog=false}){Text(t("Cancel","Cancelar"))}})
 }
}

@Composable fun VaultScreen(state: State,vm: AtlasViewModel,open: (String)->Unit,export: ()->Unit,import: ()->Unit) {
 var tab by rememberSaveable {mutableStateOf("saved")};var editing by remember {mutableStateOf<PersonalItem?>(null)};var editor by rememberSaveable {mutableStateOf(false)}
 var collection by rememberSaveable {mutableStateOf("")}
 val tabs=listOf("saved","tasks","notes","collections","history","timeline","compare","calculator")
 val local=state.local
 Page {
  item {PageTitle(t("PERSONAL COMMAND CENTER","CENTRO DE COMANDO PESSOAL"),t("Make it yours.","O teu arquivo."),t("Local profile: ","Perfil local: ")+(state.profiles.find {it.id==state.prefs.profile}?.name ?: "Explorer"))}
  item {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {items(tabs){key->FilterChip(tab==key,{tab=key;collection=""},label={Text(vaultLabel(key))})}}}
  item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TextButton(onClick=export){Icon(Icons.Outlined.FileDownload,null);Spacer(Modifier.width(6.dp));Text(t("Export","Exportar"))};TextButton(onClick=import){Icon(Icons.Outlined.FileUpload,null);Spacer(Modifier.width(6.dp));Text(t("Import","Importar"))}}}
  when(tab) {
   "saved","history" -> {
    val kind=if(tab=="saved") "favorite" else "history"
    val collections=local.filter {it.kind==kind}.map {it.collection}.filter {it.isNotBlank()}.distinct()
    if(collections.isNotEmpty()) item {LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {item {FilterChip(collection.isEmpty(),{collection=""},label={Text(t("All","Tudo"))})};items(collections){c->FilterChip(collection==c,{collection=c},label={Text(c)})}}}
    val records=local.filter {it.kind==kind && (collection.isBlank() || it.collection==collection)}.sortedByDescending {it.updated}
    val entries=records.mapNotNull {i->state.pack!!.entries.find {it.id==i.target}}
    if(entries.isEmpty()) item {EmptyState(t("Your next discovery belongs here","A tua próxima descoberta fica aqui"),t("Open a dossier and save it. Available text is already stored on your device.","Abre uma ficha e guarda-a. O texto disponível já está no teu dispositivo."))}
    items(entries,key={it.id}){EntryCard(it,state){open(it.id)}}
    if(tab=="history" && records.isNotEmpty()) item {TextButton(onClick={records.forEach {vm.delete(it.id)}}){Text(t("Clear history for this profile","Limpar histórico deste perfil"))}}
   }
   "tasks","notes","collections" -> {
    val kind=when(tab){"tasks"->"task";"notes"->"note";else->"collection"}
    val records=local.filter {it.kind==kind}.sortedWith(compareBy<PersonalItem>{it.done}.thenBy{it.date.ifBlank {"9999"}}.thenByDescending{it.updated})
    item {Button(onClick={editing=null;editor=true},Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text(t("Create ","Criar ")+when(kind){"task"->t("objective","objetivo");"note"->t("note","nota");else->t("collection","coleção")})}}
    if(kind=="task" && records.isNotEmpty()) item {
     val done=records.count {it.done};Text(t("$done of ${records.size} complete","$done de ${records.size} concluídos"));Spacer(Modifier.height(10.dp));LinearProgressIndicator(progress={done.toFloat()/records.size},modifier=Modifier.fillMaxWidth())
    }
    if(records.isEmpty()) item {EmptyState(t("A clean slate","Tudo por começar"),t("Create your own plans. Progress is manual and stays on this device.","Cria os teus planos. O progresso é manual e fica neste dispositivo."),Icons.Outlined.EditNote)}
    items(records,key={it.id}) { item ->
     Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
      Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically) {
        if(kind=="task") Checkbox(item.done,{vm.complete(item)},modifier=Modifier.semantics {contentDescription=item.title})
        Text(item.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
        IconButton(onClick={editing=item;editor=true}){Icon(Icons.Outlined.Edit,t("Edit","Editar"))}
        IconButton(onClick={vm.delete(item.id)}){Icon(Icons.Outlined.DeleteOutline,t("Delete","Eliminar"))}
       }
       if(item.body.isNotBlank()) Text(item.body,color=MaterialTheme.colorScheme.onSurfaceVariant)
       if(item.collection.isNotBlank()) Eyebrow(item.collection)
       if(item.date.isNotBlank()) Text(item.date,style=MaterialTheme.typography.labelSmall,color=if(!item.done && LocalDate.parse(item.date).isBefore(LocalDate.now())) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
       if(kind=="collection") {
        val members=local.filter {it.collection==item.title && it.kind!="collection"}
        Text(t("${members.size} items","${members.size} itens"),style=MaterialTheme.typography.bodySmall)
        members.forEach {m-> val e=state.pack!!.entries.find {it.id==m.target};if(e!=null) TextButton(onClick={open(e.id)}){Text(e.title.inLanguage(state.prefs.language))} else Text(m.title) }
       }
      }
     }
    }
   }
   "timeline" -> {
    item {Text(t("Official announcements, ordered by date.","Anúncios oficiais, ordenados por data."),color=MaterialTheme.colorScheme.onSurfaceVariant)}
    items(state.pack!!.entries.filter {it.published.isNotBlank() && it.spoiler !in state.hidden}.sortedByDescending{it.published},key={it.id}){e->Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Eyebrow(e.published);EntryCard(e,state){open(e.id)}}}
   }
   "compare" -> item {ComparePanel(state,open)}
   "calculator" -> item {CalculatorPanel()}
  }
 }
 if(editor) EditPersonal(editing,when(tab){"tasks"->"task";"notes"->"note";else->"collection"},{editor=false}) {kind,title,body,group,date->vm.save(editing,kind,title,body,group,date);editor=false}
}
@Composable fun vaultLabel(key: String)=when(key){"saved"->t("Saved","Guardados");"tasks"->t("Objectives","Objetivos");"notes"->t("Notes","Notas");"collections"->t("Collections","Coleções");"history"->t("History","Histórico");"timeline"->t("Timeline","Cronologia");"compare"->t("Compare","Comparar");else->t("Calculator","Calculadora")}
@Composable fun EditPersonal(item: PersonalItem?,kind: String,dismiss: ()->Unit,save: (String,String,String,String,String)->Unit) {
 var title by rememberSaveable {mutableStateOf(item?.title ?: "")};var body by rememberSaveable {mutableStateOf(item?.body ?: "")}
 var group by rememberSaveable {mutableStateOf(item?.collection ?: "")};var date by rememberSaveable {mutableStateOf(item?.date ?: "")}
 val validDate=date.isBlank() || runCatching {LocalDate.parse(date)}.isSuccess
 AlertDialog(onDismissRequest=dismiss,title={Text(if(item==null)t("Create","Criar") else t("Edit","Editar"))},text={
  Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   OutlinedTextField(title,{title=it.take(200)},label={Text(t("Title","Título"))},singleLine=true)
   OutlinedTextField(body,{body=it.take(10000)},label={Text(t("Notes","Notas"))},minLines=2,maxLines=6)
   if(kind!="collection") OutlinedTextField(group,{group=it.take(80)},label={Text(t("Collection (optional)","Coleção (opcional)"))},singleLine=true)
   if(kind=="task") OutlinedTextField(date,{date=it.take(10)},label={Text(t("Date YYYY-MM-DD (optional)","Data AAAA-MM-DD (opcional)"))},isError=!validDate,singleLine=true)
  }
 },confirmButton={TextButton(onClick={save(kind,title,body,group,date)},enabled=title.isNotBlank() && validDate){Text(t("Save","Guardar"))}},dismissButton={TextButton(onClick=dismiss){Text(t("Cancel","Cancelar"))}})
}
@Composable fun ComparePanel(state: State,open: (String)->Unit) {
 var selected by rememberSaveable {mutableStateOf(listOf<String>())};var query by rememberSaveable {mutableStateOf("")}
 val entries=Discovery.search(state.pack!!.entries,query,hidden=state.hidden)
 Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text(t("Compare two verified dossiers","Compara duas fichas verificadas"),style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(query,{query=it.take(200)},label={Text(t("Find a dossier","Encontrar uma ficha"))},modifier=Modifier.fillMaxWidth())
  entries.take(12).forEach {e->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(e.id in selected,{checked->selected=if(checked && selected.size<2) selected+e.id else selected-e.id},enabled=e.id in selected || selected.size<2);Text(e.title.inLanguage(state.prefs.language))}}
  selected.mapNotNull {id->state.pack.entries.find{it.id==id}}.forEach {EntryCard(it,state){open(it.id)}}
  Text(t("Only sourced facts are compared; no unverified vehicle or weapon statistics are supplied.","A comparação usa apenas informação com fonte. Não inclui estatísticas de veículos ou armas por confirmar."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
@Composable fun CalculatorPanel() {
 var goal by rememberSaveable {mutableStateOf("")};var current by rememberSaveable {mutableStateOf("")};var per by rememberSaveable {mutableStateOf("")}
 fun number(s: String)=s.replace(',','.').toDoubleOrNull()
 val result=runCatching {Rules.sessions(number(goal) ?: error(""),number(current) ?: error(""),number(per) ?: error(""))}.getOrNull()
 Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text(t("Plan your own goal","Planeia o teu objetivo"),style=MaterialTheme.typography.titleLarge)
  Text(t("Enter your values. This calculator has no connection to game progression or economy.","Introduz os teus valores. Esta calculadora não tem ligação ao progresso ou à economia do jogo."),color=MaterialTheme.colorScheme.onSurfaceVariant)
  listOf(Triple(t("Goal","Objetivo"),goal,{s: String->goal=s}),Triple(t("Current progress","Progresso atual"),current,{s: String->current=s}),Triple(t("Progress per session","Progresso por sessão"),per,{s: String->per=s})).forEach {(label,value,change)->OutlinedTextField(value,{change(it.take(20))},label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal))}
  if(result!=null) {Text("$result",style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.primary);Text(t("sessions remaining","sessões em falta"))} else Text(t("Use non-negative numbers and progress per session greater than zero.","Usa números positivos ou zero e um progresso por sessão superior a zero."),style=MaterialTheme.typography.bodySmall)
 }
}
@Composable fun SettingsScreen(state: State,vm: AtlasViewModel,export: ()->Unit,import: ()->Unit) {
 var name by rememberSaveable {mutableStateOf("")};var endpoint by rememberSaveable {mutableStateOf(state.prefs.endpoint)};var deleteDialog by remember {mutableStateOf(false)}
 val valid=endpoint.isBlank() || Rules.safeEndpoint(endpoint)
 Page {
  item {PageTitle(t("YOUR PREFERENCES","À TUA MEDIDA"),t("Set your course.","O teu rumo."))}
  item {Section(t("Appearance","Aspeto"));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("system","dark","light").forEach{v->FilterChip(state.prefs.theme==v,{vm.preference("theme",v)},label={Text(when(v){"system"->t("System","Sistema");"dark"->t("Night","Noite");else->t("Day","Dia")})})}}}
  item {Section(t("Language","Idioma"));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(state.prefs.language=="pt",{vm.preference("language","pt")},label={Text("Português (PT)")});FilterChip(state.prefs.language=="en",{vm.preference("language","en")},label={Text("English")})}}
  item {Section("Spoiler Shield");SettingSwitch(t("Hide story details","Ocultar detalhes da história"),state.prefs.storyShield){vm.preference("storyShield",it)};SettingSwitch(t("Hide discoveries","Ocultar descobertas"),state.prefs.discoveryShield){vm.preference("discoveryShield",it)}}
  item {Section(t("Home shortcut","Atalho inicial"));LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(destinations.filter{it.route!="home"}){d->FilterChip(state.prefs.shortcut==d.route,{vm.preference("shortcut",d.route)},label={Text(t(d.en,d.pt))})}}}
  item {Section(t("Local profiles","Perfis locais"));Text(t("Each profile has separate favorites, history and plans.","Cada perfil tem favoritos, histórico e planos separados."),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  items(state.profiles,key={it.id}){p->OutlinedCard(onClick={vm.preference("profile",p.id)},Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(p.id==state.prefs.profile)Icons.Outlined.CheckCircle else Icons.Outlined.AccountCircle,null,tint=MaterialTheme.colorScheme.secondary);Spacer(Modifier.width(12.dp));Text(p.name)}}}
  item {OutlinedTextField(name,{name=it.take(80)},label={Text(t("New profile name","Nome do novo perfil"))},modifier=Modifier.fillMaxWidth(),singleLine=true);TextButton(onClick={vm.addProfile(name);name=""},enabled=name.isNotBlank() && state.profiles.size<20){Text(t("Create profile","Criar perfil"))};if(state.profiles.size>1)TextButton(onClick={deleteDialog=true}){Text(t("Delete current profile","Eliminar perfil atual"),color=MaterialTheme.colorScheme.error)}}
  item {HorizontalDivider();Section(t("Your data","Os teus dados"));Text(t("No accounts, advertising or analytics. Notes and plans stay in app-private storage. Exports are unencrypted: keep them somewhere you trust.","Sem contas, publicidade ou analítica. As notas e os planos ficam no armazenamento privado da app. As exportações não são cifradas: guarda-as num local de confiança."));Row{TextButton(onClick=export){Text(t("Export data","Exportar dados"))};TextButton(onClick=import){Text(t("Import backup","Importar cópia"))}}}
  item {Section(t("Content updates","Atualizações de conteúdo"));Text(t("This build includes a verified offline edition. Live publishing requires the publisher’s HTTPS feed and signing key. No Rockstar API is assumed.","Esta versão inclui uma edição offline verificada. A publicação remota requer um feed HTTPS e a chave de assinatura do editor. Não pressupõe uma API da Rockstar."),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item {OutlinedTextField(endpoint,{endpoint=it.take(2000)},label={Text(t("Publisher HTTPS feed (optional)","Feed HTTPS do editor (opcional)"))},modifier=Modifier.fillMaxWidth(),isError=!valid,singleLine=true);TextButton(onClick={vm.endpoint(endpoint.trim())},enabled=valid){Text(t("Save feed","Guardar feed"))};TextButton(onClick={vm.sync()}){Text(t("Check for updates","Procurar atualizações"))}}
  item {Section(t("About & privacy","Sobre e privacidade"));Text(t("Leonida Atlas 1.0.0. Independent unofficial companion. Not affiliated with, sponsored by or endorsed by Rockstar Games or Take-Two Interactive. GTA VI and related marks belong to their owners.","Leonida Atlas 1.0.0. Companion independente e não oficial. Sem afiliação, patrocínio ou aprovação da Rockstar Games ou Take-Two Interactive. GTA VI e as marcas relacionadas pertencem aos respetivos titulares."),style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(12.dp));Text(t("Only internet and network-state permissions. No location, contacts, storage or notification access. External links open in your browser; its privacy policy applies. App data can be erased in Android Settings or by deleting profiles. No personal data is sent to us. A configured content host sees ordinary request metadata such as IP address. Backups are chosen by you using Android’s file picker. Uninstalling removes local app data.","Apenas permissões de internet e estado da rede. Sem acesso a localização, contactos, armazenamento ou notificações. As fontes abrem no navegador e aplica-se a política desse serviço. Podes apagar os dados nas Definições do Android ou eliminar perfis. Não nos são enviados dados pessoais. Um servidor de conteúdo configurado recebe metadados normais do pedido, como o IP. Escolhes as cópias através do seletor de ficheiros do Android. A desinstalação remove os dados locais."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
 }
 if(deleteDialog) AlertDialog(onDismissRequest={deleteDialog=false},title={Text(t("Delete this profile?","Eliminar este perfil?"))},text={Text(t("Its favorites, notes and objectives will be permanently removed. Export a backup first if you want to keep them.","Os favoritos, as notas e os objetivos deste perfil serão eliminados. Exporta uma cópia antes se os quiseres guardar."))},confirmButton={TextButton(onClick={vm.eraseProfile();deleteDialog=false}){Text(t("Delete profile","Eliminar perfil"))}},dismissButton={TextButton(onClick={deleteDialog=false}){Text(t("Cancel","Cancelar"))}})
}
@Composable fun SettingSwitch(label: String,checked: Boolean,change: (Boolean)->Unit) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(checked,change,modifier=Modifier.semantics{contentDescription=label})}}

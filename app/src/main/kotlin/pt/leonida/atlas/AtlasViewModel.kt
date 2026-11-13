package pt.leonida.atlas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.leonida.atlas.core.*
import pt.leonida.atlas.data.*
import java.time.LocalDate
import java.util.UUID

data class State(
 val pack: ContentPack? = null, val items: List<PersonalItem> = emptyList(),
 val profiles: List<Profile> = emptyList(), val prefs: Preferences = Preferences(),
 val ready: Boolean = false, val error: Boolean = false
) {
 val local get()=items.filter { it.profile==prefs.profile }
 val hidden get()=buildSet { if(prefs.storyShield) add("story"); if(prefs.discoveryShield) add("discovery") }
 fun saved(id: String)=local.any { it.kind=="favorite" && it.target==id }
 fun visited(id: String)=local.any { it.kind=="visited" && it.target==id }
}
class AtlasViewModel(app: Application): AndroidViewModel(app) {
 private val repo=(app as AtlasApp).repository
 private val ready=MutableStateFlow(false)
 private val failed=MutableStateFlow(false)
 private val _message=MutableSharedFlow<String>(extraBufferCapacity=4)
 val messages=_message.asSharedFlow()
 val state=combine(repo.pack,repo.items,repo.profiles,repo.prefs,ready) { p,i,profiles,prefs,r -> State(p,i,profiles,prefs,r) }
  .combine(failed) { s,e -> s.copy(error=e) }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),State())
 var syncing=MutableStateFlow(false); private set
 init { retry() }
 private fun run(block: suspend ()->Unit) = viewModelScope.launch {
  try { block() } catch(e: CancellationException) { throw e } catch(_: Exception) { _message.emit("error") }
 }
 fun retry()=run { failed.value=false; try { repo.initialize(); ready.value=true } catch(e: Exception) { failed.value=true; throw e } }
 fun preference(key: String,value: String)=run { repo.preference(key,value) }
 fun preference(key: String,value: Boolean)=run { repo.preference(key,value) }
 fun toggle(kind: String,id: String) = run {
  val s=state.value; val key="${s.prefs.profile}:$kind:$id"
  if(s.local.any { it.id==key }) repo.delete(key) else repo.put(PersonalItem(key,s.prefs.profile,kind,target=id,updated=System.currentTimeMillis()))
 }
 fun visit(id: String)=run {
  val s=state.value; repo.put(PersonalItem("${s.prefs.profile}:history:$id",s.prefs.profile,"history",target=id,updated=System.currentTimeMillis()))
 }
 fun delete(id: String)=run { repo.delete(id) }
 fun complete(item: PersonalItem)=run { repo.put(item.copy(done=!item.done,updated=System.currentTimeMillis())) }
 fun save(item: PersonalItem?,kind: String,title: String,body: String,collection: String,date: String)=run {
  require(title.trim().isNotBlank() && title.length<=200 && body.length<=10000 && collection.length<=80)
  if(date.isNotBlank()) LocalDate.parse(date)
  repo.put(PersonalItem(item?.id ?: UUID.randomUUID().toString(),state.value.prefs.profile,kind,
   target=item?.target ?: "",title=title.trim(),body=body,collection=collection.trim(),done=item?.done ?: false,date=date,updated=System.currentTimeMillis()))
 }
 fun moveFavorite(id: String,collection: String)=run {
  val item=state.value.local.first { it.kind=="favorite" && it.target==id }
  repo.put(item.copy(collection=collection,updated=System.currentTimeMillis()))
 }
 fun addProfile(name: String)=run {
  val id=UUID.randomUUID().toString(); repo.profile(Profile(id,name.trim())); repo.preference("profile",id)
 }
 fun eraseProfile()=run { repo.eraseProfile(state.value.prefs.profile) }
 fun sync()=run {
  syncing.value=true
  try { repo.synchronize(); _message.emit("synced") }
  catch(e: CancellationException) { throw e }
  catch(e: Exception) { _message.emit(if(e.message=="NO_ENDPOINT" || e.message=="NO_KEY") "no-sync" else "sync-error") }
  finally { syncing.value=false }
 }
 fun endpoint(url: String)=run {
  require(url.isBlank() || Rules.safeEndpoint(url)); repo.preference("endpoint",url)
  SyncWorker.configure(getApplication(),url.isNotBlank())
 }
 suspend fun export()=withContext(Dispatchers.IO) { repo.export() }
 suspend fun importBackup(text: String)=withContext(Dispatchers.IO) { repo.importBackup(text) }
 fun notify(message: String) { _message.tryEmit(message) }
}

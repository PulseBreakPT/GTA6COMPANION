package pt.leonida.atlas.data

import android.content.Context
import androidx.room.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import pt.leonida.atlas.core.*
import java.util.concurrent.TimeUnit

@Entity(tableName="items", indices=[Index("profile"), Index("kind"), Index("target")])
data class ItemRow(@PrimaryKey val id: String, val profile: String, val kind: String, val target: String, val payload: String)
@Entity(tableName="profiles") data class ProfileRow(@PrimaryKey val id: String, val name: String)
@Entity(tableName="content") data class PackRow(@PrimaryKey val id: Int = 1, val payload: String)
@Dao interface AtlasDao {
 @Query("SELECT * FROM items ORDER BY id") fun observeItems(): Flow<List<ItemRow>>
 @Query("SELECT * FROM profiles ORDER BY name") fun observeProfiles(): Flow<List<ProfileRow>>
 @Query("SELECT * FROM content WHERE id=1") fun observePack(): Flow<PackRow?>
 @Query("SELECT * FROM items ORDER BY id") suspend fun items(): List<ItemRow>
 @Query("SELECT * FROM profiles ORDER BY id") suspend fun profiles(): List<ProfileRow>
 @Query("SELECT * FROM content WHERE id=1") suspend fun pack(): PackRow?
 @Upsert suspend fun putItem(item: ItemRow)
 @Upsert suspend fun putProfile(profile: ProfileRow)
 @Upsert suspend fun putPack(pack: PackRow)
 @Query("DELETE FROM items WHERE id=:id") suspend fun deleteItem(id: String)
 @Query("DELETE FROM items WHERE profile=:profile") suspend fun clearProfileItems(profile: String)
 @Query("DELETE FROM profiles WHERE id=:id") suspend fun deleteProfile(id: String)
}
@Database(entities=[ItemRow::class, ProfileRow::class, PackRow::class], version=1, exportSchema=true)
abstract class AtlasDatabase: RoomDatabase() { abstract fun dao(): AtlasDao }
val Context.preferences by preferencesDataStore("atlas_preferences")
data class Preferences(
 val language: String = "pt", val theme: String = "system", val onboarding: Boolean = false,
 val profile: String = "default", val storyShield: Boolean = true, val discoveryShield: Boolean = false,
 val shortcut: String = "map", val endpoint: String = ""
)
class AtlasRepository(private val context: Context, val db: AtlasDatabase) {
 private val dao=db.dao()
 private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
  .connectTimeout(12,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(35,TimeUnit.SECONDS).build()
 val items=dao.observeItems().map { rows -> rows.map { AtlasJson.decodeFromString<PersonalItem>(it.payload) } }
 val profiles=dao.observeProfiles().map { rows -> rows.map { Profile(it.id,it.name) } }
 val pack=dao.observePack().map { it?.let { AtlasJson.decodeFromString<ContentPack>(it.payload) } }
 val prefs=context.preferences.data.catch { e -> if(e is java.io.IOException) emit(emptyPreferences()) else throw e }.map {
  Preferences(it[stringPreferencesKey("language")] ?: if(java.util.Locale.getDefault().language=="pt") "pt" else "en",
   it[stringPreferencesKey("theme")] ?: "system", it[booleanPreferencesKey("onboarding")] ?: false,
   it[stringPreferencesKey("profile")] ?: "default", it[booleanPreferencesKey("storyShield")] ?: true,
   it[booleanPreferencesKey("discoveryShield")] ?: false, it[stringPreferencesKey("shortcut")] ?: "map",
   it[stringPreferencesKey("endpoint")] ?: "")
 }
 suspend fun initialize() = withContext(Dispatchers.IO) {
  db.withTransaction {
   if(dao.profiles().isEmpty()) dao.putProfile(ProfileRow("default", "Explorer"))
   val seed=Rules.validate(AtlasJson.decodeFromString<ContentPack>(context.assets.open("content.json").bufferedReader().use { it.readText() }))
   val current=dao.pack()?.let { AtlasJson.decodeFromString<ContentPack>(it.payload) }
   if(current==null || current.version<seed.version) dao.putPack(PackRow(payload=AtlasJson.encodeToString(seed)))
  }
 }
 suspend fun preference(key: String, value: String) {
  require(key in setOf("language","theme","profile","shortcut","endpoint"))
  context.preferences.edit { it[stringPreferencesKey(key)]=value }
 }
 suspend fun preference(key: String, value: Boolean) {
  require(key in setOf("onboarding","storyShield","discoveryShield"))
  context.preferences.edit { it[booleanPreferencesKey(key)]=value }
 }
 suspend fun put(item: PersonalItem) = db.withTransaction {
  val all=dao.items().map { AtlasJson.decodeFromString<PersonalItem>(it.payload) }
  if(item.kind=="collection") {
   require(all.none { it.profile==item.profile && it.kind=="collection" && it.title==item.title && it.id!=item.id })
   val old=all.find { it.id==item.id }
   if(old!=null && old.title!=item.title) all.filter { it.profile==item.profile && it.collection==old.title }.forEach {
    dao.putItem(it.copy(collection=item.title,updated=item.updated).row())
   }
  } else if(item.collection.isNotBlank() && all.none { it.profile==item.profile && it.kind=="collection" && it.title==item.collection }) {
   val suffix=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(item.collection.toByteArray())
   dao.putItem(PersonalItem("${item.profile}:collection:$suffix",item.profile,"collection",title=item.collection,updated=item.updated).row())
  }
  dao.putItem(item.row())
 }
 suspend fun delete(id: String) = db.withTransaction {
  val all=dao.items().map { AtlasJson.decodeFromString<PersonalItem>(it.payload) }
  val old=all.find { it.id==id }
  if(old?.kind=="collection") all.filter { it.profile==old.profile && it.collection==old.title }.forEach {
   dao.putItem(it.copy(collection="",updated=System.currentTimeMillis()).row())
  }
  dao.deleteItem(id)
 }
 suspend fun profile(profile: Profile) {
  require(profile.name.isNotBlank() && profile.name.length<=80)
  require(dao.profiles().size<20); dao.putProfile(ProfileRow(profile.id,profile.name))
 }
 suspend fun eraseProfile(id: String) {
  db.withTransaction {
   require(dao.profiles().size>1)
   dao.clearProfileItems(id); dao.deleteProfile(id)
   preference("profile",dao.profiles().first().id)
  }
 }
 suspend fun export(): String = db.withTransaction {
  AtlasJson.encodeToString(Backup(profiles=dao.profiles().map { Profile(it.id,it.name) }, items=dao.items().map { AtlasJson.decodeFromString<PersonalItem>(it.payload) }))
 }
 suspend fun importBackup(text: String) {
  require(text.toByteArray().size<=2_000_000)
  val b=Rules.validate(AtlasJson.decodeFromString<Backup>(text))
  db.withTransaction {
   require((dao.profiles().map { it.id } + b.profiles.map { it.id }).distinct().size<=20)
   val existing=dao.items().associateBy { it.id }
   require((existing.keys+b.items.map { it.id }).size<=10000)
   b.profiles.forEach { dao.putProfile(ProfileRow(it.id,it.name)) }
   b.items.forEach {
    val old=existing[it.id]?.let { row -> AtlasJson.decodeFromString<PersonalItem>(row.payload) }
    if(old==null || it.updated>old.updated) dao.putItem(it.row())
   }
  }
 }
 suspend fun synchronize(): Int = withContext(Dispatchers.IO) {
  val endpoint=prefs.first().endpoint
  check(endpoint.isNotBlank()) { "NO_ENDPOINT" }
  require(Rules.safeEndpoint(endpoint)) { "Invalid HTTPS endpoint" }
  val key=context.assets.open("editorial_public_key.txt").bufferedReader().use { it.readText().trim() }
  check(key.isNotBlank()) { "NO_KEY" }
  client.newCall(Request.Builder().url(endpoint).header("Accept","application/json").build()).execute().use { response ->
   check(response.isSuccessful) { "HTTP ${response.code}" }
   val body=checkNotNull(response.body)
   require(body.contentLength()<=2_000_000) { "Content too large" }
   val bytes=body.byteStream().use { input ->
    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var count=input.read(buffer)
    while(count!=-1) { require(output.size()+count<=2_000_000);output.write(buffer,0,count);count=input.read(buffer) }
    output.toByteArray()
   }
   require(bytes.size<=2_000_000)
   db.withTransaction {
    val previous=dao.pack()?.let { AtlasJson.decodeFromString<ContentPack>(it.payload).version } ?: 0
    val pack=Rules.decodeSigned(bytes.decodeToString(),key,previous)
    dao.putPack(PackRow(payload=AtlasJson.encodeToString(pack))); pack.version
   }
  }
 }
 private fun PersonalItem.row()=ItemRow(id,profile,kind,target,AtlasJson.encodeToString(this))
}
class SyncWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
 override suspend fun doWork(): Result {
  val db=Room.databaseBuilder(applicationContext,AtlasDatabase::class.java,"atlas.db").build()
  return try { AtlasRepository(applicationContext,db).synchronize(); Result.success() }
  catch (e: java.io.IOException) { if(runAttemptCount<3) Result.retry() else Result.failure() }
  catch (_: Exception) { Result.failure() }
  finally { db.close() }
 }
 companion object {
  fun configure(context: Context, enabled: Boolean) {
   val work=WorkManager.getInstance(context)
   if(!enabled) { work.cancelUniqueWork("editorial-sync"); return }
   val request=PeriodicWorkRequestBuilder<SyncWorker>(12,TimeUnit.HOURS)
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).build()
   work.enqueueUniquePeriodicWork("editorial-sync",ExistingPeriodicWorkPolicy.UPDATE,request)
  }
 }
}

fun createAtlasRepository(context: Context): AtlasRepository = AtlasRepository(context,Room.databaseBuilder(context,AtlasDatabase::class.java,"atlas.db").build())

package pt.leonida.atlas.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.math.ceil

val AtlasJson = Json { ignoreUnknownKeys = false; encodeDefaults = true }
@Serializable data class LocalText(val en: String, val pt: String) {
 fun inLanguage(lang: String) = if (lang == "pt") pt else en
}
@Serializable data class Entry(
 val id: String, val category: String, val title: LocalText, val summary: LocalText,
 val source: String, val verified: String, val published: String = "", val evidence: String = "official",
 val spoiler: String = "none", val related: List<String> = emptyList(), val tags: List<String> = emptyList()
)
@Serializable data class ContentPack(
 val schema: Int = 1, val version: Int, val verified: String, val launchDate: String,
 val launchSource: String, val entries: List<Entry>
)
@Serializable data class SignedPack(val payload: String, val signature: String)
@Serializable data class Profile(val id: String, val name: String)
@Serializable data class PersonalItem(
 val id: String, val profile: String, val kind: String, val target: String = "",
 val title: String = "", val body: String = "", val collection: String = "",
 val done: Boolean = false, val date: String = "", val updated: Long = 0
)
@Serializable data class Backup(val schema: Int = 1, val profiles: List<Profile>, val items: List<PersonalItem>)

object Rules {
 val categories = setOf("people", "regions", "news", "trailers", "vehicles", "weapons", "business", "wildlife", "culture", "radio", "mechanics", "story")
 private val officialHosts = setOf("www.rockstargames.com", "rockstargames.com", "support.rockstargames.com", "www.take2games.com", "take2games.com")
 fun safeSource(url: String): Boolean = runCatching {
  val u = URI(url)
  u.scheme == "https" && u.host in officialHosts && u.userInfo == null && (u.port == -1 || u.port == 443)
 }.getOrDefault(false)
 fun safeEndpoint(url: String): Boolean = runCatching {
  val u = URI(url); val h=u.host.orEmpty().lowercase()
  u.scheme == "https" && h.contains('.') && h.any { it.isLetter() } &&
   !h.endsWith(".local") && !h.endsWith(".localhost") && h != "localhost" &&
   u.userInfo == null && u.fragment == null && (u.port == -1 || u.port == 443) && url.length <= 2000
 }.getOrDefault(false)
 fun validate(pack: ContentPack, previous: Int = 0): ContentPack {
  require(pack.schema == 1 && pack.version >= previous && pack.version > 0) { "Unsupported or older content version" }
  LocalDate.parse(pack.verified); LocalDate.parse(pack.launchDate)
  require(safeSource(pack.launchSource)) { "Invalid launch source" }
  require(pack.entries.size in 1..10000)
  require(pack.entries.map { it.id }.distinct().size == pack.entries.size)
  val ids=pack.entries.map { it.id }.toSet()
  pack.entries.forEach {
   require(it.id.matches(Regex("[a-z0-9-]{1,80}")))
   require(it.category in categories && safeSource(it.source))
   require(it.evidence in setOf("official", "confirmed", "editorial", "unconfirmed"))
   require(it.spoiler in setOf("none", "story", "discovery"))
   require(it.title.en.length in 1..200 && it.title.pt.length in 1..200)
   require(it.summary.en.length in 1..12000 && it.summary.pt.length in 1..12000)
   require(it.related.all { ref -> ref in ids }); LocalDate.parse(it.verified)
   if (it.published.isNotBlank()) LocalDate.parse(it.published)
  }
  return pack
 }
 fun validate(backup: Backup): Backup {
  require(backup.schema == 1 && backup.profiles.size in 1..20 && backup.items.size <= 10000)
  require(backup.profiles.map { it.id }.distinct().size == backup.profiles.size)
  require(backup.items.map { it.id }.distinct().size == backup.items.size)
  val profiles=backup.profiles.map { it.id }.toSet()
  backup.profiles.forEach { require(it.id.matches(Regex("[a-zA-Z0-9-]{1,80}")) && it.name.length in 1..80) }
  backup.items.forEach {
   require(it.profile in profiles && it.id.length in 1..200 && it.target.length <= 80)
   require(it.kind in setOf("favorite", "history", "visited", "task", "note", "collection"))
   require(it.title.length <= 200 && it.body.length <= 10000 && it.collection.length <= 80)
   require(it.updated >= 0)
   if (it.date.isNotBlank()) LocalDate.parse(it.date)
  }
  return backup
 }
 fun daysUntil(date: String, today: LocalDate) = ChronoUnit.DAYS.between(today, LocalDate.parse(date)).coerceAtLeast(0)
 fun sessions(goal: Double, current: Double, perSession: Double): Long {
  require(goal.isFinite() && current.isFinite() && perSession.isFinite() && goal >= 0 && current >= 0 && perSession > 0)
  val count=ceil((goal-current).coerceAtLeast(0.0)/perSession)
  require(count < Long.MAX_VALUE.toDouble()); return count.toLong()
 }
 fun decodeSigned(envelope: String, publicKey: String, previous: Int): ContentPack {
  require(envelope.toByteArray().size <= 2_000_000)
  val signed=AtlasJson.decodeFromString<SignedPack>(envelope)
  val bytes=Base64.getDecoder().decode(signed.payload)
  val key=KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
  val verifier=Signature.getInstance("SHA256withRSA")
  verifier.initVerify(key); verifier.update(bytes)
  require(verifier.verify(Base64.getDecoder().decode(signed.signature))) { "Invalid content signature" }
  return validate(AtlasJson.decodeFromString<ContentPack>(bytes.decodeToString()), previous)
 }
}
private val accentMarks = Regex("\\p{M}+")
private val wordBreaks = Regex("\\s+")
fun String.folded() = Normalizer.normalize(lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD).replace(accentMarks, "")

/** Local immutable index; normalized documents are built once per content edition. */
private class LocalSearchIndex(entries: List<Entry>) {
 private data class Document(val entry: Entry, val text: String, val englishTitle: String, val portugueseTitle: String)
 private val documents=entries.map { entry -> Document(entry,
  (entry.title.en+" "+entry.title.pt+" "+entry.summary.en+" "+entry.summary.pt+" "+entry.tags.joinToString(" ")).folded(),
  entry.title.en.folded(),entry.title.pt.folded()) }
 fun search(query: String,category: String,hidden: Set<String>): List<Entry> {
  val terms=query.folded().trim().split(wordBreaks).filter { it.isNotBlank() }.take(12)
  return documents.asSequence().filter { d ->
   (category=="all" || d.entry.category==category) && d.entry.spoiler !in hidden && terms.all { d.text.contains(it) }
  }.sortedByDescending { d -> terms.count { d.englishTitle.startsWith(it) || d.portugueseTitle.startsWith(it) } }.map { it.entry }.toList()
 }
}
object Discovery {
 private var source: List<Entry>?=null
 private var index: LocalSearchIndex?=null
 @Synchronized private fun indexFor(entries: List<Entry>): LocalSearchIndex {
  if(source !== entries) { index=LocalSearchIndex(entries);source=entries }
  return checkNotNull(index)
 }
 fun search(entries: List<Entry>,query: String,category: String="all",hidden: Set<String> = emptySet()): List<Entry> =
  indexFor(entries).search(query,category,hidden)
 fun recommended(entries: List<Entry>, items: List<PersonalItem>, hidden: Set<String>): List<Entry> {
  val known=items.filter { it.kind in setOf("favorite", "history") }.map { it.target }.toSet()
  val weights=entries.filter { it.id in known }.groupingBy { it.category }.eachCount()
  return entries.filter { it.spoiler !in hidden && it.id !in known }
   .sortedWith(compareByDescending<Entry> { weights[it.category] ?: 0 }.thenBy { it.id }).take(6)
 }
}

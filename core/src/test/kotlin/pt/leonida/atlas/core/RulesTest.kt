package pt.leonida.atlas.core
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlinx.serialization.encodeToString

class RulesTest {
 private val source="https://www.rockstargames.com/VI"
 private val e=Entry("vice-city","regions",LocalText("Vice City","Vice City"),LocalText("Known region","Região conhecida"),source,"2026-11-13")
 private fun pack()=ContentPack(version=2,verified="2026-11-13",launchDate="2026-11-19",launchSource=source,entries=listOf(e))
 private fun rejects(block: ()->Unit) { try {block();fail("Expected rejection")}catch(_: RuntimeException){} }
 @Test fun bundledEditorialPackIsValid() {
  val text=checkNotNull(javaClass.getResourceAsStream("/content.json")).bufferedReader().use { it.readText() }
  assertEquals(18,Rules.validate(AtlasJson.decodeFromString<ContentPack>(text)).entries.size)
 }
 @Test fun validatesSeedStructure() {assertEquals(2,Rules.validate(pack()).version)}
 @Test fun rejectsDuplicateIds() {rejects {Rules.validate(pack().copy(entries=listOf(e,e)))}}
 @Test fun rejectsRollback() {rejects {Rules.validate(pack(),3)}}
 @Test fun rejectsUnknownSchema() {rejects {Rules.validate(pack().copy(schema=3))}}
 @Test fun rejectsDanglingRelations() {rejects {Rules.validate(pack().copy(entries=listOf(e.copy(related=listOf("missing")))))}}
 @Test fun rejectsUntrustedSources() {rejects {Rules.validate(pack().copy(entries=listOf(e.copy(source="https://rockstargames.com.evil.test/VI"))))}}
 @Test fun rejectsJavascriptAndCredentials() {assertFalse(Rules.safeSource("javascript:alert(1)"));assertFalse(Rules.safeSource("https://user@www.rockstargames.com/VI"));assertFalse(Rules.safeSource("http://www.rockstargames.com/VI"))}
 @Test fun rejectsLocalEndpoint() {assertFalse(Rules.safeEndpoint("http://localhost"));assertFalse(Rules.safeEndpoint("https://127.0.0.1"));assertFalse(Rules.safeEndpoint("https://home.local/feed"));assertTrue(Rules.safeEndpoint("https://example.org/content.json"))}
 @Test fun rejectsUnknownCategory() {rejects {Rules.validate(pack().copy(entries=listOf(e.copy(category="cheats"))))}}
 @Test fun rejectsInvalidDates() {rejects {Rules.validate(pack().copy(launchDate="wrong"))}}
 @Test fun searchHandlesPortugueseAccents() {assertEquals(listOf(e),Discovery.search(listOf(e),"regiao"))}
 @Test fun searchMatchesAllTerms() {assertTrue(Discovery.search(listOf(e),"vice moon").isEmpty());assertEquals(1,Discovery.search(listOf(e),"vice city").size)}
 @Test fun shieldRemovesProtectedSearchResults() {assertTrue(Discovery.search(listOf(e.copy(spoiler="story")),"",hidden=setOf("story")).isEmpty())}
 @Test fun recommendationsRespectPrivacyAndShield() {
  val saved=PersonalItem("x","default","favorite",target=e.id)
  val other=e.copy(id="keys",spoiler="story")
  assertTrue(Discovery.recommended(listOf(e,other),listOf(saved),setOf("story")).isEmpty())
 }
 @Test fun countdownHandlesLaunchAndPast() {assertEquals(6,Rules.daysUntil("2026-11-19",LocalDate.of(2026,11,13)));assertEquals(0,Rules.daysUntil("2026-11-19",LocalDate.of(2026,11,20)))}
 @Test fun calculatorRoundsUpAndHandlesAchievedGoal() {assertEquals(4,Rules.sessions(100.0,5.0,25.0));assertEquals(0,Rules.sessions(100.0,150.0,10.0))}
 @Test fun calculatorRejectsZeroNegativeInfinityAndNan() {rejects {Rules.sessions(100.0,0.0,0.0)};rejects {Rules.sessions(-1.0,0.0,1.0)};rejects {Rules.sessions(Double.NaN,0.0,1.0)};rejects {Rules.sessions(Double.POSITIVE_INFINITY,0.0,1.0)}}
 @Test fun backupRoundTripPreservesProfilesAndUnicode() {
  val b=Backup(profiles=listOf(Profile("default","Tiago")),items=listOf(PersonalItem("one","default","note",title="Férias",body="Olá Leonida",collection="Planos",updated=9)))
  assertEquals(b,Rules.validate(AtlasJson.decodeFromString<Backup>(AtlasJson.encodeToString(b))))
 }
 @Test fun rejectsBackupOrphanAndDuplicates() {
  val b=Backup(profiles=listOf(Profile("default","Tiago")),items=listOf(PersonalItem("one","missing","note")))
  rejects {Rules.validate(b)};rejects {Rules.validate(b.copy(profiles=listOf(Profile("default","Tiago"),Profile("default","Other"))))}
 }
 @Test fun signedContentAcceptsValidSignatureAndRejectsTampering() {
  val pair=KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()
  val bytes=AtlasJson.encodeToString(pack()).toByteArray()
  val sig=Signature.getInstance("SHA256withRSA").apply {initSign(pair.private);update(bytes)}.sign()
  val envelope=SignedPack(Base64.getEncoder().encodeToString(bytes),Base64.getEncoder().encodeToString(sig))
  val key=Base64.getEncoder().encodeToString(pair.public.encoded)
  assertEquals(pack(),Rules.decodeSigned(AtlasJson.encodeToString(envelope),key,1))
  val bad=envelope.copy(payload=Base64.getEncoder().encodeToString(AtlasJson.encodeToString(pack().copy(version=99)).toByteArray()))
  rejects {Rules.decodeSigned(AtlasJson.encodeToString(bad),key,1)}
 }
}

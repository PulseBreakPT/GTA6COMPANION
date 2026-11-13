package pt.leonida.atlas.data
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import pt.leonida.atlas.core.*

@RunWith(AndroidJUnit4::class) class RepositoryTest {
 private lateinit var db: AtlasDatabase
 private lateinit var repo: AtlasRepository
 @Before fun setup() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  db=Room.inMemoryDatabaseBuilder(context,AtlasDatabase::class.java).build();repo=AtlasRepository(context,db)
 }
 @After fun close() {db.close()}
 @Test fun renamingAndDeletingCollectionPreservesItsMembers()=runBlocking {
  repo.profile(Profile("default","Tiago"))
  repo.put(PersonalItem("note","default","note",title="Route",collection="Trip",updated=1))
  val group=AtlasJson.decodeFromString<Backup>(repo.export()).items.first { it.kind=="collection" }
  repo.put(group.copy(title="Weekend",updated=2))
  assertEquals("Weekend",AtlasJson.decodeFromString<Backup>(repo.export()).items.first { it.id=="note" }.collection)
  repo.delete(group.id)
  assertEquals("",AtlasJson.decodeFromString<Backup>(repo.export()).items.first { it.id=="note" }.collection)
 }
 @Test fun roomBackupImportIsAtomicAndProfileScoped()=runBlocking {
  val backup=Backup(profiles=listOf(Profile("default","Tiago"),Profile("other","Guest")),items=listOf(
   PersonalItem("default:favorite:vice-city","default","favorite",target="vice-city",updated=1),
   PersonalItem("note-1","other","note",title="Private",body="Local",updated=2)))
  repo.importBackup(AtlasJson.encodeToString(Backup.serializer(),backup))
  assertEquals(2,db.dao().items().size)
  val before=repo.export()
  try {repo.importBackup("{bad}");fail("Expected invalid input")} catch(_: IllegalArgumentException){}
  assertEquals(before,repo.export())
  repo.importBackup(AtlasJson.encodeToString(Backup.serializer(),backup.copy(items=listOf(backup.items[1].copy(body="New",updated=3)))))
  val merged=AtlasJson.decodeFromString<Backup>(repo.export())
  assertEquals("New",merged.items.first{it.id=="note-1"}.body)
  repo.eraseProfile("other")
  assertEquals(1,db.dao().items().size);assertEquals("default",db.dao().items().first().profile)
 }
}
